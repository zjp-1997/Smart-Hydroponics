package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeOperationOverviewResponse {

    private LocalDateTime generatedAt;

    private List<HomeOperationOverviewItem> items;
}
