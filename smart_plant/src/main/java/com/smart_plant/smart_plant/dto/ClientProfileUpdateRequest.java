package com.smart_plant.smart_plant.dto;

import lombok.Data;

/**
 * farm 移动端用户个人信息修改请求。
 *
 * <p>仅声明页面允许编辑的字段，避免客户端借个人资料接口修改角色、状态或密码。</p>
 */
@Data
public class ClientProfileUpdateRequest {

    /** 登录用户名，修改后下次登录使用新用户名。 */
    private String username;

    /** 联系手机号；未填写时保留账号原有手机号或内部占位值。 */
    private String phone;

    /** 页面展示昵称，可为空。 */
    private String nickname;

    /** 性别：0保密、1男、2女。 */
    private Integer gender;

    /** 联系邮箱，可为空。 */
    private String email;

    /** 头像文件访问地址，可为空。 */
    private String avatar;

    /** 个性签名，对应 user.remark 字段。 */
    private String remark;
}
