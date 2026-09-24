package com.smart_plant.smart_plant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** farm 用户端云台方向控制请求。 */
@Data
public class CameraPtzRequest {

    /** 控制方向：UP、DOWN、LEFT、RIGHT 或 STOP。 */
    @NotBlank(message = "云台控制方向不能为空")
    private String direction;
}
