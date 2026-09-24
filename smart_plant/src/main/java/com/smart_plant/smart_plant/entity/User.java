package com.smart_plant.smart_plant.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;

    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String phone;

    private String email;

    private String avatar;

    private String nickname;

    private Integer gender;

    private Long roleId;

    private String roleName;

    private String roleCode;

    /** 用户管理表单中的所属农场主；实际关系保存在普通用户或技术员绑定表。 */
    private Long farmOwnerId;

    private Integer status;

    private String region;

    private Integer loginCount;

    private LocalDateTime lastLoginTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
