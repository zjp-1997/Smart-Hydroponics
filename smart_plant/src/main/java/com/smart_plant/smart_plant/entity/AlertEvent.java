package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent {

    private Long id;

    private Long userId;

    private String username;

    private String nickname;

    private Long plotId;

    private String plotName;

    private String plotCode;

    private Long deviceId;

    private String deviceName;

    private String deviceCode;

    private Long batchId;

    private String batchNo;

    private Integer alertType;

    private String alertTitle;

    private String alertContent;

    private String metricCode;

    private String metricName;

    private BigDecimal metricValue;

    private String metricUnit;

    private BigDecimal thresholdMin;

    private BigDecimal thresholdMax;

    private Integer alertLevel;

    private String sourceType;

    private Long sourceId;

    private String dedupKey;

    private Integer processStatus;

    private Long handlerId;

    private LocalDateTime handleTime;

    private String handleResult;

    private LocalDateTime triggerTime;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
