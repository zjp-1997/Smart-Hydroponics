package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备信息实体类。
 *
 * <p>该类与数据库 iot_device 表一一对应，用于承载设备信息管理接口的入参和返回结果。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotDevice {

    /** 设备主键ID，由数据库自增生成。 */
    private Long id;

    /** 关联地块ID，对应 plot.id，用于标识设备安装在哪个地块。 */
    private Long plotId;

    /** 关联地块名称，列表和详情查询时通过 plot 表联查得到。 */
    private String plotName;

    /** 关联地块编号，列表和详情查询时通过 plot 表联查得到。 */
    private String plotCode;

    /** 关联地块状态，便于前端判断设备所属地块是否可用。 */
    private Integer plotStatus;

    /** 所属用户ID，从关联地块归属用户同步而来。 */
    private Long userId;

    /** 所属用户登录名，列表和详情查询时通过 user 表联查得到。 */
    private String username;

    /** 所属用户昵称，列表和详情查询时通过 user 表联查得到。 */
    private String nickname;

    /** 设备唯一编码，用于对接物联网平台或网关侧设备标识。 */
    private String deviceCode;

    /** 设备名称，用于页面列表和详情展示。 */
    private String name;

    /** 设备类型ID，对应 device_type.id，便于后台动态维护设备类型。 */
    private Long typeId;

    /** 设备类型名称，由 device_type 表联查得到，方便前端直接展示。 */
    private String typeName;

    /** 设备类型编码，由 device_type 表联查得到，便于前端或业务侧做稳定判断。 */
    private String typeCode;

    /** 设备类型分类，1传感器，2执行器，由 device_type 表联查得到。 */
    private Integer typeCategory;

    /** 业务控制状态，0关闭，1开启。 */
    private Integer controlStatus;

    /** 连接层在线状态，0离线，1在线。 */
    private Integer onlineStatus;

    /** 设备健康状态，0正常，1故障，2维护。 */
    private Integer healthStatus;

    /** 设备安装时间，用于计算设备从安装到离线、故障或当前时间的在线时长。 */
    private LocalDateTime installTime;

    /** 最近一次离线时间，用于离线设备在线时长结算。 */
    private LocalDateTime offlineTime;

    /** 最近一次故障或维护时间，用于异常设备在线时长结算。 */
    private LocalDateTime faultTime;

    /** 最后心跳时间，用于判断设备最近一次上报或保活时间。 */
    private LocalDateTime lastHeartbeatTime;

    /** 最后在线时间，用于记录设备最近一次确认在线的时间。 */
    private LocalDateTime lastOnlineTime;

    /** 累计在线时长，单位秒；列表查询时会叠加当前在线会话时长。 */
    private Long onlineDuration;

    /** 最后数据上报时间，用于记录设备最近一次采集数据入库时间。 */
    private LocalDateTime lastDataTime;

    /** 设备安装位置或所属地块位置说明。 */
    private String location;

    /** 记录创建时间，由数据库默认写入。 */
    private LocalDateTime createTime;

    /** 记录更新时间，由数据库自动维护。 */
    private LocalDateTime updateTime;
}
