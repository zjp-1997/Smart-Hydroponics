package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 专家详情实体，对应 expert_detail 表，用于存储专家擅长方向和简介等长文本信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpertDetail {

    /** 专家详情ID */
    private Long id;

    /** 关联专家信息ID */
    private Long expertId;

    /** 擅长方向 */
    private String specialty;

    /** 专家简介 */
    private String introduction;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
