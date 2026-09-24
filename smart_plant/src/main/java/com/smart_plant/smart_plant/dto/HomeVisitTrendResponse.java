package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 首页访问量趋势响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeVisitTrendResponse {

    private Integer days;

    private Long total;

    private List<HomeVisitTrendPoint> points;
}
