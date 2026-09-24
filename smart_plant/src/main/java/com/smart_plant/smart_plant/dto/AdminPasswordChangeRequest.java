package com.smart_plant.smart_plant.dto;

import lombok.Data;

/** 当前后台用户修改本人密码的请求对象。 */
@Data
public class AdminPasswordChangeRequest {

    /** 用于确认本人身份的当前密码。 */
    private String currentPassword;

    /** 符合系统密码强度要求的新密码。 */
    private String newPassword;

    /** 新密码确认值，服务端再次比对以防前端校验被绕过。 */
    private String confirmPassword;
}
