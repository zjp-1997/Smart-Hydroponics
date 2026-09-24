package com.smart_plant.smart_plant.dto;

import lombok.Data;

/**
 * 手机验证码登录请求。
 */
@Data
public class ClientSmsLoginRequest {

    /** 已绑定账号的手机号。 */
    private String phone;

    /** 用户输入的六位短信验证码。 */
    private String code;

    /** 是否签发较长有效期的登录令牌。 */
    private Boolean rememberMe;
}
