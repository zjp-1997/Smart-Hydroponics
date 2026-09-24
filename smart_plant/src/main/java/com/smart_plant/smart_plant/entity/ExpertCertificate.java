package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 专家资质证书实体，对应 expert_certificate 表，支持一个专家维护多条证书记录。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpertCertificate {

    /** 专家证书ID */
    private Long id;

    /** 关联专家信息ID */
    private Long expertId;

    /** 证书名称 */
    private String certificateName;

    /** 资质证书图片地址 */
    private String certificateUrl;

    /** 审核状态：1待审核 2审核通过 3审核拒绝 */
    private Integer auditStatus;

    /** 是否过期：1已过期 0未过期 */
    private Integer isExpired;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
