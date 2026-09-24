package com.smart_plant.smart_plant.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统错误日志。
 * 异常现场字段只在异常发生时写入，管理员后续只能维护处理状态和备注。
 */
@Data
public class ErrorLog {

    private Long id;
    private String traceId;
    private String level;
    private String exceptionType;
    private String errorMessage;
    private String requestMethod;
    private String requestUri;
    private Long operatorId;
    private String operatorName;
    private String ip;
    private String userAgent;
    private String stackTrace;
    private Integer handleStatus;
    private String handleRemark;
    private Long handlerId;
    private String handlerName;
    private LocalDateTime handleTime;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
