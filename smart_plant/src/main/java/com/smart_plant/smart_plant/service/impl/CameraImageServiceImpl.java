package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.CameraImageDownload;
import com.smart_plant.smart_plant.entity.CameraDevice;
import com.smart_plant.smart_plant.entity.CameraImage;
import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CameraDeviceMapper;
import com.smart_plant.smart_plant.mapper.CameraImageMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.CameraImageService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

@Service
@RequiredArgsConstructor
public class CameraImageServiceImpl implements CameraImageService {

    private static final long MAX_DOWNLOAD_BYTES = 20L * 1024L * 1024L;
    private static final String LOCAL_IMAGE_STORAGE_PREFIX = "smart_farm_camera_picture:";

    private final CameraImageMapper cameraImageMapper;
    private final CameraDeviceMapper cameraDeviceMapper;
    private final PlotMapper plotMapper;
    private final DataPermissionService dataPermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CameraImage addCameraImage(CameraImage cameraImage) {
        validateCreate(cameraImage);
        normalizeDefaults(cameraImage);
        CameraDevice cameraDevice = checkCameraExists(cameraImage.getCameraId());
        ensureCameraPlotMatched(cameraImage, cameraDevice);
        dataPermissionService.requireFarmManager(cameraDevice.getUserId());
        cameraImage.setUserId(cameraDevice.getUserId());
        cameraImageMapper.insert(cameraImage);
        return cameraImageMapper.selectById(cameraImage.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCameraImage(Long id) {
        CameraImage oldImage = getCameraImageById(id);
        int rows = cameraImageMapper.deleteById(oldImage.getId());
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "摄像头图片不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCameraImages(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择需要删除的摄像头图片");
        }
        for (Long id : ids) {
            getCameraImageById(id);
        }
        return cameraImageMapper.deleteBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CameraImage updateCameraImage(CameraImage cameraImage) {
        if (cameraImage == null || cameraImage.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "摄像头图片ID不能为空");
        }
        CameraImage oldImage = cameraImageMapper.selectById(cameraImage.getId());
        if (oldImage == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "摄像头图片不存在");
        }
        dataPermissionService.requireFarmManager(oldImage.getUserId());
        normalizeUpdateFields(cameraImage, oldImage);
        validateCommon(cameraImage);
        CameraDevice cameraDevice = checkCameraExists(cameraImage.getCameraId());
        ensureCameraPlotMatched(cameraImage, cameraDevice);
        dataPermissionService.requireFarmManager(cameraDevice.getUserId());
        int rows = cameraImageMapper.updateById(cameraImage);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "摄像头图片修改失败");
        }
        return cameraImageMapper.selectById(cameraImage.getId());
    }

    @Override
    public CameraImage getCameraImageById(Long id) {
        requireId(id);
        CameraImage cameraImage = cameraImageMapper.selectById(id);
        if (cameraImage == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "摄像头图片不存在");
        }
        dataPermissionService.requireFarmManager(cameraImage.getUserId());
        return cameraImage;
    }

    @Override
    public CameraImageDownload downloadCameraImage(Long id) {
        CameraImage cameraImage = getCameraImageById(id);
        String imageUrl = normalizeRequiredText(cameraImage.getImageUrl(), "图片地址不能为空");

        if (imageUrl.startsWith(LOCAL_IMAGE_STORAGE_PREFIX)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "本地缓存图片请在前端下载");
        }

        DownloadSource source = loadDownloadSource(imageUrl);
        String contentType = chooseContentType(source.contentType(), imageUrl);
        String fileName = chooseFileName(cameraImage, imageUrl, contentType);

        return new CameraImageDownload(source.bytes(), fileName, contentType);
    }

    @Override
    public PageInfo<CameraImage> listCameraImages(Long cameraId, Long plotId, String cameraName, String plotName,
                                                  LocalDate startDate, LocalDate endDate,
                                                  Integer pageNum, Integer pageSize) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }
        if (cameraId != null) {
            CameraDevice cameraDevice = checkCameraExists(cameraId);
            dataPermissionService.requireFarmManager(cameraDevice.getUserId());
            if (plotId != null && !plotId.equals(cameraDevice.getPlotId())) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "摄像头与地块不匹配");
            }
        }
        if (plotId != null) {
            Plot plot = checkPlotExists(plotId);
            dataPermissionService.requireFarmManager(plot.getUserId());
        }

        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(cameraImageMapper.selectList(
                scopedUserId,
                cameraId,
                plotId,
                normalizeOptionalText(cameraName),
                normalizeOptionalText(plotName),
                startTime,
                endTime));
    }

    private void validateCreate(CameraImage cameraImage) {
        if (cameraImage == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "摄像头图片信息不能为空");
        }
        if (cameraImage.getCameraId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属摄像头不能为空");
        }
        if (!StringUtils.hasText(cameraImage.getImageUrl())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "图片地址不能为空");
        }
    }

    private void normalizeDefaults(CameraImage cameraImage) {
        cameraImage.setImageUrl(normalizeRequiredText(cameraImage.getImageUrl(), "图片地址不能为空"));
        cameraImage.setRemark(normalizeOptionalText(cameraImage.getRemark()));
        if (cameraImage.getCaptureTime() == null) {
            cameraImage.setCaptureTime(LocalDateTime.now());
        }
        if (cameraImage.getCaptureType() == null) {
            cameraImage.setCaptureType(1);
        }
        if (cameraImage.getAiChecked() == null) {
            cameraImage.setAiChecked(0);
        }
        if (cameraImage.getStatus() == null) {
            cameraImage.setStatus(1);
        }
        validateCommon(cameraImage);
    }

    private void normalizeUpdateFields(CameraImage cameraImage, CameraImage oldImage) {
        cameraImage.setCameraId(cameraImage.getCameraId() == null ? oldImage.getCameraId() : cameraImage.getCameraId());
        cameraImage.setPlotId(cameraImage.getPlotId() == null ? oldImage.getPlotId() : cameraImage.getPlotId());
        cameraImage.setImageUrl(cameraImage.getImageUrl() == null ? oldImage.getImageUrl() : normalizeOptionalText(cameraImage.getImageUrl()));
        cameraImage.setImageSize(cameraImage.getImageSize() == null ? oldImage.getImageSize() : cameraImage.getImageSize());
        cameraImage.setCaptureTime(cameraImage.getCaptureTime() == null ? oldImage.getCaptureTime() : cameraImage.getCaptureTime());
        cameraImage.setCaptureType(cameraImage.getCaptureType() == null ? oldImage.getCaptureType() : cameraImage.getCaptureType());
        cameraImage.setAiChecked(cameraImage.getAiChecked() == null ? oldImage.getAiChecked() : cameraImage.getAiChecked());
        cameraImage.setStatus(cameraImage.getStatus() == null ? oldImage.getStatus() : cameraImage.getStatus());
        cameraImage.setRemark(cameraImage.getRemark() == null ? oldImage.getRemark() : normalizeOptionalText(cameraImage.getRemark()));
    }

    private void validateCommon(CameraImage cameraImage) {
        if (!StringUtils.hasText(cameraImage.getImageUrl())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "图片地址不能为空");
        }
        if (cameraImage.getImageSize() != null && cameraImage.getImageSize() < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "图片大小不能小于0");
        }
        validateLength(cameraImage.getImageUrl(), 500, "图片地址不能超过500个字符");
        validateLength(cameraImage.getRemark(), 500, "备注不能超过500个字符");
    }

    private CameraDevice checkCameraExists(Long cameraId) {
        if (cameraId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属摄像头不能为空");
        }
        CameraDevice cameraDevice = cameraDeviceMapper.selectById(cameraId);
        if (cameraDevice == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "所属摄像头不存在");
        }
        return cameraDevice;
    }

    private Plot checkPlotExists(Long plotId) {
        if (plotId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属地块不能为空");
        }
        Plot plot = plotMapper.selectById(plotId);
        if (plot == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "所属地块不存在");
        }
        return plot;
    }

    private void ensureCameraPlotMatched(CameraImage cameraImage, CameraDevice cameraDevice) {
        if (cameraImage.getPlotId() == null) {
            cameraImage.setPlotId(cameraDevice.getPlotId());
            return;
        }
        if (!cameraImage.getPlotId().equals(cameraDevice.getPlotId())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "摄像头与地块不匹配");
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
            throw new BusinessException(ResponseCode.PARAM_ERROR, "摄像头图片ID不能为空");
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

    private String chooseFileName(CameraImage cameraImage, String imageUrl, String contentType) {
        String baseName = fileNameFromUrl(imageUrl);
        if (baseName == null) {
            baseName = "camera-picture-" + cameraImage.getId();
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
