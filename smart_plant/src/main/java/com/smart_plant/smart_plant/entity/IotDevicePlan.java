package com.smart_plant.smart_plant.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 设备当前生效的采集与自动控制计划。 */
@Data
public class IotDevicePlan {
    private Long id;
    private Long deviceId;
    private Long userId;
    private Long plotId;
    private String plotName;
    private String deviceCode;
    private String deviceName;
    private String typeCode;
    private String typeName;
    private Integer onlineStatus;
    private Integer healthStatus;
    private Integer collectionEnabled;
    private Integer collectionIntervalMinutes;
    private Integer controlEnabled;
    private String timezone;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer applyStatus;
    private LocalDateTime lastAppliedAt;
    private String lastApplyError;
    private Integer version;
    private Long createBy;
    private Long updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
