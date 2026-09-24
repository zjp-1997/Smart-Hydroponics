package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 监控设备实体类。
 *
 * <p>该类与 camera_device 表对应，用于承载地块摄像头和视频流基础配置。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraDevice {

    /** 监控设备主键ID。 */
    private Long id;

    /** 原 iot_device.id 或外部设备编号，用于保留摄像头来源设备标识。 */
    private Long deviceId;

    /** 所属地块ID。 */
    private Long plotId;

    /** 所属地块名称，列表和详情查询时通过 plot 表联查得到。 */
    private String plotName;

    /** 所属地块编号，列表和详情查询时通过 plot 表联查得到。 */
    private String plotCode;

    /** 地块所属用户ID。 */
    private Long userId;

    /** 地块所属用户名。 */
    private String username;

    /** 地块所属用户昵称。 */
    private String nickname;

    /** 监控设备名称。 */
    private String name;

    /** 视频流协议，如 RTSP、GB28181、HTTP。 */
    private String streamProtocol;

    /** 视频流地址。 */
    private String streamUrl;

    /** 截图地址。 */
    private String snapshotUrl;

    /** 分辨率，如 1920x1080。 */
    private String resolution;

    /** 在线状态，0离线，1在线。 */
    private Integer onlineStatus;

    /** 监控方向。 */
    private String direction;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
