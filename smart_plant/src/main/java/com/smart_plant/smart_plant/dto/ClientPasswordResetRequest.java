package com.smart_plant.smart_plant.dto;

import lombok.Data;

/**
 * 当前用户通过已绑定手机号找回密码的请求参数。
 */
@Data
public class ClientPasswordResetRequest {

    /** 当前账号已经绑定的手机号。 */
    private String phone;

    /** 找回密码场景的六位短信验证码。 */
    private String code;

    /** 用户设置的新明文密码，服务端校验后使用 BCrypt 加密入库。 */
    private String newPassword;

    /** 新密码确认值，用于防止用户输入错误。 */
    private String confirmPassword;
}
