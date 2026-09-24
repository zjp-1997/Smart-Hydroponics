package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * farm 用户端实时监控响应对象。
 *
 * <p>只返回监控页面需要的展示和播放字段，用户归属等后台管理字段不会暴露给移动端。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientMonitorResponse {

    /** 监控设备主键，供详情页和云台控制接口使用。 */
    private Long id;

    /** 摄像头来源设备编号，供后续对接硬件网关。 */
    private Long deviceId;

    /** 所属地块主键。 */
    private Long plotId;

    /** 所属地块名称。 */
    private String plotName;

    /** 地块当前种植作物名称。 */
    private String cropName;

    /** 摄像头显示名称。 */
    private String cameraName;

    /** 视频流协议，如 HTTP、RTSP 或 GB28181。 */
    private String streamProtocol;

    /** 实时视频流地址。 */
    private String streamUrl;

    /** 摄像头实时截图地址。 */
    private String snapshotUrl;

    /** 列表封面地址，优先使用截图，其次使用地块或作物图片。 */
    private String coverUrl;

    /** 视频分辨率。 */
    private String resolution;

    /** 在线状态，0 表示离线，1 表示在线。 */
    private Integer onlineStatus;

    /** 当前监控方向或最近一次云台指令。 */
    private String direction;
}
