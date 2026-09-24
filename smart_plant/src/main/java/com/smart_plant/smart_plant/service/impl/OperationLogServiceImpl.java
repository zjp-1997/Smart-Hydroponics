package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.OperationLog;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.mapper.OperationLogMapper;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.OperationLogService;
import com.smart_plant.smart_plant.utils.ClientRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    private final DataPermissionService dataPermissionService;

    @Override
    public void record(String operation) {
        try {
            User user = CurrentUserContext.get();
            OperationLog operationLog = new OperationLog(
                    null,
                    user == null ? null : user.getId(),
                    user == null ? "" : user.getUsername(),
                    operation,
                    getClientIp(),
                    LocalDateTime.now()
            );
            operationLogMapper.insert(operationLog);
        } catch (Exception exception) {
            log.warn("Failed to record operation log", exception);
        }
    }

    @Override
    public PageInfo<OperationLog> listOperationLogs(String operatorName, String operation,
                                                    LocalDateTime startTime, LocalDateTime endTime,
                                                    Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedOperatorId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(operationLogMapper.selectList(scopedOperatorId, operatorName, operation, startTime, endTime));
    }

    private String getClientIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return "";
        }
        HttpServletRequest request = attributes.getRequest();
        return request == null ? "" : ClientRequestUtils.getClientIp(request);
    }
}
