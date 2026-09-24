package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * farm 用户端专家列表响应对象。
 *
 * <p>用户端专家咨询页面只需要展示专家基础介绍和推荐分，
 * 因此使用 DTO 隔离后台管理字段，避免手机号、邮箱、证书等敏感或管理字段被过度暴露。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientExpertListResponse {

    /** 专家主键，后续进入咨询详情或发起咨询时作为业务标识。 */
    private Long id;

    /** 专家姓名，对应 expert_profile.real_name 字段。 */
    private String name;

    /** 专家头像地址，对应 expert_profile.avatar 字段，farm 端用于刷新展示最新头像。 */
    private String avatar;

    /** 专家所属学校或机构名称，由 institution 表关联查询得到。 */
    private String unit;

    /** 专家职称，例如教授、高级农艺师等。 */
    private String jobTitle;

    /** 专家擅长方向，用于用户端标签展示。 */
    private String specialty;

    /** 专家简介，用于咨询卡片的摘要说明。 */
    private String introduction;

    /** 专家评分，用户端展示为官方推荐分。 */
    private BigDecimal rating;

    /** 当前是否可以发起咨询。 */
    private Boolean consultable;
}
