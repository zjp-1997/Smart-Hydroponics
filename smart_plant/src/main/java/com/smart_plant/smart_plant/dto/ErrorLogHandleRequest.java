package com.smart_plant.smart_plant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员处理错误日志时允许修改的字段。 */
@Data
public class ErrorLogHandleRequest {

    /** 0未处理、1已处理、2已忽略。 */
    @NotNull(message = "处理状态不能为空")
    @Min(value = 0, message = "处理状态不正确")
    @Max(value = 2, message = "处理状态不正确")
    private Integer handleStatus;

    @Size(max = 500, message = "处理备注不能超过500个字符")
    private String handleRemark;

    /** 客户端回传版本号，防止两名管理员互相覆盖处理结果。 */
    @NotNull(message = "版本号不能为空")
    @Min(value = 0, message = "版本号不正确")
    private Integer version;
}
