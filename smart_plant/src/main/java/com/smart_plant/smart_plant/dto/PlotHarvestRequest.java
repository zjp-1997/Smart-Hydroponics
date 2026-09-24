package com.smart_plant.smart_plant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PlotHarvestRequest {

    @NotBlank(message = "采收人不能为空")
    @Size(max = 50, message = "采收人长度不能超过50个字符")
    private String harvester;

    @NotNull(message = "实际产量不能为空")
    @PositiveOrZero(message = "实际产量不能小于0")
    private BigDecimal yieldAmount;

    @Size(max = 20, message = "产量单位长度不能超过20个字符")
    private String yieldUnit;

    @NotNull(message = "实际采收日期不能为空")
    private LocalDate actualHarvestAt;
}
