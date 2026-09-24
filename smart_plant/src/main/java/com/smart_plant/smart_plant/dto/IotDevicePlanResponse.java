package com.smart_plant.smart_plant.dto;

import com.smart_plant.smart_plant.entity.IotDeviceSchedule;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 设备计划管理端及用户端共用的安全响应模型。 */
@Data
public class IotDevicePlanResponse {
    private Long id;
    private Long deviceId;
    private Long plotId;
    private String plotName;
    private String deviceCode;
    private String deviceName;
    private String typeCode;
    private String typeName;
    private Integer onlineStatus;
    private Integer healthStatus;
    private Boolean collectionEnabled;
    private Integer collectionIntervalMinutes;
    private Boolean controlEnabled;
    private String timezone;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer applyStatus;
    private LocalDateTime lastAppliedAt;
    private String lastApplyError;
    private Integer version;
    private LocalDateTime updateTime;
    private List<IotDeviceSchedule> schedules = new ArrayList<>();
}
