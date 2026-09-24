package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.ManagedImageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ManagedImageServiceImpl implements ManagedImageService {

    private static final long MAX_UPLOAD_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Map<String, String> CATEGORY_DIRECTORIES = Map.of(
            "crop", "crop-images",
            "disease", "disease-images",
            "user-avatar", "user-avatars",
            "oauth-avatar", "oauth-avatars",
            "planting-batch", "planting-batch-images",
            // 仓库图片使用独立目录，避免与作物、用户等业务资源混放。
            "warehouse", "warehouse-images"
    );

    @Override
    public CropImageUploadResult upload(MultipartFile file, String category) {
        validate(file);
        String uploadDirectory = CATEGORY_DIRECTORIES.get(normalize(category));
        if (uploadDirectory == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "图片业务类型不支持");
        }

        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new BusinessException(ResponseCode.PARAM_ERROR, "图片文件类型不支持");
        };
        LocalDate today = LocalDate.now();
        Path uploadDir = Paths.get(System.getProperty("user.dir"))
                .resolve("uploads")
                .resolve(uploadDirectory)
                .resolve(today.toString())
                .toAbsolutePath()
                .normalize();
        String fileName = UUID.randomUUID() + "." + extension;
        Path targetPath = uploadDir.resolve(fileName).normalize();
        if (!targetPath.startsWith(uploadDir)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "图片文件名不合法");
        }

        try {
            Files.createDirectories(uploadDir);
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new BusinessException(ResponseCode.FAIL, "图片上传失败");
        }

        return new CropImageUploadResult(
                "/uploads/" + uploadDirectory + "/" + today + "/" + fileName,
                file.getSize()
        );
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择需要上传的图片");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "图片大小不能超过5MB");
        }
        String contentType = normalize(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "仅支持JPG、PNG或WEBP图片");
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }
}
