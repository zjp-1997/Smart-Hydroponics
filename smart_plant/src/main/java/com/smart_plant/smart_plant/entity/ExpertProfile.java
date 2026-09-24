package com.smart_plant.smart_plant.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 专家信息聚合实体，主表字段对应 expert_profile，同时承载详情表和证书表的常用展示字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpertProfile {

    /** 专家信息ID */
    private Long id;

    /** 关联用户ID */
    private Long userId;

    /** 所属学校或机构ID */
    private Long institutionId;

    /** 所属机构名称，由 institution 表关联查询得到，不在 expert_profile 表冗余存储 */
    private String organization;

    /** 专家登录用户名 */
    private String username;

    /** 专家登录密码密文，仅允许写入，不向前端返回 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwordHash;

    /** 专家真实姓名 */
    private String realName;

    /** 职称 */
    private String jobTitle;

    /** 专家手机号 */
    private String phone;

    /** 专家邮箱 */
    private String email;

    /** 擅长方向，实际存储在 expert_detail 表 */
    private String specialty;

    /** 专家简介，实际存储在 expert_detail 表 */
    private String introduction;

    /** 资质证书图片地址，实际存储在 expert_certificate 表 */
    private String certificateUrl;

    /** 证书名称，实际存储在 expert_certificate 表 */
    private String certificateName;

    /** 证书审核状态：1待审核 2审核通过 3审核拒绝 */
    private Integer certificateAuditStatus;

    /** 专家头像 */
    private String avatar;

    /** 评分 */
    private BigDecimal rating;

    /** 评价数量 */
    private Integer reviewCount;

    /** 咨询次数 */
    private Integer consultationCount;

    /** 审核状态：0未提交 1待审核 2审核通过 3审核拒绝 */
    private Integer auditStatus;

    /** 审核通过时间 */
    private LocalDateTime auditPassTime;

    /** 服务状态：1可咨询 0暂停咨询 */
    private Integer serviceStatus;

    /** 咨询状态：1可咨询 0暂停咨询 */
    private Integer consultationStatus;

    /** 用户端是否可发起咨询，由专家账号和咨询状态计算 */
    private Boolean consultable;

    /** 账号状态：1正常 0禁用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
