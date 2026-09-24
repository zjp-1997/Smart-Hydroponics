package com.smart_plant.smart_plant.service;

public record RecognitionDecision(
        boolean automatic,
        boolean abnormal,
        boolean createAlert,
        boolean createFarmTask,
        Integer alertLevel,
        Integer taskPriority,
        Integer taskType,
        String reason
) {
}
