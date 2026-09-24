package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AdminLoginLog;

import java.time.LocalDateTime;

public interface AdminLoginLogService {

    void record(Long adminId, String username, boolean success, String failureReason, String ip, String userAgent);

    PageInfo<AdminLoginLog> listLoginLogs(String username, String ip, Integer success,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          Integer pageNum, Integer pageSize);
}
