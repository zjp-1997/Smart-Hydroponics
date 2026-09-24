package com.smart_plant.smart_plant.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** 设备计划保存白名单，拒绝客户端修改硬件应用状态和审计字段。 */
@Data
public class IotDevicePlanSaveRequest {
    private Long deviceId;
    private Boolean collectionEnabled;
    private Integer collectionIntervalMinutes;
    private Boolean controlEnabled;
    private String timezone;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer version;
    private List<ScheduleItem> schedules = new ArrayList<>();

    @Data
    public static class ScheduleItem {
        private String scheduleName;
        private Integer weekdaysMask;
        private LocalTime startTime;
        private LocalTime endTime;
        private Boolean enabled;
        private Integer sortOrder;
    }
}
