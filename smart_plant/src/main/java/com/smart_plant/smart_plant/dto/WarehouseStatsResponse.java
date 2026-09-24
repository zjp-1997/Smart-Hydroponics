package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStatsResponse {

    private long totalItems;

    private BigDecimal stockQuantity;

    private BigDecimal inboundQuantity;

    private BigDecimal outboundQuantity;
}
