package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {

    private Long id;

    private Long operatorId;

    private String operatorName;

    private String operation;

    private String ip;

    private LocalDateTime operateTime;
}
