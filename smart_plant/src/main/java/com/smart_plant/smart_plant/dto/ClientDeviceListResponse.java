package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * farm 用户端设备列表响应对象。
 *
 * <p>移动端设备管理页按设备类别分组展示，所以后端直接返回分组结构，
 * 前端无需再理解后台设备类型表的实现细节。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientDeviceListResponse {

    /** 补光灯设备列表。 */
    private List<DeviceItem> growLightList = new ArrayList<>();

    /** 水泵设备列表。 */
    private List<DeviceItem> pumpList = new ArrayList<>();

    /** 摄像头设备列表。 */
    private List<DeviceItem> cameraList = new ArrayList<>();

    /** 水质检测仪设备列表。 */
    private List<DeviceItem> waterQualityList = new ArrayList<>();

    /** 环境检测仪设备列表。 */
    private List<DeviceItem> environmentList = new ArrayList<>();

    /**
     * 用户端设备行数据。
     *
     * <p>只暴露设备名称、所属地块和状态等页面必需字段，避免把后台控制字段完整透出。</p>
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceItem {
        /** 设备主键，供后续进入设备详情或控制设备使用。 */
        private Long id;

        /** 设备名称。 */
        private String deviceName;

        /** 所属地块主键，用于移动端精确筛选。 */
        private Long plotId;

        /** 所属地块名称。 */
        private String plotName;

        /** 中文设备状态：在线、离线或故障。 */
        private String status;

        /** 开关状态，供当前 UI 的开关控件展示。 */
        private Boolean enabled;

        /** 数据来源：iot 为普通设备，camera 为独立监控设备。 */
        private String source;
    }
}
