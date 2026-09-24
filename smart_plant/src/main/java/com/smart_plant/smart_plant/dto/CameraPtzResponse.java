package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** farm 用户端云台控制结果。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraPtzResponse {

    /** 被控制的摄像头主键。 */
    private Long cameraId;

    /** 已校验并下发的标准方向。 */
    private String direction;

    /** 后端接收指令的时间。 */
    private LocalDateTime commandTime;
}
