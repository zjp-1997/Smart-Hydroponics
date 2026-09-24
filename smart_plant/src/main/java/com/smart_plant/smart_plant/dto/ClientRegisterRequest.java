package com.smart_plant.smart_plant.dto;

import lombok.Data;

/**
 * 用户端注册请求。
 *
 * <p>该 DTO 面向 farm 移动端/小程序端页面，不复用后台用户管理的 User 实体，
 * 是为了避免前端传入 roleId、status 等管理字段造成越权创建用户。</p>
 */
@Data
public class ClientRegisterRequest {

    /** 用户登录账号，对应 user.username。 */
    private String username;

    /** 明文密码只在请求传输和服务端校验阶段短暂存在，入库前必须 BCrypt 加密。 */
    private String password;

    /** 前端注册页的确认密码字段，服务端再次校验以防绕过前端校验。 */
    private String confirmPassword;

    /** 手机号可选；当前 farm 注册 UI 没有手机号输入框，服务端会做兼容处理。 */
    private String phone;

    /** 用户昵称可选；未传时默认使用 username。 */
    private String nickname;

    /** 自助注册仅允许 user、technician、expert，服务端会校验角色白名单。 */
    private String roleCode;

    /** 普通用户和技术人员必须选择启用中的农场主；专家不需要此字段。 */
    private Long farmOwnerId;
}
