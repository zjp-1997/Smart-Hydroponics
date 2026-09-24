package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.ConsultAttachmentResponse;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.ConsultMessageMapper;
import com.smart_plant.smart_plant.mapper.FarmChatMessageMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 将聊天图片和常见文档保存到独立目录，并校验发送者只能引用本人上传的文件。 */
@Service
@RequiredArgsConstructor
public class ConsultAttachmentService {
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
    private static final Set<String> FILE_EXTENSIONS = Set.of(
            "pdf", "txt", "csv", "md", "json", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip");
    private static final String FILE_NAME_PATTERN =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.[a-z0-9]{2,5}";

    private final DataPermissionService dataPermissionService;
    private final ConsultMessageMapper consultMessageMapper;
    private final FarmChatMessageMapper farmChatMessageMapper;

    public ConsultAttachmentResponse upload(MultipartFile file, String kind) {
        return upload(file, kind, null);
    }

    /** displayName 保留 App 沙箱复制前的文件名，实际落盘始终使用 UUID。 */
    public ConsultAttachmentResponse upload(MultipartFile file, String kind, String displayName) {
        boolean image = "image".equals(kind);
        if (!image && !"file".equals(kind)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "附件类型不支持");
        }
        if (file == null || file.isEmpty() || file.getSize() > (image ? MAX_IMAGE_BYTES : MAX_FILE_BYTES)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, image ? "图片不能为空且不能超过10MB" : "文件不能为空且不能超过20MB");
        }
        // 浏览器有时会提交带路径的文件名，仅保留末段供气泡显示。
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename()
                .replace('\\', '/').replaceAll("^.*/", "");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (image) {
            String expected = IMAGE_TYPES.get(contentType);
            if (expected == null || !(extension.isEmpty() || expected.equals(extension)
                    || "jpg".equals(expected) && "jpeg".equals(extension)) || !hasImageSignature(file, expected)) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "仅支持 JPG、PNG、WEBP 图片");
            }
            extension = expected;
        } else if (!FILE_EXTENSIONS.contains(extension)) {
            // 禁止 HTML、脚本和可执行文件进入静态上传目录。
            throw new BusinessException(ResponseCode.PARAM_ERROR, "仅支持常见文档、文本和 ZIP 文件");
        }

        Long userId = dataPermissionService.currentUser().getId();
        Path directory = Paths.get(System.getProperty("user.dir"), "uploads", "consult-chat", userId.toString(), kind)
                .toAbsolutePath().normalize();
        String fileName = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(directory);
            file.transferTo(directory.resolve(fileName));
        } catch (IOException exception) {
            throw new BusinessException(ResponseCode.FAIL, "聊天附件上传失败");
        }
        String shownName = displayName == null || displayName.isBlank() ? original : displayName;
        shownName = shownName.replace('\\', '/').replaceAll("^.*/", "").replaceAll("[\\p{Cntrl}]", "");
        String safeName = shownName.length() > 180 ? shownName.substring(shownName.length() - 180) : shownName;
        return new ConsultAttachmentResponse("/uploads/consult-chat/" + userId + "/" + kind + "/" + fileName,
                safeName, file.getSize());
    }

    /** 同时核对文件头，避免将伪装成图片的 HTML 等内容公开为聊天图片。 */
    private boolean hasImageSignature(MultipartFile file, String extension) {
        try (InputStream input = file.getInputStream()) {
            byte[] head = input.readNBytes(12);
            if ("jpg".equals(extension)) {
                return head.length >= 3 && (head[0] & 0xff) == 0xff
                        && (head[1] & 0xff) == 0xd8 && (head[2] & 0xff) == 0xff;
            }
            if ("png".equals(extension)) {
                return head.length >= 8 && (head[0] & 0xff) == 0x89
                        && head[1] == 'P' && head[2] == 'N' && head[3] == 'G'
                        && head[4] == 13 && head[5] == 10 && head[6] == 26 && head[7] == 10;
            }
            return head.length >= 12 && head[0] == 'R' && head[1] == 'I'
                    && head[2] == 'F' && head[3] == 'F' && head[8] == 'W'
                    && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
        } catch (IOException exception) {
            return false;
        }
    }

    /** 消息发送时再检查路径、所属上传用户和真实文件，不能直接提交外部 URL。 */
    public void requireOwned(String mediaUrl, int messageType) {
        resolveOwnedPath(mediaUrl, messageType);
    }

    /** AI 咨询需读取本人已上传附件的真实内容，复用聊天附件的归属与路径校验。 */
    public Path resolveOwnedPath(String mediaUrl, int messageType) {
        String kind = messageType == 2 ? "image" : messageType == 4 ? "file" : "";
        Long userId = dataPermissionService.currentUser().getId();
        String prefix = "/uploads/consult-chat/" + userId + "/" + kind + "/";
        if (kind.isEmpty() || mediaUrl == null || !mediaUrl.startsWith(prefix)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择本人上传的聊天附件");
        }
        String fileName = mediaUrl.substring(prefix.length());
        if (!fileName.matches(FILE_NAME_PATTERN)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "附件地址不合法");
        }
        Path directory = Paths.get(System.getProperty("user.dir"), "uploads", "consult-chat", userId.toString(), kind)
                .toAbsolutePath().normalize();
        Path path = directory.resolve(fileName).normalize();
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "附件不存在");
        }
        return path;
    }

    /** 下载时重新核验消息和当前参与关系，复制 URL 本身不构成访问授权。 */
    public Path resolveForDownload(Long ownerUserId, String kind, String fileName) {
        if (ownerUserId == null || ownerUserId <= 0 || !("image".equals(kind) || "file".equals(kind))
                || fileName == null || !fileName.matches(FILE_NAME_PATTERN)) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "附件不存在");
        }
        String mediaUrl = "/uploads/consult-chat/" + ownerUserId + "/" + kind + "/" + fileName;
        Long currentUserId = dataPermissionService.currentUser().getId();
        boolean admin = dataPermissionService.isAdmin();
        int consultReferences = consultMessageMapper.countMediaReferences(mediaUrl);
        int farmReferences = farmChatMessageMapper.countMediaReferences(mediaUrl);
        boolean accessible = consultReferences > 0
                && consultMessageMapper.countAccessibleMedia(mediaUrl, currentUserId, admin) > 0
                || farmReferences > 0
                && farmChatMessageMapper.countAccessibleMedia(mediaUrl, currentUserId, admin) > 0;
        // 尚未发送的附件仅供上传者预览；发送后必须按对应会话的当前授权判断。
        if (!accessible && (consultReferences > 0 || farmReferences > 0 || !ownerUserId.equals(currentUserId))) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "无权读取该聊天附件");
        }
        Path path = Paths.get(System.getProperty("user.dir"), "uploads", "consult-chat",
                ownerUserId.toString(), kind, fileName).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "附件不存在");
        }
        return path;
    }
}
