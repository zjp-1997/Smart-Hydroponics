package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientAuthResponse;
import com.smart_plant.smart_plant.dto.ClientLoginRequest;
import com.smart_plant.smart_plant.dto.ClientPasswordResetRequest;
import com.smart_plant.smart_plant.dto.ClientRegisterRequest;
import com.smart_plant.smart_plant.dto.ClientFarmOwnerOption;
import com.smart_plant.smart_plant.dto.ClientPhoneCodeRequest;
import com.smart_plant.smart_plant.dto.ClientSmsCodeRequest;
import com.smart_plant.smart_plant.dto.ClientSmsCodeResponse;
import com.smart_plant.smart_plant.dto.ClientSmsLoginRequest;
import com.smart_plant.smart_plant.dto.ClientProfileUpdateRequest;
import com.smart_plant.smart_plant.dto.ClientAvatarUploadResponse;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.AuthCookieService;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import com.smart_plant.smart_plant.service.ClientAuthService;
import com.smart_plant.smart_plant.utils.ClientRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

import static com.smart_plant.smart_plant.service.impl.ClientAuthServiceImpl.SMS_SCENE_BIND_PHONE;
import static com.smart_plant.smart_plant.service.impl.ClientAuthServiceImpl.SMS_SCENE_CHANGE_PHONE;
import static com.smart_plant.smart_plant.service.impl.ClientAuthServiceImpl.SMS_SCENE_LOGIN;
import static com.smart_plant.smart_plant.service.impl.ClientAuthServiceImpl.SMS_SCENE_RESET_PASSWORD;

/**
 * farm 用户端认证入口。
 *
 * <p>该 Controller 不加 RequirePermission，因为注册和登录必须匿名可访问；
 * 匿名白名单在 WebMvcConfig 中统一配置。</p>
 */
@RestController
@RequestMapping({"/client/auth", "/api/client/auth"})
@RequiredArgsConstructor
public class ClientAuthController {

    private final ClientAuthService clientAuthService;

    private final AuthCookieService authCookieService;

    /** 应用重开时从已验证令牌返回服务端当前角色，避免使用过期的本地角色缓存。 */
    @GetMapping("/me")
    public R<User> me() {
        User user = CurrentUserContext.get();
        return R.success(user);
    }

    /** 修改当前移动端用户个人资料，用户 ID 始终来自登录上下文。 */
    @PutMapping("/profile")
    public R<User> updateProfile(@RequestBody ClientProfileUpdateRequest request) {
        return R.success(clientAuthService.updateProfile(currentUserId(), request));
    }

    /** 上传头像文件并返回可保存到 user.avatar 的相对访问地址。 */
    @PostMapping("/profile/avatar")
    public R<ClientAvatarUploadResponse> uploadProfileAvatar(@RequestParam("file") MultipartFile file) {
        return R.success(clientAuthService.uploadProfileAvatar(currentUserId(), file));
    }

    @PostMapping("/register")
    public R<User> register(@RequestBody ClientRegisterRequest request) {
        // Controller 只负责协议适配，注册规则、密码加密和入库事务都交给 Service。
        return R.success(clientAuthService.register(request));
    }

    /** 匿名注册页读取启用中的农场主供绑定选择。 */
    @GetMapping("/register/farm-owners")
    public R<List<ClientFarmOwnerOption>> listFarmOwners() {
        return R.success(clientAuthService.listFarmOwners());
    }

    @PostMapping("/login")
    public R<ClientAuthResponse> login(@RequestBody ClientLoginRequest request,
                                       HttpServletRequest servletRequest,
                                       HttpServletResponse servletResponse) {
        // 传入客户端 IP，便于服务层复用登录限流，降低暴力破解风险。
        ClientAuthResponse loginResponse = clientAuthService.login(request, ClientRequestUtils.getClientIp(servletRequest));
        authCookieService.writeAccessCookie(loginResponse.getToken(), servletRequest, servletResponse);
        return R.success(loginResponse);
    }

