package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备故障实体，对应 iot_device_fault 表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IotDeviceFault {

    private Long id;
    private Long deviceId;
    /** 查询时关联得到的设备所有者 ID，用于移动端权限判断。 */
    private Long deviceOwnerId;
    private String deviceCode;
    private String deviceName;
    private String plotName;
    private String faultCode;
    private String faultName;
    private Integer faultType;
    private Integer severity;
    private String faultDesc;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private Integer status;
    private Integer assignStatus;
    private Long handleUserId;
    private String handleUsername;
    private String handleNickname;
    private LocalDateTime handleTime;
    private String handleResult;
    /** farm 端完成处理时上传的现场图片相对地址。 */
    private String completionImageUrl;
    /** 逻辑删除标记：0正常，1已删除。 */
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
