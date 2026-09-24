package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.ClientPictureBatchRequest;
import com.smart_plant.smart_plant.dto.ClientPictureDownload;
import com.smart_plant.smart_plant.dto.ClientPictureListResponse;
import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.entity.CameraImage;
import com.smart_plant.smart_plant.entity.CropImage;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CameraImageMapper;
import com.smart_plant.smart_plant.mapper.CropImageMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.ClientPictureService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * farm 用户端图片服务实现。
 *
 * <p>用户端图片页只需要“图片时间 + 图片 URL”，因此这里直接复用已有图片 Mapper，
 * 并按当前登录用户 ID 做强制过滤，避免客户端传参导致越权。</p>
 */
@Service
@RequiredArgsConstructor
public class ClientPictureServiceImpl implements ClientPictureService {

    private static final String SOURCE_TYPE_PHONE = "phone";
    private static final String SOURCE_TYPE_CAMERA = "camera";
    private static final String APP_CONTEXT_PATH = "smart_plant";
    private static final String UPLOAD_ROOT = "uploads";
    private static final long MAX_SINGLE_DOWNLOAD_BYTES = 20L * 1024L * 1024L;
    private static final long MAX_BATCH_DOWNLOAD_BYTES = 100L * 1024L * 1024L;

    /** 手机图片 Mapper，对应 crop_image 表。 */
    private final CropImageMapper cropImageMapper;

    /** 摄像头采集图片 Mapper，对应 camera_capture_record 表。 */
    private final CameraImageMapper cameraImageMapper;

    /** 数据权限服务，用于从 token 获取当前用户身份。 */
    private final DataPermissionService dataPermissionService;

    /** 复用已有手机图片文件校验与存储能力。 */
    private final CropImageService cropImageService;

