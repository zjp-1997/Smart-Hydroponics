package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.AdminLoginRequest;
import com.smart_plant.smart_plant.dto.AdminLoginResponse;
import com.smart_plant.smart_plant.dto.AdminUserInfoResponse;
import com.smart_plant.smart_plant.dto.AdminProfileUpdateRequest;
import com.smart_plant.smart_plant.dto.AdminPasswordChangeRequest;
import com.smart_plant.smart_plant.dto.AdminAvatarUploadResponse;
import com.smart_plant.smart_plant.dto.JwtPayload;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.exception.RateLimitException;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.mapper.RoleMapper;
import com.smart_plant.smart_plant.entity.Role;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import com.smart_plant.smart_plant.service.AdminAuthService;
import com.smart_plant.smart_plant.service.AdminLoginLogService;
import com.smart_plant.smart_plant.service.CaptchaService;
import com.smart_plant.smart_plant.service.JwtService;
import com.smart_plant.smart_plant.service.LoginSessionService;
import com.smart_plant.smart_plant.service.LoginMetricsService;
import com.smart_plant.smart_plant.service.LoginRateLimitService;
import com.smart_plant.smart_plant.service.PermissionAuthorizationService;
import com.smart_plant.smart_plant.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final String LOGIN_FAILED_MESSAGE = "账号或密码错误";

    private static final String ROLE_ADMIN = "admin";

    private static final String ROLE_FARM_OWNER = "farm_owner";

    private static final Set<String> BACKEND_LOGIN_ROLES = Set.of(ROLE_ADMIN, ROLE_FARM_OWNER);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final long MAX_AVATAR_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final CaptchaService captchaService;

    private final LoginSessionService loginSessionService;

    private final TokenBlacklistService tokenBlacklistService;

    private final LoginRateLimitService rateLimitService;

    private final AdminLoginLogService loginLogService;

    private final LoginMetricsService metricsService;

    private final PermissionAuthorizationService permissionAuthorizationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminLoginResponse login(AdminLoginRequest loginRequest, String ip, String userAgent) {
        String username = loginRequest == null ? null : loginRequest.getUsername();
        String password = loginRequest == null ? null : loginRequest.getPassword();
        boolean rememberMe = loginRequest != null && Boolean.TRUE.equals(loginRequest.getRememberMe());

        try {
            rateLimitService.check(ip, username);
        } catch (RateLimitException exception) {
            metricsService.recordRateLimited();
            loginLogService.record(null, safeUsername(username), false, "rate_limited", ip, userAgent);
            throw exception;
        }

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return fail(null, safeUsername(username), ip, userAgent, "bad_credentials");
        }

        captchaService.verify(loginRequest.getCaptchaKey(), loginRequest.getCaptcha());

        User user = userMapper.selectByUsername(username.trim());
        if (user == null || !matchesPasswordAndUpgradeIfNeeded(user, password)) {
            return fail(null, username, ip, userAgent, "bad_credentials");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            rateLimitService.recordFailure(ip, username);
            metricsService.recordFailure("disabled");
            loginLogService.record(user.getId(), safeUsername(username), false, "disabled", ip, userAgent);
            throw new BusinessException(ResponseCode.FORBIDDEN, "账号已冻结请联系管理员");
        }

        String roleCode = normalizeRoleCode(user.getRoleCode());
        if (!BACKEND_LOGIN_ROLES.contains(roleCode)) {
            rateLimitService.recordFailure(ip, username);
            metricsService.recordFailure("not_backend_role");
            loginLogService.record(user.getId(), safeUsername(username), false, "not_backend_role", ip, userAgent);
            throw new BusinessException(ResponseCode.FORBIDDEN, "当前账号无后台登录权限");
        }

        rateLimitService.recordSuccess(ip, username);
        userMapper.updateLastLogin(user.getId());
        User refreshedUser = userMapper.selectByUsername(username.trim());
        String refreshedRoleCode = normalizeRoleCode(refreshedUser.getRoleCode());
        metricsService.recordSuccess();
        loginLogService.record(refreshedUser.getId(), refreshedUser.getUsername(), true, null, ip, userAgent);
        return issueTokenPair(refreshedUser, refreshedRoleCode, rememberMe);
    }

    @Override
    public AdminLoginResponse refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "刷新令牌不能为空");
        }
        JwtPayload payload = jwtService.parseToken(refreshToken);
        if (!"refresh".equals(payload.getTokenType()) || tokenBlacklistService.isBlacklisted(payload.getJti())) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "刷新令牌无效");
        }
        if (!loginSessionService.isRefreshTokenActive(payload.getAdminId(), payload.getJti())) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }

        User user = userMapper.selectById(payload.getAdminId());
        validateActiveBackendUser(user);
        tokenBlacklistService.blacklist(payload.getJti(), payload.getExp());
        return issueTokenPair(user, normalizeRoleCode(user.getRoleCode()), Boolean.TRUE.equals(payload.getRememberMe()));
    }

    @Override
    public AdminUserInfoResponse currentUserInfo() {
        User user = CurrentUserContext.get();
        if (user == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        String roleCode = normalizeRoleCode(user.getRoleCode());
        return new AdminUserInfoResponse(
                user,
                roleCode,
                permissionAuthorizationService.resolveDataScope(user),
                permissionAuthorizationService.resolvePermissions(user));
    }

    @Override
    public User currentProfile() {
        return requireCurrentUserFromDatabase();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateCurrentProfile(AdminProfileUpdateRequest request) {
        User currentUser = requireCurrentUserFromDatabase();
        validateProfile(request);

        // 所有文本统一去除首尾空格；空字符串按 null 保存，保持数据库语义一致。
        request.setPhone(request.getPhone().trim());
        request.setEmail(trimToNull(request.getEmail()));
        request.setAvatar(trimToNull(request.getAvatar()));
        request.setRegion(trimToNull(request.getRegion()));
        request.setRemark(trimToNull(request.getRemark()));
        request.setRoleId(resolveProfileRole(currentUser, request.getRoleId()).getId());

        if (userMapper.countByPhone(request.getPhone(), currentUser.getId()) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "手机号已存在");
        }
        if (StringUtils.hasText(request.getEmail())
                && userMapper.countByEmail(request.getEmail(), currentUser.getId()) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "邮箱已存在");
        }
        if (userMapper.updateProfile(currentUser.getId(), request) != 1) {
            throw new BusinessException(ResponseCode.FAIL, "个人资料修改失败");
        }
        return userMapper.selectById(currentUser.getId());
    }

    @Override
    public AdminAvatarUploadResponse uploadCurrentAvatar(MultipartFile file) {
        requireCurrentUserFromDatabase();
        validateAvatar(file);

        String contentType = file.getContentType().trim().toLowerCase(Locale.ROOT);
        String extension = "image/jpeg".equals(contentType) ? "jpg" : contentType.substring("image/".length());
        LocalDate today = LocalDate.now();
        Path uploadDirectory = Paths.get(System.getProperty("user.dir"))
                .resolve("uploads").resolve("profile-avatars").resolve(today.toString());
        String fileName = UUID.randomUUID() + "." + extension;

        try {
            Files.createDirectories(uploadDirectory);
            Path targetPath = uploadDirectory.resolve(fileName).normalize();
            if (!targetPath.startsWith(uploadDirectory.normalize())) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "头像文件名不合法");
            }
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new BusinessException(ResponseCode.FAIL, "头像上传失败");
        }

        String url = "/uploads/profile-avatars/" + today + "/" + fileName;
        return new AdminAvatarUploadResponse(url, file.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeCurrentPassword(AdminPasswordChangeRequest request) {
        User currentUser = requireCurrentUserFromDatabase();
        if (request == null || !StringUtils.hasText(request.getCurrentPassword())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "当前密码不能为空");
        }
        validatePassword(request.getNewPassword());
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "两次输入的新密码不一致");
        }
        if (!matchesEncodedPassword(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "当前密码不正确");
        }
        if (matchesEncodedPassword(request.getNewPassword(), currentUser.getPassword())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "新密码不能与当前密码相同");
        }
        if (userMapper.updatePassword(currentUser.getId(), passwordEncoder.encode(request.getNewPassword().trim())) != 1) {
            throw new BusinessException(ResponseCode.FAIL, "密码修改失败");
        }
        // 密码变化后撤销该用户全部刷新会话，阻止其他设备继续续期旧登录状态。
        loginSessionService.revoke(currentUser.getId());
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "令牌不能为空");
        }
        JwtPayload payload = jwtService.parseToken(token);
        tokenBlacklistService.blacklist(payload.getJti(), payload.getExp());
        loginSessionService.revoke(payload.getAdminId());
    }

    private AdminLoginResponse fail(Long adminId, String username, String ip, String userAgent, String reason) {
        rateLimitService.recordFailure(ip, username);
        metricsService.recordFailure(reason);
        loginLogService.record(adminId, safeUsername(username), false, reason, ip, userAgent);
        throw new BusinessException(ResponseCode.UNAUTHORIZED, LOGIN_FAILED_MESSAGE);
    }

    private String safeUsername(String username) {
        return StringUtils.hasText(username) ? username.trim() : "";
    }

    private boolean matchesPasswordAndUpgradeIfNeeded(User user, String rawPassword) {
        String encodedPassword = user.getPassword();
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(encodedPassword)) {
            return false;
        }

        if (encodedPassword.startsWith("{bcrypt}")) {
            encodedPassword = encodedPassword.substring("{bcrypt}".length());
        }

        if (isPasswordEncoderHash(encodedPassword)) {
            try {
                return passwordEncoder.matches(rawPassword, encodedPassword);
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }

        String legacyPassword = encodedPassword;
        if (legacyPassword.startsWith("{noop}")) {
            legacyPassword = legacyPassword.substring("{noop}".length());
        }

        if (!rawPassword.equals(legacyPassword)) {
            return false;
        }

        userMapper.updatePassword(user.getId(), passwordEncoder.encode(rawPassword));
        return true;
    }

    private boolean isPasswordEncoderHash(String password) {
        return password.startsWith("$2a$")
                || password.startsWith("$2b$")
                || password.startsWith("$2y$");
    }

    private AdminLoginResponse issueTokenPair(User user, String roleCode, boolean rememberMe) {
        String accessToken = jwtService.generateAccessToken(user, rememberMe);
        String refreshToken = jwtService.generateRefreshToken(user, rememberMe);
        JwtPayload accessPayload = jwtService.parseToken(accessToken);
        JwtPayload refreshPayload = jwtService.parseToken(refreshToken);
        loginSessionService.save(
                user.getId(),
                accessPayload.getJti(),
                accessPayload.getExp(),
                refreshPayload.getJti(),
                refreshPayload.getExp()
        );
        return new AdminLoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                LocalDateTime.ofInstant(Instant.ofEpochSecond(accessPayload.getExp()), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(Instant.ofEpochSecond(refreshPayload.getExp()), ZoneId.systemDefault()),
                user,
                roleCode,
                permissionAuthorizationService.resolveDataScope(user),
                permissionAuthorizationService.resolvePermissions(user)
        );
    }

    private void validateActiveBackendUser(User user) {
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "用户不存在或已被禁用");
        }
        if (!BACKEND_LOGIN_ROLES.contains(normalizeRoleCode(user.getRoleCode()))) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "当前账号无后台登录权限");
        }
    }

    private User requireCurrentUserFromDatabase() {
        User contextUser = CurrentUserContext.get();
        if (contextUser == null || contextUser.getId() == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        User currentUser = userMapper.selectById(contextUser.getId());
        validateActiveBackendUser(currentUser);
        return currentUser;
    }

    private void validateProfile(AdminProfileUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.getPhone())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "手机号不能为空");
        }
        if (request.getPhone().trim().length() > 20) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "手机号不能超过20个字符");
        }
        validateLength(request.getEmail(), 100, "邮箱");
        validateLength(request.getAvatar(), 255, "头像地址");
        validateLength(request.getRegion(), 100, "所属地区");
        validateLength(request.getRemark(), 255, "个人备注");
        if (StringUtils.hasText(request.getEmail())
                && !EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "邮箱格式不正确");
        }
        if (request.getGender() != null && request.getGender() != 0
                && request.getGender() != 1 && request.getGender() != 2) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "性别只能为0、1或2");
        }
    }

    private Role resolveProfileRole(User currentUser, Long requestedRoleId) {
        Long resolvedRoleId = requestedRoleId == null ? currentUser.getRoleId() : requestedRoleId;
        if (resolvedRoleId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "当前用户未配置有效角色");
        }
        if (!resolvedRoleId.equals(currentUser.getRoleId())
                && !ROLE_ADMIN.equals(normalizeRoleCode(currentUser.getRoleCode()))) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "只有管理员可以修改角色");
        }
        // 即使角色未发生变化也重新查询校验，避免已停用或已删除的角色被继续写回用户表。
        Role role = roleMapper.selectById(resolvedRoleId);
        if (role == null || role.getStatus() == null || role.getStatus() != 1
                || !BACKEND_LOGIN_ROLES.contains(normalizeRoleCode(role.getRoleCode()))) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择有效的后台登录角色");
        }
        return role;
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择需要上传的头像");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "头像大小不能超过5MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_AVATAR_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "头像仅支持JPG、PNG或WEBP格式");
        }
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "新密码不能为空");
        }
        String value = password.trim();
        if (value.length() < 8 || value.length() > 32
                || !value.matches(".*[A-Za-z].*") || !value.matches(".*\\d.*")) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "密码需为8到32个字符，且同时包含字母和数字");
        }
    }

    private boolean matchesEncodedPassword(String rawPassword, String encodedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(encodedPassword)) {
            return false;
        }
        String normalizedPassword = encodedPassword.startsWith("{bcrypt}")
                ? encodedPassword.substring("{bcrypt}".length()) : encodedPassword;
        if (isPasswordEncoderHash(normalizedPassword)) {
            try {
                return passwordEncoder.matches(rawPassword, normalizedPassword);
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }
        return rawPassword.equals(normalizedPassword.startsWith("{noop}")
                ? normalizedPassword.substring("{noop}".length()) : normalizedPassword);
    }

    private void validateLength(String value, int maxLength, String fieldName) {
        if (value != null && value.trim().length() > maxLength) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, fieldName + "不能超过" + maxLength + "个字符");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeRoleCode(String roleCode) {
        return StringUtils.hasText(roleCode) ? roleCode.trim().toLowerCase(Locale.ROOT) : "";
    }

}
