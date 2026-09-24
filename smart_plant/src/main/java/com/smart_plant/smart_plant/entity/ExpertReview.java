package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 专家服务评价实体，对应 expert_review 表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpertReview {

    /** 专家评价ID */
    private Long id;

    /** 关联专家信息ID */
    private Long expertId;

    /** 评价用户ID */
    private Long userId;

    /** 专家真实姓名，列表展示字段 */
    private String expertName;

    /** 评价用户名称，列表展示字段 */
    private String userName;

    /** 本次评价评分 */
    private BigDecimal rating;

    /** 评价内容 */
    private String content;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
