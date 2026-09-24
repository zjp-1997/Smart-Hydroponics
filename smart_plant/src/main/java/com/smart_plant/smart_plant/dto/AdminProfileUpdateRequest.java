package com.smart_plant.smart_plant.dto;

import lombok.Data;

/**
 * 后台当前用户可修改的个人资料白名单。
 *
 * <p>刻意不接收用户名、昵称、状态和密码；角色修改还会在服务层执行管理员权限校验。</p>
 */
@Data
public class AdminProfileUpdateRequest {

    /** 联系手机号，数据库要求非空且唯一。 */
    private String phone;

    /** 联系邮箱，可为空。 */
    private String email;

    /** 头像地址，可为空。 */
    private String avatar;

    /** 性别：0未知、1男、2女。 */
    private Integer gender;

    /** 角色ID；仅管理员可在后台登录角色范围内调整。 */
    private Long roleId;

    /** 所属地区，可为空。 */
    private String region;

    /** 个人备注，可为空。 */
    private String remark;
}
