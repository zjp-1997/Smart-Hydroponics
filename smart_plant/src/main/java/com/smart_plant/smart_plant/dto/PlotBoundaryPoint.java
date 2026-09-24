package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 地块边界顶点，统一使用系统持久化坐标系 WGS-84。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlotBoundaryPoint {

    /** 顶点经度，合法范围为 -180 到 180。 */
    private BigDecimal longitude;

    /** 顶点纬度，合法范围为 -90 到 90。 */
    private BigDecimal latitude;
}
