package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.AdminCaptchaResponse;
import com.smart_plant.smart_plant.dto.AdminLoginRequest;
import com.smart_plant.smart_plant.dto.AdminLoginResponse;
import com.smart_plant.smart_plant.dto.AdminRefreshTokenRequest;
import com.smart_plant.smart_plant.dto.AdminUserInfoResponse;
import com.smart_plant.smart_plant.dto.AdminProfileUpdateRequest;
import com.smart_plant.smart_plant.dto.AdminPasswordChangeRequest;
import com.smart_plant.smart_plant.dto.AdminAvatarUploadResponse;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.AuthCookieService;
import com.smart_plant.smart_plant.service.AdminAuthService;
import com.smart_plant.smart_plant.service.CaptchaService;
import com.smart_plant.smart_plant.utils.ClientRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin/auth", "/api/auth"})
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    private final CaptchaService captchaService;

    private final AuthCookieService authCookieService;

    @GetMapping("/captcha")
    public R<AdminCaptchaResponse> captcha() {
        return R.success(captchaService.create());
    }

    @PostMapping("/login")
    public R<AdminLoginResponse> login(@RequestBody AdminLoginRequest loginRequest,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        AdminLoginResponse loginResponse = adminAuthService.login(
                loginRequest,
                ClientRequestUtils.getClientIp(request),
                ClientRequestUtils.getUserAgent(request)
        );
        authCookieService.writeAccessCookie(loginResponse.getToken(), request, response);
        return R.success(loginResponse);
    }

    @PostMapping("/refresh")
    public R<AdminLoginResponse> refresh(@RequestBody AdminRefreshTokenRequest refreshTokenRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        String refreshToken = refreshTokenRequest == null ? null : refreshTokenRequest.getRefreshToken();
        if (!StringUtils.hasText(refreshToken)) {
            throw new IllegalArgumentException("刷新令牌不能为空");
        }
        AdminLoginResponse loginResponse = adminAuthService.refresh(refreshToken);
        authCookieService.writeAccessCookie(loginResponse.getToken(), request, response);
        return R.success(loginResponse);
    }

    @GetMapping("/info")
    public R<AdminUserInfoResponse> info() {
        return R.success(adminAuthService.currentUserInfo());
    }

    /** 读取当前后台账号的最新个人资料，供 smart_farm 个人中心回显。 */
    @GetMapping("/profile")
    public R<User> profile() {
        return R.success(adminAuthService.currentProfile());
    }

    /** 更新个人资料白名单字段；用户名、昵称、状态和密码不通过此接口修改。 */
    @PutMapping("/profile")
    public R<User> updateProfile(@RequestBody AdminProfileUpdateRequest request) {
        return R.success(adminAuthService.updateCurrentProfile(request));
    }

    /** 上传个人头像文件，资料保存时再将返回 URL 写入 user.avatar。 */
    @PostMapping("/profile/avatar")
    public R<AdminAvatarUploadResponse> uploadProfileAvatar(@RequestParam("file") MultipartFile file) {
        return R.success(adminAuthService.uploadCurrentAvatar(file));
    }

    /** 修改当前登录用户密码，服务层会校验原密码和新密码强度。 */
    @PutMapping("/password")
    public R<Void> changePassword(@RequestBody AdminPasswordChangeRequest request) {
        adminAuthService.changeCurrentPassword(request);
        return R.success();
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        adminAuthService.logout(ClientRequestUtils.getBearerToken(request));
        authCookieService.clearAccessCookie(request, response);
        return R.success();
    }
}
