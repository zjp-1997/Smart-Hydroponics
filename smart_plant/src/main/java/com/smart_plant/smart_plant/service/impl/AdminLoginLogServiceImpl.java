package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AdminLoginLog;
import com.smart_plant.smart_plant.mapper.AdminLoginLogMapper;
import com.smart_plant.smart_plant.service.AdminLoginLogService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLoginLogServiceImpl implements AdminLoginLogService {

    private final AdminLoginLogMapper loginLogMapper;

    private final DataPermissionService dataPermissionService;

    @Async
    @Override
    public void record(Long adminId, String username, boolean success, String failureReason, String ip, String userAgent) {
        try {
            AdminLoginLog loginLog = new AdminLoginLog(
                    null,
                    adminId,
                    username,
                    success ? 1 : 0,
                    failureReason,
                    ip,
                    userAgent,
                    LocalDateTime.now()
            );
            loginLogMapper.insert(loginLog);
        } catch (Exception exception) {
            log.warn("Failed to record admin login log", exception);
        }
    }

    @Override
    public PageInfo<AdminLoginLog> listLoginLogs(String username, String ip, Integer success,
                                                 LocalDateTime startTime, LocalDateTime endTime,
                                                 Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        if (!dataPermissionService.isAdmin()) {
            username = dataPermissionService.currentUser().getUsername();
        }
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(loginLogMapper.selectList(username, ip, success, startTime, endTime));
    }
}
