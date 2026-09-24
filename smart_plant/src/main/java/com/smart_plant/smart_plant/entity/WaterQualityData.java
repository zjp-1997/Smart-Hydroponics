package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 水质监测数据实体，对应 water_quality_data 表。
 *
 * <p>用于存储水质检测设备采集的水温、PH值、EC值、溶解氧等数据，
 * 并根据关联作物的适宜生长环境自动判定数据状态（正常/异常）。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaterQualityData {

    /** 水质监测ID，由数据库自增生成。 */
    private Long id;

    /** 水质监测设备ID，关联 iot_device.id。 */
    private Long deviceId;

    /** 设备或协议提供的消息唯一标识；为空时不参与去重。 */
    private String messageKey;

    /** 采集时设备所属用户快照。 */
    private Long ownerUserId;

    /** 设备编码，由 iot_device 表联查得到，便于页面展示和筛选。 */
    private String deviceCode;

    /** 设备名称，由 iot_device 表联查得到。 */
    private String deviceName;

    /** 设备类型ID，由 iot_device 表联查得到。 */
    private Long typeId;

    /** 设备类型编码，由 device_type 表联查得到。 */
    private String typeCode;

    /** 设备类型名称，由 device_type 表联查得到。 */
    private String typeName;

    /** 采集时地块ID；历史数据缺少快照时回退到设备当前地块。 */
    private Long plotId;

    /** 地块名称，由 plot 表联查得到。 */
    private String plotName;

    /** 地块编号，由 plot 表联查得到。 */
    private String plotCode;

    /** 水温（℃）。 */
    private BigDecimal waterTemperature;

    /** PH值。 */
    private BigDecimal ph;

    /** EC值（mS/cm）。 */
    private BigDecimal ecValue;

    /** 溶解氧（mg/L）。 */
    private BigDecimal dissolvedOxygen;

    /** 数据状态（1正常 2异常），根据作物适宜范围自动判定。 */
    private Integer dataStatus;

    /** 异常详情，记录哪些指标超出范围，如：水温偏高、EC值偏低。 */
    private String abnormalDetail;

    /** 累计运行时长（小时）。 */
    private BigDecimal cumulativeRuntime;

    /** 数据采集时间。 */
    private LocalDateTime collectTime;

    /** 备注。 */
    private String remark;

    /** 创建时间，由数据库默认写入。 */
    private LocalDateTime createTime;
}
