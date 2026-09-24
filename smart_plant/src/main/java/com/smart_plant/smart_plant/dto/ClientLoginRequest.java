package com.smart_plant.smart_plant.dto;

import lombok.Data;

/**
 * 用户端账号密码登录请求。
 */
@Data
public class ClientLoginRequest {

    /** 用户登录账号，对应 user.username。 */
    private String username;

    /** 用户输入的明文密码，仅用于与数据库中的 BCrypt 密文做匹配。 */
    private String password;

    /** 是否延长登录有效期，底层复用现有 JWT rememberMe 配置。 */
    private Boolean rememberMe;
}
