package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlotStatisticsResponse {

    private Long plotTotalCount;

    private Long idlePlotCount;

    private Long cropTypeCount;

    private BigDecimal plantingArea;

    private String areaUnit;

    public static PlotStatisticsResponse empty() {
        return new PlotStatisticsResponse(0L, 0L, 0L, BigDecimal.ZERO, "亩");
    }
}
