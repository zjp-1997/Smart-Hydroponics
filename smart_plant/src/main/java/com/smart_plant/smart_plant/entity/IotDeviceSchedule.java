package com.smart_plant.smart_plant.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/** 补光灯或水泵的每周运行时段。 */
@Data
public class IotDeviceSchedule {
    private Long id;
    private Long planId;
    private String scheduleName;
    private Integer weekdaysMask;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer enabled;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
