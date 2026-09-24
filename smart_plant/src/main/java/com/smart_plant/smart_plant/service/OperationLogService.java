package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.OperationLog;

import java.time.LocalDateTime;

public interface OperationLogService {

    void record(String operation);

    PageInfo<OperationLog> listOperationLogs(String operatorName, String operation,
                                             LocalDateTime startTime, LocalDateTime endTime,
                                             Integer pageNum, Integer pageSize);
}
