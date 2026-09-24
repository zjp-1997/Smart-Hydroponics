package com.smart_plant.smart_plant.dto;

import com.smart_plant.smart_plant.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户端登录成功响应。
 *
 * <p>返回结构和后台登录响应相似，但命名为 user，便于 farm 客户端区分用户端和管理端上下文。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientAuthResponse {

    /** 访问令牌，farm 前端会保存后通过 Authorization: Bearer xxx 访问受保护接口。 */
    private String token;

    /** 刷新令牌，预留给后续无感续期能力。 */
    private String refreshToken;

    /** 令牌类型，当前固定为 Bearer。 */
    private String tokenType;

    /** 访问令牌过期时间，便于客户端做过期提示或主动刷新。 */
    private LocalDateTime expireTime;

    /** 刷新令牌过期时间。 */
    private LocalDateTime refreshExpireTime;

    /** 登录用户信息；User.password 通过 Jackson WRITE_ONLY 避免返回给客户端。 */
    private User user;

    /** 当前用户角色编码，farm 依据此字段进入对应角色工作台。 */
    private String roleCode;
}
