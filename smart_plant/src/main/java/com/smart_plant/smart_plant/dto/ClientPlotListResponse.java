package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * farm 用户端地块列表响应对象。
 *
 * <p>该 DTO 面向移动端列表卡片，避免直接返回后台管理用的 Plot 全量字段，
 * 只暴露作物、地块、种植批次和预估产量等用户端展示需要的信息。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientPlotListResponse {

    /** 地块主键，供用户端进入地块详情页使用。 */
    private Long id;

    /** 所属农场主键，供用户端从农场列表进入地块列表时保持上下文。 */
    private Long farmId;

    /** 所属农场名称。 */
    private String farmName;

    /** 地块名称，如 3号架、A区01。 */
    private String plotName;

    /** 地块编号，供 farm 用户端按编号搜索。 */
    private String plotCode;

    /** 作物图片地址，优先取种植批次图片，其次取作物基础图片。 */
    private String cropImage;

    /** 当前种植作物名称。 */
    private String cropName;

    /** 当前作物已种植天数。 */
    private Integer plantingDays;

    /** 当前作物生长阶段名称。 */
    private String growthStageName;

    /** 用户端种植状态编码：PLANTING、HARVEST_READY 或 IDLE。 */
    private String plantingStatus;

    /** 用户端种植状态中文名称：种植中、待采收或空闲中。 */
    private String plantingStatusName;

    /** 地块面积数值。 */
    private BigDecimal plotAreaValue;

    /** 地块面积单位。 */
    private String plotAreaUnit;

    /** 已格式化的地块面积文本，便于 uni-app 端直接渲染。 */
    private String plotArea;

    /** 种植时间。 */
    private LocalDate plantingTime;

    /** 预计或实际采收时间。 */
    private LocalDate harvestTime;

    /** 预估产量数值。 */
    private BigDecimal estimatedYieldAmount;

    /** 产量单位。 */
    private String yieldUnit;

    /** 已格式化的预估产量文本，便于 uni-app 端直接渲染。 */
    private String estimatedYield;
}
