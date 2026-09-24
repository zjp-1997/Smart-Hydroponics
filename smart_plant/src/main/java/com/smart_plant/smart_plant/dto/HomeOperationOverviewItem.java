package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeOperationOverviewItem {

    private String metricKey;

    private String label;

    private Long totalCount;

    private Long onlineCount;

    private Long activeCount;

    private BigDecimal onlineRate;
}
