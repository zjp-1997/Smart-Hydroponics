package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 首页访问量趋势单日数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeVisitTrendPoint {

    private String date;

    private String label;

    private Long visitCount;
}
