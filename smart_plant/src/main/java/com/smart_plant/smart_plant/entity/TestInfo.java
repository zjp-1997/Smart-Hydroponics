package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestInfo {

    private String status;

    private String message;

    private String currentTime;
}
