package com.smart_plant.smart_plant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** farm 端地块基本信息编辑请求；只允许提交页面展示的五项业务字段。 */
@Data
public class ClientPlotUpdateRequest {
    /** 已有的作物主键，名称从作物表读取，避免客户端直接修改作物字典。 */
    @NotNull(message = "请选择作物名称")
    private Long cropId;

    /** 地块名称。 */
    @NotBlank(message = "请输入地块名称")
    @Size(max = 100, message = "地块名称不能超过100个字符")
    private String plotName;

    /** 种植面积，沿用当前地块的面积单位。 */
    @NotNull(message = "请输入种植面积")
    @DecimalMin(value = "0.01", message = "种植面积必须大于0")
    @Digits(integer = 8, fraction = 2, message = "种植面积最多为八位整数、两位小数")
    private BigDecimal plantingArea;

    /** 种植日期。 */
    @NotNull(message = "请选择种植时间")
    private LocalDate plantedAt;

    /** 预计采收日期。 */
    @NotNull(message = "请选择预计采收时间")
    private LocalDate expectedHarvestAt;
}
