package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 水泵监测数据实体，对应 pump_data 表。
 *
 * <p>用于存储水泵设备采集的水流量、水压等运行数据，
 * 并根据设备正常运行范围自动判定数据状态（正常/异常）。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PumpData {

    /** 水泵监测ID，由数据库自增生成。 */
    private Long id;

    /** 水泵设备ID，关联 iot_device.id。 */
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

    /** 水流量（m³/h）。 */
    private BigDecimal waterFlow;

    /** 水压（MPa）。 */
    private BigDecimal waterPressure;

    /** 数据状态（1正常 2异常），根据水流量和水压正常运行范围判定。 */
    private Integer dataStatus;

    /** 异常详情，记录哪些指标超出范围，如：水流量偏低、水压偏高。 */
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