    @PostMapping("/sms-code/login")
    public R<ClientSmsCodeResponse> sendLoginSmsCode(@RequestBody ClientSmsCodeRequest request,
                                                     HttpServletRequest servletRequest) {
        // 登录发码必须匿名可访问；服务层负责手机号格式、冷却时间和验证码有效期。
        return R.success(clientAuthService.sendSmsCode(
                null,
                request == null ? null : request.getPhone(),
                SMS_SCENE_LOGIN,
                ClientRequestUtils.getClientIp(servletRequest),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/sms-login")
    public R<ClientAuthResponse> smsLogin(@RequestBody ClientSmsLoginRequest request,
                                          HttpServletRequest servletRequest,
                                          HttpServletResponse servletResponse) {
        // 验证码登录成功后沿用账号密码登录的 Token 和 HttpOnly Cookie 签发逻辑。
        ClientAuthResponse loginResponse = clientAuthService.loginBySms(
                request,
                ClientRequestUtils.getClientIp(servletRequest)
        );
        authCookieService.writeAccessCookie(loginResponse.getToken(), servletRequest, servletResponse);
        return R.success(loginResponse);
    }

    @PostMapping("/phone/code")
    public R<ClientSmsCodeResponse> sendBindPhoneSmsCode(@RequestBody ClientSmsCodeRequest request,
                                                         HttpServletRequest servletRequest) {
        // 此路径不在匿名白名单中，只有已登录用户才能为绑定场景获取验证码。
        return R.success(clientAuthService.sendSmsCode(
                currentUserId(),
                request == null ? null : request.getPhone(),
                SMS_SCENE_BIND_PHONE,
                ClientRequestUtils.getClientIp(servletRequest),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/phone/bind")
    public R<User> bindPhone(@RequestBody ClientPhoneCodeRequest request) {
        // 用户身份只取 JWT 解析后的上下文，绝不接受客户端提交 userId，防止越权绑定。
        return R.success(clientAuthService.bindPhone(
                currentUserId(),
                request == null ? null : request.getPhone(),
                request == null ? null : request.getCode()
        ));
    }

    @PostMapping("/phone/change-code")
    public R<ClientSmsCodeResponse> sendChangePhoneSmsCode(@RequestBody ClientSmsCodeRequest request,
                                                           HttpServletRequest servletRequest) {
        // 修改手机号使用独立改手机专用场景，不能复用首次绑定验证码。
        return R.success(clientAuthService.sendSmsCode(
                currentUserId(),
                request == null ? null : request.getPhone(),
                SMS_SCENE_CHANGE_PHONE,
                ClientRequestUtils.getClientIp(servletRequest),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/phone/change")
    public R<User> changePhone(@RequestBody ClientPhoneCodeRequest request) {
        // 换绑结果返回最新用户信息，farm 端可立即刷新本地手机号显示。
        return R.success(clientAuthService.changePhone(
                currentUserId(),
                request == null ? null : request.getPhone(),
                request == null ? null : request.getCode()
        ));
    }

    @PostMapping("/password/code")
    public R<ClientSmsCodeResponse> sendPasswordResetCode(@RequestBody ClientSmsCodeRequest request,
                                                          HttpServletRequest servletRequest) {
        // 找回密码验证码只能发送到当前账号已经绑定的手机号。
        return R.success(clientAuthService.sendSmsCode(
                currentUserId(),
                request == null ? null : request.getPhone(),
                SMS_SCENE_RESET_PASSWORD,
                ClientRequestUtils.getClientIp(servletRequest),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/password/reset")
    public R<Void> resetPassword(@RequestBody ClientPasswordResetRequest request) {
        // 服务层校验手机号、验证码和两次新密码，并在成功后撤销旧会话。
        clientAuthService.resetPassword(currentUserId(), request);
        return R.success();
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        // Bearer token 由统一工具解析，业务层负责令牌失效和服务端会话撤销。
        clientAuthService.logout(ClientRequestUtils.getBearerToken(request));
        // 同时清除浏览器图片访问所使用的 HttpOnly 辅助 Cookie。
        authCookieService.clearAccessCookie(request, response);
        return R.success();
    }

    /** 从 JWT 认证上下文取得当前用户主键，避免所有账号设置接口信任客户端 userId。 */
    private Long currentUserId() {
        User currentUser = CurrentUserContext.get();
        return currentUser == null ? null : currentUser.getId();
    }
}
