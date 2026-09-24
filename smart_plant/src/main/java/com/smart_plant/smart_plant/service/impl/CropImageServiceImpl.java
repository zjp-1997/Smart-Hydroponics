package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.CropImageDownload;
import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.entity.CropImage;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CropImageMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.CropImageService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CropImageServiceImpl implements CropImageService {

    private static final long MAX_DOWNLOAD_BYTES = 20L * 1024L * 1024L;
    private static final long MAX_UPLOAD_BYTES = 5L * 1024L * 1024L;
    private static final String LOCAL_IMAGE_STORAGE_PREFIX = "smart_farm_phone_picture:";
    private static final String UPLOAD_ROOT = "uploads";
    private static final String PHONE_IMAGE_UPLOAD_DIR = "phone-images";

    private static final String PLOT_IMAGE_UPLOAD_DIR = "plot-images";
    private static final String TASK_COMPLETION_IMAGE_UPLOAD_DIR = "task-completion-images";
    private static final String FAULT_COMPLETION_IMAGE_UPLOAD_DIR = "fault-completion-images";
    private static final Set<String> ALLOWED_UPLOAD_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final CropImageMapper cropImageMapper;
    private final UserMapper userMapper;
    private final DataPermissionService dataPermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CropImage addCropImage(CropImage cropImage) {
        validateCreate(cropImage);
        normalizeDefaults(cropImage);
        cropImage.setUserId(resolveWritableUserId(cropImage.getUserId()));
        ensureUserExists(cropImage.getUserId());
        cropImageMapper.insert(cropImage);
        return cropImageMapper.selectById(cropImage.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCropImage(Long id) {
        CropImage oldImage = getCropImageById(id);
        int rows = cropImageMapper.deleteById(oldImage.getId());
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "手机图片不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCropImages(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的手机图片");
        }
        for (Long id : ids) {
            getCropImageById(id);
        }
        return cropImageMapper.deleteBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CropImage updateCropImage(CropImage cropImage) {
        if (cropImage == null || cropImage.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "手机图片ID不能为空");
        }
        CropImage oldImage = cropImageMapper.selectById(cropImage.getId());
        if (oldImage == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "手机图片不存在");
        }
        dataPermissionService.requireOwnedResource(oldImage.getUserId());
        normalizeUpdateFields(cropImage, oldImage);
        validateCommon(cropImage);
        cropImage.setUserId(resolveWritableUserId(cropImage.getUserId()));
        ensureUserExists(cropImage.getUserId());
        int rows = cropImageMapper.updateById(cropImage);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "手机图片修改失败");
        }
        return cropImageMapper.selectById(cropImage.getId());
    }

    @Override
    public CropImage getCropImageById(Long id) {
        requireId(id);
        CropImage cropImage = cropImageMapper.selectById(id);
        if (cropImage == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "手机图片不存在");
        }
        dataPermissionService.requireOwnedResource(cropImage.getUserId());
        return cropImage;
    }

    @Override
    public CropImageDownload downloadCropImage(Long id) {
        CropImage cropImage = getCropImageById(id);
        String imageUrl = normalizeRequiredText(cropImage.getImageUrl(), "图片地址不能为空");

        if (imageUrl.startsWith(LOCAL_IMAGE_STORAGE_PREFIX)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "本地缓存图片请在前端下载");
        }

        DownloadSource source = loadDownloadSource(imageUrl);
        String contentType = chooseContentType(source.contentType(), imageUrl);
        String fileName = chooseFileName(cropImage, imageUrl, contentType);

        return new CropImageDownload(source.bytes(), fileName, contentType);
    }

    @Override
    public CropImageUploadResult uploadCropImage(MultipartFile file) {
        return storeCropImage(file, PHONE_IMAGE_UPLOAD_DIR);
    }

    @Override
    public CropImageUploadResult uploadPlotCropImage(MultipartFile file) {
        return storeCropImage(file, PLOT_IMAGE_UPLOAD_DIR);
    }

    /** 完成凭证沿用统一的类型、大小和路径穿越校验，仅调整存储目录。 */
    @Override
    public CropImageUploadResult uploadTaskCompletionImage(MultipartFile file) {
        return storeCropImage(file, TASK_COMPLETION_IMAGE_UPLOAD_DIR);
    }

    /** 故障凭证使用独立目录，复用上传入口的 5MB 和图片类型校验。 */
    @Override
    public CropImageUploadResult uploadFaultCompletionImage(MultipartFile file) {
        return storeCropImage(file, FAULT_COMPLETION_IMAGE_UPLOAD_DIR);
    }

    private CropImageUploadResult storeCropImage(MultipartFile file, String uploadDirectory) {
        validateUploadFile(file);
        String contentType = normalizeOptionalText(file.getContentType());
        String extension = extensionFromContentType(contentType);
        if (extension == null) {
            extension = extensionFromName(file.getOriginalFilename());
        }
        if (extension == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "图片文件类型不支持");
        }

        LocalDate today = LocalDate.now();
        Path uploadDir = Paths.get(System.getProperty("user.dir"))
                .resolve(UPLOAD_ROOT)
                .resolve(uploadDirectory)
                .resolve(today.toString());
        String fileName = UUID.randomUUID() + "." + extension.replace(".", "").toLowerCase(Locale.ROOT);

        try {
            Files.createDirectories(uploadDir);
            Path targetPath = uploadDir.resolve(fileName).normalize();
            if (!targetPath.startsWith(uploadDir.normalize())) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "图片文件名不合法");
            }
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new BusinessException(ResponseCode.FAIL, "图片上传失败");
        }

        String imageUrl = "/" + UPLOAD_ROOT + "/" + uploadDirectory + "/" + today + "/" + fileName;
        return new CropImageUploadResult(imageUrl, file.getSize());
    }

    @Override
    public PageInfo<CropImage> listCropImages(Long userId, LocalDate startDate, LocalDate endDate,
                                              Integer pageNum, Integer pageSize) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }
        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = dataPermissionService.restrictUserId(userId);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(cropImageMapper.selectList(
                scopedUserId,
                startTime,
                endTime));
    }

    private void validateCreate(CropImage cropImage) {
        if (cropImage == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "手机图片信息不能为空");
        }
        if (!StringUtils.hasText(cropImage.getImageUrl())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "图片地址不能为空");
        }
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择需要上传的图片");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "图片大小不能超过5MB");
        }
        String contentType = normalizeOptionalText(file.getContentType());
        if (contentType == null || !ALLOWED_UPLOAD_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "仅支持JPG、PNG或WEBP图片");
        }
    }

    private void normalizeDefaults(CropImage cropImage) {
        cropImage.setImageUrl(normalizeRequiredText(cropImage.getImageUrl(), "图片地址不能为空"));
        cropImage.setRemark(normalizeOptionalText(cropImage.getRemark()));
        validateCommon(cropImage);
    }

    private void normalizeUpdateFields(CropImage cropImage, CropImage oldImage) {
        cropImage.setUserId(cropImage.getUserId() == null ? oldImage.getUserId() : cropImage.getUserId());
        cropImage.setImageUrl(cropImage.getImageUrl() == null ? oldImage.getImageUrl() : normalizeOptionalText(cropImage.getImageUrl()));
        cropImage.setImageSize(cropImage.getImageSize() == null ? oldImage.getImageSize() : cropImage.getImageSize());
        cropImage.setRemark(cropImage.getRemark() == null ? oldImage.getRemark() : normalizeOptionalText(cropImage.getRemark()));
    }

    private void validateCommon(CropImage cropImage) {
        if (!StringUtils.hasText(cropImage.getImageUrl())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "图片地址不能为空");
        }
        if (cropImage.getImageSize() != null && cropImage.getImageSize() < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "图片大小不能小于0");
        }
        validateLength(cropImage.getImageUrl(), 500, "图片地址不能超过500个字符");
        validateLength(cropImage.getRemark(), 500, "备注不能超过500个字符");
    }

    private Long resolveWritableUserId(Long requestedUserId) {
        if (dataPermissionService.isAdmin()) {
            return requestedUserId == null ? dataPermissionService.currentUser().getId() : requestedUserId;
        }
        return dataPermissionService.restrictUserId(requestedUserId);
    }

    private void ensureUserExists(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "上传用户不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "上传用户不存在");
        }
    }

    private String normalizeRequiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "手机图片ID不能为空");
        }
    }

    private DownloadSource loadDownloadSource(String imageUrl) {
        if (imageUrl.startsWith("data:image/")) {
            return loadDataUrl(imageUrl);
        }

        String lowerUrl = imageUrl.toLowerCase(Locale.ROOT);
        if (lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")) {
            return loadRemoteUrl(imageUrl);
        }

        return loadLocalFile(imageUrl);
    }

    private DownloadSource loadDataUrl(String imageUrl) {
        int commaIndex = imageUrl.indexOf(',');
        if (commaIndex < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "图片Data URL格式错误");
        }
        String meta = imageUrl.substring(5, commaIndex);
        String data = imageUrl.substring(commaIndex + 1);
        if (!meta.toLowerCase(Locale.ROOT).contains(";base64")) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "仅支持Base64图片Data URL下载");
        }
        try {
            return new DownloadSource(Base64.getDecoder().decode(data), meta.replace(";base64", ""));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "图片Data URL内容错误");
        }
    }

    private DownloadSource loadRemoteUrl(String imageUrl) {
        try {
            URLConnection connection = URI.create(imageUrl).toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);

            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_DOWNLOAD_BYTES) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "图片文件不能超过20MB");
            }

            try (InputStream inputStream = connection.getInputStream()) {
                return new DownloadSource(readAllBytes(inputStream), connection.getContentType());
            }
        } catch (IOException exception) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "图片文件读取失败");
        }
    }

    private DownloadSource loadLocalFile(String imageUrl) {
        try {
            Path path = Paths.get(imageUrl);
            if (!path.isAbsolute()) {
                String normalized = imageUrl.startsWith("/") || imageUrl.startsWith("\\")
                        ? imageUrl.substring(1)
                        : imageUrl;
                path = Paths.get(System.getProperty("user.dir")).resolve(normalized);
            }
            path = path.normalize();

            if (!Files.isRegularFile(path)) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "图片文件不存在");
            }
            if (Files.size(path) > MAX_DOWNLOAD_BYTES) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "图片文件不能超过20MB");
            }

            return new DownloadSource(Files.readAllBytes(path), Files.probeContentType(path));
        } catch (IOException exception) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "图片文件读取失败");
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;

        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_DOWNLOAD_BYTES) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "图片文件不能超过20MB");
            }
            outputStream.write(buffer, 0, read);
        }

        return outputStream.toByteArray();
    }

    private String chooseContentType(String sourceContentType, String imageUrl) {
        if (StringUtils.hasText(sourceContentType) && sourceContentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return sourceContentType.split(";", 2)[0].trim();
        }
        String type = extensionFromName(imageUrl);
        if (type != null) {
            type = type.replace(".", "").toLowerCase(Locale.ROOT);
            if ("jpg".equals(type)) {
                type = "jpeg";
            }
            return "image/" + type;
        }
        return "application/octet-stream";
    }

    private String chooseFileName(CropImage cropImage, String imageUrl, String contentType) {
        String baseName = fileNameFromUrl(imageUrl);
        if (baseName == null) {
            baseName = "phone-picture-" + cropImage.getId();
        }
        if (extensionFromName(baseName) == null) {
            String extension = extensionFromContentType(contentType);
            if (extension != null) {
                baseName = baseName + "." + extension;
            }
        }
        return sanitizeFileName(baseName);
    }

    private String fileNameFromUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl) || imageUrl.startsWith("data:image/")) {
            return null;
        }
        String normalized = imageUrl.split("[?#]", 2)[0];
        int slashIndex = Math.max(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\'));
        String fileName = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
        return normalizeOptionalText(fileName);
    }

    private String extensionFromContentType(String contentType) {
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            return null;
        }
        String extension = contentType.substring("image/".length()).toLowerCase(Locale.ROOT);
        return "jpeg".equals(extension) ? "jpg" : extension;
    }

    private String extensionFromName(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        int slashIndex = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        int dotIndex = value.lastIndexOf('.');
        if (dotIndex <= slashIndex || dotIndex == value.length() - 1) {
            return null;
        }
        return value.substring(dotIndex + 1);
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
    }

    private record DownloadSource(byte[] bytes, String contentType) {
    }
}
