package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.SystemAssetUploadResponse;
import com.smart_plant.smart_plant.dto.SystemSettingUpdateRequest;
import com.smart_plant.smart_plant.entity.SystemSetting;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.SystemSettingMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import com.smart_plant.smart_plant.service.OperationLogService;
import com.smart_plant.smart_plant.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** 系统设置业务实现，统一负责权限、格式、安全白名单和并发更新校验。 */
@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private static final String ROLE_ADMIN = "admin";
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern EXTENSION_PATTERN = Pattern.compile("^[a-z0-9]{1,10}$");
    private static final Set<String> SAFE_UPLOAD_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "pdf", "xlsx", "xls", "csv", "doc", "docx", "txt", "zip"
    );
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/x-icon", "image/vnd.microsoft.icon"
    );

    private final SystemSettingMapper systemSettingMapper;
    private final OperationLogService operationLogService;

    @Override
    public SystemSetting getPublicSetting() {
        return requireSetting();
    }

    @Override
    public SystemSetting getAdminSetting() {
        requireAdmin();
        return requireSetting();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemSetting updateSetting(SystemSettingUpdateRequest request) {
        User administrator = requireAdmin();
        validateAndNormalize(request);
        if (systemSettingMapper.updateSingleton(request, administrator.getId()) != 1) {
            throw new BusinessException(ResponseCode.FAIL, "系统设置已被其他管理员修改，请刷新后重试");
        }
        operationLogService.record("修改系统设置");
        return requireSetting();
    }

    @Override
    public SystemAssetUploadResponse uploadAsset(String type, MultipartFile file) {
        requireAdmin();
        AssetType assetType = AssetType.from(type);
        validateImage(file);

        String contentType = file.getContentType().trim().toLowerCase(Locale.ROOT);
        String extension = extensionFor(contentType);
        LocalDate today = LocalDate.now();
        Path uploadDirectory = Paths.get(System.getProperty("user.dir"))
                .resolve("uploads").resolve("system-settings")
                .resolve(assetType.pathName).resolve(today.toString()).normalize();
        String fileName = UUID.randomUUID() + "." + extension;

        try {
            Files.createDirectories(uploadDirectory);
            Path targetPath = uploadDirectory.resolve(fileName).normalize();
            if (!targetPath.startsWith(uploadDirectory)) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "系统资源文件名不合法");
            }
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new BusinessException(ResponseCode.FAIL, "系统资源上传失败");
        }

        String url = "/uploads/system-settings/" + assetType.pathName + "/" + today + "/" + fileName;
        return new SystemAssetUploadResponse(assetType.requestName, url, file.getSize());
    }

    /** 保存前完成字段标准化，确保数据库和前端始终收到一致格式。 */
    private void validateAndNormalize(SystemSettingUpdateRequest request) {
        if (request == null || request.getVersion() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "系统设置版本号不能为空");
        }
        request.setSystemName(requiredText(request.getSystemName(), 100, "系统名称"));
        request.setHomeTitle(requiredText(request.getHomeTitle(), 100, "首页标题"));
        request.setThemeColor(requiredText(request.getThemeColor(), 7, "系统主题色").toLowerCase(Locale.ROOT));
        if (!COLOR_PATTERN.matcher(request.getThemeColor()).matches()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "系统主题色必须为#RRGGBB格式");
        }
        request.setTimezone(requiredText(request.getTimezone(), 64, "时区"));
        try {
            ZoneId.of(request.getTimezone());
        } catch (Exception exception) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择有效的IANA时区");
        }
        request.setDatetimeFormat(requiredText(request.getDatetimeFormat(), 64, "日期时间格式"));
        try {
            DateTimeFormatter.ofPattern(request.getDatetimeFormat());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "日期时间格式不合法");
        }
        validateRange(request.getDefaultPageSize(), 5, 200, "分页默认条数");
        validateRange(request.getMaxUploadSizeMb(), 1, 50, "文件上传大小限制");
        request.setAllowedUploadTypes(normalizeUploadTypes(request.getAllowedUploadTypes()));
        request.setLogoUrl(optionalText(request.getLogoUrl(), 255, "Logo地址"));
        request.setFaviconUrl(optionalText(request.getFaviconUrl(), 255, "网站图标地址"));
        request.setLoginBackgroundUrl(optionalText(request.getLoginBackgroundUrl(), 255, "登录页背景地址"));
        request.setSystemDescription(optionalText(request.getSystemDescription(), 500, "系统描述"));
        request.setCopyrightInfo(optionalText(request.getCopyrightInfo(), 255, "版权信息"));
        request.setRecordNumber(optionalText(request.getRecordNumber(), 100, "备案号"));
        request.setOfficialWebsite(optionalHttpUrl(request.getOfficialWebsite(), "官网地址"));
        request.setContactEmail(optionalText(request.getContactEmail(), 100, "联系邮箱"));
        if (StringUtils.hasText(request.getContactEmail())
                && !EMAIL_PATTERN.matcher(request.getContactEmail()).matches()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "联系邮箱格式不正确");
        }
        request.setServicePhone(optionalText(request.getServicePhone(), 30, "客服电话"));
    }

    private SystemSetting requireSetting() {
        SystemSetting setting = systemSettingMapper.selectSingleton();
        if (setting == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "系统设置尚未初始化");
        }
        return setting;
    }

    private User requireAdmin() {
        User user = CurrentUserContext.get();
        if (user == null || user.getId() == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        if (!ROLE_ADMIN.equals(normalize(user.getRoleCode()))) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "只有管理员可以修改系统设置");
        }
        return user;
    }

    private String normalizeUploadTypes(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "允许上传文件类型不能为空");
        }
        Set<String> extensions = new LinkedHashSet<>();
        Arrays.stream(value.split(","))
                .map(item -> normalize(item).replaceFirst("^\\.", ""))
                .filter(StringUtils::hasText)
                .forEach(extension -> {
                    if (!EXTENSION_PATTERN.matcher(extension).matches()
                            || !SAFE_UPLOAD_EXTENSIONS.contains(extension)) {
                        throw new BusinessException(ResponseCode.PARAM_ERROR, "包含不安全或不支持的上传文件类型：" + extension);
                    }
                    extensions.add(extension);
                });
        if (extensions.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "允许上传文件类型不能为空");
        }
        return String.join(",", extensions);
    }

    private String optionalHttpUrl(String value, String fieldName) {
        String normalized = optionalText(value, 255, fieldName);
        if (normalized == null) {
            return null;
        }
        try {
            URI uri = URI.create(normalized);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || !StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException();
            }
        } catch (Exception exception) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, fieldName + "必须是合法的HTTP或HTTPS地址");
        }
        return normalized;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择需要上传的图片");
        }
        SystemSetting setting = requireSetting();
        long maxBytes = setting.getMaxUploadSizeMb().longValue() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ResponseCode.PARAM_ERROR,
                    "图片大小不能超过" + setting.getMaxUploadSizeMb() + "MB");
        }
        String contentType = file.getContentType() == null ? "" : normalize(file.getContentType());
        if (!IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "仅支持JPG、PNG、WEBP或ICO图片");
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/x-icon", "image/vnd.microsoft.icon" -> "ico";
            default -> contentType.substring("image/".length());
        };
    }

    private String requiredText(String value, int maxLength, String fieldName) {
        String normalized = optionalText(value, maxLength, fieldName);
        if (normalized == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, fieldName + "不能为空");
        }
        return normalized;
    }

    private String optionalText(String value, int maxLength, String fieldName) {
        String normalized = StringUtils.hasText(value) ? value.trim() : null;
        if (normalized != null && normalized.length() > maxLength) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, fieldName + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }

    private void validateRange(Integer value, int min, int max, String fieldName) {
        if (value == null || value < min || value > max) {
            throw new BusinessException(ResponseCode.PARAM_ERROR,
                    fieldName + "必须在" + min + "到" + max + "之间");
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    /** 支持的品牌图片类型，同时负责生成隔离的上传目录。 */
    private enum AssetType {
        LOGO("logo", "logo"),
        FAVICON("favicon", "favicon"),
        LOGIN_BACKGROUND("loginBackground", "login-background");

        private final String requestName;
        private final String pathName;

        AssetType(String requestName, String pathName) {
            this.requestName = requestName;
            this.pathName = pathName;
        }

        private static AssetType from(String value) {
            return Arrays.stream(values())
                    .filter(item -> item.requestName.equals(value))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            ResponseCode.PARAM_ERROR, "资源类型仅支持logo、favicon或loginBackground"));
        }
    }
}
