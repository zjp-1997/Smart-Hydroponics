package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.ClientAuthResponse;
import com.smart_plant.smart_plant.dto.ClientLoginRequest;
import com.smart_plant.smart_plant.dto.ClientRegisterRequest;
import com.smart_plant.smart_plant.dto.ClientFarmOwnerOption;
import com.smart_plant.smart_plant.dto.ClientSmsCodeResponse;
import com.smart_plant.smart_plant.dto.ClientSmsLoginRequest;
import com.smart_plant.smart_plant.dto.ClientPasswordResetRequest;
import com.smart_plant.smart_plant.dto.ClientProfileUpdateRequest;
import com.smart_plant.smart_plant.dto.ClientAvatarUploadResponse;
import com.smart_plant.smart_plant.entity.User;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * farm 用户端认证服务接口。
 *
 * <p>接口层集中暴露用户端注册、登录和退出能力，便于统一维护客户端认证生命周期。</p>
 */
public interface ClientAuthService {

    /**
     * 注册普通用户、技术人员或专家账号，并保存相应的绑定资料。
     *
     * @param request 用户端注册请求
     * @return 入库后的用户信息
     */
    User register(ClientRegisterRequest request);

    /** 获取可供普通用户和技术人员注册时选择的农场主。 */
    List<ClientFarmOwnerOption> listFarmOwners();

    /**
     * 使用账号密码登录用户端。
     *
     * @param request 登录请求
     * @param ip      客户端 IP，用于登录限流和安全审计
     * @return token、refreshToken 和用户信息
     */
    ClientAuthResponse login(ClientLoginRequest request, String ip);

    /**
     * 生成登录、绑定、换绑或找回密码场景的模拟短信验证码。
     *
     * @param userId    账号设置场景中的当前用户 ID；登录场景可为空
     * @param phone     手机号
     * @param scene     验证码使用场景
     * @param ip        发码请求 IP
     * @param userAgent 客户端标识
     * @return 模拟验证码及有效期
     */
    ClientSmsCodeResponse sendSmsCode(Long userId, String phone, int scene, String ip, String userAgent);

    /** 使用已绑定手机号和短信验证码登录。 */
    ClientAuthResponse loginBySms(ClientSmsLoginRequest request, String ip);

    /** 校验绑定验证码，并将手机号绑定到当前登录用户。 */
    User bindPhone(Long userId, String phone, String code);

    /** 校验换绑验证码，并把当前账号更新为新的唯一手机号。 */
    User changePhone(Long userId, String phone, String code);

    /** 校验当前账号已绑定手机号及验证码，更新密码后撤销所有旧会话。 */
    void resetPassword(Long userId, ClientPasswordResetRequest request);

    /** 更新当前移动端用户可编辑的个人资料白名单字段。 */
    User updateProfile(Long userId, ClientProfileUpdateRequest request);

    /** 校验并保存当前移动端用户选择的头像文件。 */
    ClientAvatarUploadResponse uploadProfileAvatar(Long userId, MultipartFile file);

    /**
     * 注销当前访问令牌，并撤销该用户仍有效的服务端登录会话。
     *
     * @param token Authorization 请求头中的访问令牌
     */
    void logout(String token);
}
