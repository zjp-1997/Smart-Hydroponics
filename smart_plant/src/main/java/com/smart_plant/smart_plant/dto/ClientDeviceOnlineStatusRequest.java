package com.smart_plant.smart_plant.dto;

import lombok.Data;

/** farm 端设备上线、离线操作请求。 */
@Data
public class ClientDeviceOnlineStatusRequest {

    /** 在线状态：0 离线，1 在线。 */
    private Integer onlineStatus;
}
