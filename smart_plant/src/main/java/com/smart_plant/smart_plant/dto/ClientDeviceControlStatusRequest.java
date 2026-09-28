package com.smart_plant.smart_plant.dto;

import lombok.Data;

/** farm 地块详情页设备启停请求。 */
@Data
public class ClientDeviceControlStatusRequest {

    /** 期望控制状态：0 关闭，1 开启。 */
    private Integer controlStatus;
}
