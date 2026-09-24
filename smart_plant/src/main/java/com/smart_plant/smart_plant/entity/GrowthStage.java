package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作物生长期实体，对应 growth_stage 表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrowthStage {

    /** 阶段ID */
    private Long id;

    /** 作物ID */
    private Long cropId;

    /** 作物名称，列表展示字段 */
    private String cropName;

    /** 阶段名称 */
    private String stageName;

    /** 阶段编码 */
    private String stageCode;

    /** 阶段顺序 */
    private Integer stageOrder;

    /** 开始天数 */
    private Integer startDay;

    /** 结束天数 */
    private Integer endDay;

    /** 持续天数 */
    private Integer duration;

    /** 建议光照时长（小时/天） */
    private BigDecimal lightHours;

    /** 最低适宜温度 */
    private BigDecimal tempMin;

    /** 最高适宜温度 */
    private BigDecimal tempMax;

    /** 最低适宜湿度 */
    private BigDecimal humidityMin;

    /** 最高适宜湿度 */
    private BigDecimal humidityMax;

    /** 最低适宜PH */
    private BigDecimal phMin;

    /** 最高适宜PH */
    private BigDecimal phMax;

    /** 最低适宜EC值 */
    private BigDecimal ecMin;

    /** 最高适宜EC值 */
    private BigDecimal ecMax;

    /** 建议浇水间隔天数 */
    private Integer waterIntervalDays;

    /** 建议施肥间隔天数 */
    private Integer fertilizerIntervalDays;

    /** 管理建议 */
    private String managementAdvice;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