    @Override
    public ClientPictureListResponse listCurrentClientPictures() {
        ClientPictureListResponse response = new ClientPictureListResponse();

        List<ClientPictureListResponse.PictureItem> phoneImages = listCurrentClientPhonePictures();
        List<ClientPictureListResponse.PictureItem> cameraImages = listCurrentClientCameraPictures();

        response.getPhoneImages().addAll(phoneImages);
        response.getCameraImages().addAll(cameraImages);

        // pictures 是接口的主列表，统一返回手机图片和摄像头图片，按图片产生时间倒序展示。
        response.getPictures().addAll(phoneImages);
        response.getPictures().addAll(cameraImages);
        response.getPictures().sort(Comparator.comparing(
                ClientPictureListResponse.PictureItem::getImageTime,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return response;
    }

    @Override
    public List<ClientPictureListResponse.PictureItem> listCurrentClientPhonePictures() {
        Long currentUserId = dataPermissionService.currentUser().getId();
        // 手机图片来自 crop_image 表，查询条件固定为当前登录用户，避免跨用户查看图片。
        return cropImageMapper.selectList(currentUserId, null, null)
                .stream()
                .map(this::toPhonePictureItem)
                .toList();
    }

    @Override
    public List<ClientPictureListResponse.PictureItem> listCurrentClientCameraPictures() {
        Long currentUserId = dataPermissionService.currentUser().getId();
        // 摄像头图片来自 camera_capture_record 表，Mapper 会同时过滤已删除记录。
        return cameraImageMapper.selectList(currentUserId, null, null, null, null, null, null)
                .stream()
                .map(this::toCameraPictureItem)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientPictureListResponse.PictureItem uploadCurrentClientPhonePicture(MultipartFile image) {
        Long currentUserId = dataPermissionService.currentUser().getId();
        CropImageUploadResult uploadResult = cropImageService.uploadCropImage(image);
        CropImage cropImage = new CropImage();
        cropImage.setUserId(currentUserId);
        cropImage.setImageUrl(uploadResult.imageUrl());
        cropImage.setImageSize(uploadResult.imageSize());
        cropImage.setRemark("farm用户端图片管理上传");
        return toPhonePictureItem(cropImageService.addCropImage(cropImage));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCurrentClientPictures(List<ClientPictureBatchRequest.PictureRef> pictures) {
        List<ClientPictureListResponse.PictureItem> checkedPictures = resolveOwnedPictures(pictures);
        List<Long> phoneIds = checkedPictures.stream()
                .filter(picture -> SOURCE_TYPE_PHONE.equals(picture.getSourceType()))
                .map(ClientPictureListResponse.PictureItem::getId)
                .toList();
        List<Long> cameraIds = checkedPictures.stream()
                .filter(picture -> SOURCE_TYPE_CAMERA.equals(picture.getSourceType()))
                .map(ClientPictureListResponse.PictureItem::getId)
                .toList();

        int deletedCount = 0;
        if (!phoneIds.isEmpty()) {
            deletedCount += cropImageMapper.deleteBatchByIds(phoneIds);
        }
        if (!cameraIds.isEmpty()) {
            deletedCount += cameraImageMapper.deleteBatchByIds(cameraIds);
        }
        return deletedCount;
    }

    @Override
    public List<ClientPictureListResponse.PictureItem> prepareCurrentClientPictureDownloads(List<ClientPictureBatchRequest.PictureRef> pictures) {
        // 下载前只做权限校验并返回可访问地址，真正保存到相册由 farm 端调用系统能力完成。
        return resolveOwnedPictures(pictures);
    }

    @Override
    public ClientPictureDownload downloadCurrentClientPicture(ClientPictureBatchRequest.PictureRef picture) {
        ClientPictureListResponse.PictureItem checkedPicture = resolveOwnedPicture(
                picture,
                dataPermissionService.currentUser().getId()
        );
        return buildPictureDownload(checkedPicture);
    }

    @Override
    public ClientPictureDownload downloadCurrentClientPictures(List<ClientPictureBatchRequest.PictureRef> pictures) {
        List<ClientPictureListResponse.PictureItem> checkedPictures = resolveOwnedPictures(pictures);
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            Set<String> usedNames = new LinkedHashSet<>();
            long totalBytes = 0;
            for (ClientPictureListResponse.PictureItem picture : checkedPictures) {
                ClientPictureDownload download = buildPictureDownload(picture);
                totalBytes += download.bytes().length;
                if (totalBytes > MAX_BATCH_DOWNLOAD_BYTES) {
                    throw new BusinessException(ResponseCode.PARAM_ERROR, "批量下载图片不能超过100MB");
                }
                // zip 内文件名需要去重，避免同名图片在压缩包中相互覆盖。
                ZipEntry entry = new ZipEntry(resolveUniqueZipEntryName(download.fileName(), usedNames));
                zipOutputStream.putNextEntry(entry);
                zipOutputStream.write(download.bytes());
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
            return new ClientPictureDownload(byteArrayOutputStream.toByteArray(), "farm-pictures.zip", "application/zip");
        } catch (IOException exception) {
            throw new BusinessException(ResponseCode.FAIL, "图片打包下载失败");
        }
    }

    /** 将手机图片实体转换为用户端图片行，时间取 crop_image.create_time。 */
    private ClientPictureListResponse.PictureItem toPhonePictureItem(CropImage image) {
        return new ClientPictureListResponse.PictureItem(
                image.getId(),
                "phone",
                image.getImageUrl(),
                image.getCreateTime()
        );
    }

    /** 将摄像头图片实体转换为用户端图片行，时间优先取采集时间，缺失时用创建时间兜底。 */
    private ClientPictureListResponse.PictureItem toCameraPictureItem(CameraImage image) {
        LocalDateTime imageTime = image.getCaptureTime() != null ? image.getCaptureTime() : image.getCreateTime();
        return new ClientPictureListResponse.PictureItem(
                image.getId(),
                "camera",
                image.getImageUrl(),
                imageTime
        );
    }

    /** 解析并校验当前用户选中的图片，防止用户通过篡改 id 删除或下载他人图片。 */
    private List<ClientPictureListResponse.PictureItem> resolveOwnedPictures(List<ClientPictureBatchRequest.PictureRef> pictures) {
        if (pictures == null || pictures.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择需要操作的图片");
        }
        Long currentUserId = dataPermissionService.currentUser().getId();
        List<ClientPictureListResponse.PictureItem> result = new ArrayList<>();
        for (ClientPictureBatchRequest.PictureRef picture : pictures) {
            result.add(resolveOwnedPicture(picture, currentUserId));
        }
        return result;
    }

    /** 根据图片来源路由到对应表查询，并校验图片归属当前登录用户。 */
    private ClientPictureListResponse.PictureItem resolveOwnedPicture(ClientPictureBatchRequest.PictureRef picture, Long currentUserId) {
        if (picture == null || picture.getId() == null || !StringUtils.hasText(picture.getSourceType())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "图片参数不完整");
        }
        if (SOURCE_TYPE_PHONE.equals(picture.getSourceType())) {
            CropImage image = cropImageMapper.selectById(picture.getId());
            if (image == null || !currentUserId.equals(image.getUserId())) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "手机图片不存在");
            }
            return toPhonePictureItem(image);
        }
        if (SOURCE_TYPE_CAMERA.equals(picture.getSourceType())) {
            CameraImage image = cameraImageMapper.selectById(picture.getId());
            if (image == null || !currentUserId.equals(image.getUserId())) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "摄像头图片不存在");
            }
            return toCameraPictureItem(image);
        }
        throw new BusinessException(ResponseCode.PARAM_ERROR, "图片来源类型不合法");
    }

    /** 根据图片地址读取真实文件内容，并生成浏览器或 App 可识别的下载响应信息。 */
    private ClientPictureDownload buildPictureDownload(ClientPictureListResponse.PictureItem picture) {
        String imageUrl = normalizeRequiredText(picture.getImageUrl(), "图片地址不能为空");
        DownloadSource source = loadDownloadSource(imageUrl);
        String contentType = chooseContentType(source.contentType(), imageUrl);
        String fileName = chooseFileName(picture, imageUrl, contentType);
        return new ClientPictureDownload(source.bytes(), fileName, contentType);
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
            if (contentLength > MAX_SINGLE_DOWNLOAD_BYTES) {
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
            Path path = resolveLocalImagePath(imageUrl);

            if (!Files.isRegularFile(path)) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "图片文件不存在");
            }
            if (Files.size(path) > MAX_SINGLE_DOWNLOAD_BYTES) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "图片文件不能超过20MB");
            }

            return new DownloadSource(Files.readAllBytes(path), Files.probeContentType(path));
        } catch (IOException exception) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "图片文件读取失败");
        }
    }

    /** 将数据库中保存的 /uploads 或 /smart_plant/uploads 地址转换为服务端真实文件路径。 */
    private Path resolveLocalImagePath(String imageUrl) {
        String normalizedUrl = imageUrl.replace("\\", "/").trim();
        String normalizedWithoutSlash = normalizedUrl.startsWith("/")
                ? normalizedUrl.substring(1)
                : normalizedUrl;
        if (normalizedWithoutSlash.startsWith(APP_CONTEXT_PATH + "/" + UPLOAD_ROOT + "/")) {
            normalizedWithoutSlash = normalizedWithoutSlash.substring(APP_CONTEXT_PATH.length() + 1);
        }
        if (normalizedWithoutSlash.startsWith(UPLOAD_ROOT + "/")) {
            return Paths.get(System.getProperty("user.dir")).resolve(normalizedWithoutSlash).normalize();
        }

        Path path = Paths.get(imageUrl);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Paths.get(System.getProperty("user.dir")).resolve(normalizedWithoutSlash).normalize();
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;

        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_SINGLE_DOWNLOAD_BYTES) {
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
        String extension = extensionFromName(imageUrl);
        if (extension != null) {
            String normalizedExtension = extension.replace(".", "").toLowerCase(Locale.ROOT);
            if ("jpg".equals(normalizedExtension)) {
                normalizedExtension = "jpeg";
            }
            return "image/" + normalizedExtension;
        }
        return "application/octet-stream";
    }

    private String chooseFileName(ClientPictureListResponse.PictureItem picture, String imageUrl, String contentType) {
        String baseName = fileNameFromUrl(imageUrl);
        if (baseName == null) {
            baseName = SOURCE_TYPE_PHONE.equals(picture.getSourceType())
                    ? "phone-picture-" + picture.getId()
                    : "camera-picture-" + picture.getId();
        }
        if (extensionFromName(baseName) == null) {
            String extension = extensionFromContentType(contentType);
            if (extension != null) {
                baseName = baseName + "." + extension;
            }
        }
        return sanitizeFileName(baseName);
    }

    private String resolveUniqueZipEntryName(String fileName, Set<String> usedNames) {
        String sanitizedName = sanitizeFileName(fileName);
        String candidate = sanitizedName;
        String extension = extensionFromName(sanitizedName);
        String nameWithoutExtension = extension == null
                ? sanitizedName
                : sanitizedName.substring(0, sanitizedName.length() - extension.length() - 1);
        int index = 1;
        while (usedNames.contains(candidate)) {
            candidate = extension == null
                    ? nameWithoutExtension + "-" + index
                    : nameWithoutExtension + "-" + index + "." + extension;
            index++;
        }
        usedNames.add(candidate);
        return candidate;
    }

    private String fileNameFromUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl) || imageUrl.startsWith("data:image/")) {
            return null;
        }
        String normalized = imageUrl.split("[?#]", 2)[0];
        int slashIndex = Math.max(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\'));
        String fileName = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
        return StringUtils.hasText(fileName) ? fileName.trim() : null;
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

    private String normalizeRequiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
        return value.trim();
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
    }

    private record DownloadSource(byte[] bytes, String contentType) {
    }
}
