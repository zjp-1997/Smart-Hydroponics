package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * farm 用户端地块详情响应对象。
 *
 * <p>详情页需要聚合地块、作物批次、环境/水质监测、农事任务和设备状态等多个业务模块。
 * 使用专用 DTO 可以避免移动端直接依赖后台管理实体结构，也能集中控制字段暴露范围。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientPlotDetailResponse {

    /** 地块主键，来源于 plot.id。 */
    private Long id;

    /** 当前没有有效种植批次时为空闲，前端据此显示占位信息。 */
    private Boolean idle;

    /** 地块名称。 */
    private String plotName;

    /** 地块图片，优先使用当前种植批次作物图，其次使用作物基础图。 */
    private String plotImage;

    /** 当前作物名称，空闲时返回“-”。 */
    private String cropName;

    /** 当前作物主键，供农场主编辑时回填作物选择器。 */
    private Long cropId;

    /** 地块面积数值。 */
    private BigDecimal plotAreaValue;

    /** 地块面积单位。 */
    private String plotAreaUnit;

    /** 已格式化的地块面积文本，便于用户端直接渲染。 */
    private String plotArea;

    /** 当前批次种植日期。 */
    private LocalDate plantingTime;

    /** 当前批次预计采收日期，不使用可能存在的实际采收日期回填编辑表单。 */
    private LocalDate expectedHarvestAt;

    /** 当前批次采收日期，优先返回实际采收日期，其次返回预计采收日期。 */
    private LocalDate harvestTime;

    /** 当前作物已经种植天数。 */
    private Integer plantingDays;

    /** 当前作物所在生长期。 */
    private String growthStageName;

    /** 当前生长期排序，用于前端高亮生长进度。 */
    private Integer growthStageOrder;

    /** 当前批次是否已进入成熟期且允许执行采收。 */
    private Boolean harvestable;

    /** 最新环境监测数据，包含空气温度、湿度、风速、气压、CO2、PM2.5 等完整字段。 */
    private EnvironmentMonitor environmentMonitor;

    /** 最新水质监测数据，包含水温、EC、PH、溶解氧等完整字段。 */
    private WaterQualityMonitor waterQualityMonitor;

    /** 详情页顶部环境监测卡片使用的指标列表。 */
    private List<MonitorItem> monitorItems = new ArrayList<>();

    /** 地块关联的真实农事任务，按截至时间从近到远返回。 */
    private List<FarmTaskSummary> farmTasks = new ArrayList<>();

    /** 地块下全部设备的在线、离线或故障状态。 */
    private List<DeviceStatus> devices = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnvironmentMonitor {
        /** 空气温度（℃）。 */
        private BigDecimal airTemperature;
        /** 空气湿度（%）。 */
        private BigDecimal airHumidity;
        /** 风速（m/s）。 */
        private BigDecimal windSpeed;
        /** 气压（hPa）。 */
        private BigDecimal airPressure;
        /** 二氧化碳浓度（ppm）。 */
        private BigDecimal co2Concentration;
        /** PM2.5（μg/m³）。 */
        private BigDecimal pm25;
        /** 数据状态（1正常 2异常）。 */
        private Integer dataStatus;
        /** 异常详情。 */
        private String abnormalDetail;
        /** 采集时间。 */
        private LocalDateTime collectTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaterQualityMonitor {
        /** 水温（℃）。 */
        private BigDecimal waterTemperature;
        /** PH 值。 */
        private BigDecimal ph;
        /** EC 值。 */
        private BigDecimal ecValue;
        /** 溶解氧（mg/L）。 */
        private BigDecimal dissolvedOxygen;
        /** 数据状态（1正常 2异常）。 */
        private Integer dataStatus;
        /** 异常详情。 */
        private String abnormalDetail;
        /** 采集时间。 */
        private LocalDateTime collectTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonitorItem {
        /** 指标名称，例如水温、PH值。 */
        private String label;
        /** 已拼接单位的指标值，例如 18℃。 */
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FarmTaskSummary {
        /** 任务主键，对应 farm_task.id。 */
        private Long id;
        /** 任务标题。 */
        private String taskTitle;
        /** 任务状态编码：1未开始、2进行中、3已完成、4已逾期、5已取消。 */
        private Integer status;
        /** 已格式化的任务状态名称，供移动端直接展示。 */
        private String statusName;
        /** 任务截至时间。 */
        private LocalDateTime deadlineTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceStatus {
        /** 设备主键。 */
        private Long id;
        /** 设备名称。 */
        private String name;
        /** 设备类型编码，用于前端匹配 iconfont 图标。 */
        private String typeCode;
        /** 在线状态（0离线 1在线）。 */
        private Integer onlineStatus;
        /** 健康状态（0正常 1故障 2维护）。 */
        private Integer healthStatus;
        /** 控制状态（0关闭 1开启）。 */
        private Integer controlStatus;
        /** 已归一化的中文状态：在线、离线或故障。 */
        private String state;
        /** 是否开启，供页面开关组件展示。 */
        private Boolean enabled;
    }
}
