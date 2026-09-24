package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * farm 移动端设备故障展示模型。
 *
 * <p>除故障基础信息外，同时返回当前登录用户可执行的操作，避免前端自行推断权限。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientDeviceFaultResponse {

    /** 故障、设备和地块的基础信息。 */
    private Long id;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private String plotName;
    private String faultCode;
    private String faultName;
    private Integer faultType;
    private Integer severity;
    private String faultDesc;

    /** 故障处理状态和处理过程信息。 */
    private Integer status;
    private Integer assignStatus;
    private Long handleUserId;
    private String handleUserName;
    private LocalDateTime startTime;
    private LocalDateTime handleTime;
    private LocalDateTime endTime;
    private Integer duration;
    private String handleResult;
    /** 完成故障处理时保存的现场凭证图片。 */
    private String completionImageUrl;

    /** 当前登录用户是否可以接受或完成该故障。 */
    private Boolean canAccept;
    private Boolean canComplete;
}
