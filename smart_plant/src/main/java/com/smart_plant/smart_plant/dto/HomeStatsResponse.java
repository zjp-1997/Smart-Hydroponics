package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 首页核心指标统计。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeStatsResponse {

    private Long farms;
    private Long plots;
    private Long batches;
    private Long devices;
    private Long faults;
}
