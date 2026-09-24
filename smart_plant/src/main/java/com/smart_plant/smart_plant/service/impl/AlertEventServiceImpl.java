package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AlertEvent;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.AlertEventMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.AlertEventService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertEventServiceImpl implements AlertEventService {

    private static final int PROCESS_PENDING = 1;
    private static final int PROCESSING = 2;
    private static final int PROCESSED = 3;
    private static final int IGNORED = 4;

    private final AlertEventMapper alertEventMapper;
    private final DataPermissionService dataPermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertEvent createIfAbsent(AlertEvent alertEvent) {
        validateCreate(alertEvent);
        AlertEvent existing = getBySource(alertEvent.getSourceType(), alertEvent.getSourceId(), alertEvent.getAlertType());
        if (existing != null) {
            return existing;
        }
        if (alertEvent.getProcessStatus() == null) {
            alertEvent.setProcessStatus(PROCESS_PENDING);
        }
        if (alertEvent.getStatus() == null) {
            alertEvent.setStatus(1);
        }
        if (alertEvent.getAlertLevel() == null) {
            alertEvent.setAlertLevel(1);
        }
        alertEvent.setDedupKey(buildDedupKey(
                alertEvent.getSourceType(), alertEvent.getSourceId(), alertEvent.getAlertType()));
        try {
            alertEventMapper.insert(alertEvent);
        } catch (DuplicateKeyException duplicate) {
            AlertEvent concurrent = alertEventMapper.selectByDedupKeyForUpdate(alertEvent.getDedupKey());
            if (concurrent != null) {
                return concurrent;
            }
            throw duplicate;
        }
        return alertEventMapper.selectById(alertEvent.getId());
    }

    @Override
    public AlertEvent getBySource(String sourceType, Long sourceId, Integer alertType) {
        if (sourceType == null || sourceId == null || alertType == null) {
            return null;
        }
        return alertEventMapper.selectBySource(sourceType, sourceId, alertType);
    }

    @Override
    public AlertEvent getAlertEventById(Long id) {
        requireId(id);
        AlertEvent alertEvent = alertEventMapper.selectById(id);
        if (alertEvent == null || Integer.valueOf(0).equals(alertEvent.getStatus())) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "Alert event does not exist");
        }
        dataPermissionService.requireOwnedResource(alertEvent.getUserId());
        return alertEvent;
    }

    @Override
    public PageInfo<AlertEvent> listAlertEvents(String plotName, Integer alertType, Integer alertLevel,
                                                Integer processStatus, String sourceType, LocalDate startDate,
                                                LocalDate endDate, Integer pageNum, Integer pageSize) {
        validateAlertType(alertType);
        validateAlertLevel(alertLevel);
        validateProcessStatus(processStatus);
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Start date cannot be after end date");
        }
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(alertEventMapper.selectList(
                scopedUserId,
                normalizeOptionalText(plotName),
                alertType,
                alertLevel,
                processStatus,
                normalizeOptionalText(sourceType),
                startTime,
                endTime));
    }

    @Override
    public Map<String, Object> statisticsAlertEvents() {
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        Map<String, Object> statistics = alertEventMapper.selectStatistics(scopedUserId);
        return statistics == null ? new HashMap<>() : statistics;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertEvent updateProcessStatus(Long id, Integer processStatus, String handleResult) {
        AlertEvent oldEvent = getAlertEventById(id);
        validateProcessStatus(processStatus);
        User user = dataPermissionService.currentUser();
        int rows = alertEventMapper.updateProcessStatus(
                oldEvent.getId(),
                processStatus,
                user.getId(),
                normalizeOptionalText(handleResult));
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "Failed to update alert process status");
        }
        return alertEventMapper.selectById(oldEvent.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlertEvent(Long id) {
        AlertEvent oldEvent = getAlertEventById(id);
        int rows = alertEventMapper.deleteById(oldEvent.getId());
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "Alert event does not exist");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAlertEvents(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Please select alert events to delete");
        }
        for (Long id : ids) {
            getAlertEventById(id);
        }
        return alertEventMapper.deleteBatchByIds(ids);
    }

    private void validateCreate(AlertEvent alertEvent) {
        if (alertEvent == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Alert event is required");
        }
        if (alertEvent.getUserId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Alert user is required");
        }
        if (alertEvent.getAlertType() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Alert type is required");
        }
        if (!StringUtils.hasText(alertEvent.getAlertTitle())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Alert title is required");
        }
    }

    private void validateAlertType(Integer alertType) {
        if (alertType != null && (alertType < 1 || alertType > 6)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Alert type must be between 1 and 6");
        }
    }

    private void validateAlertLevel(Integer alertLevel) {
        if (alertLevel != null && (alertLevel < 1 || alertLevel > 3)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Alert level must be between 1 and 3");
        }
    }

    private void validateProcessStatus(Integer processStatus) {
        if (processStatus != null
                && processStatus != PROCESS_PENDING
                && processStatus != PROCESSING
                && processStatus != PROCESSED
                && processStatus != IGNORED) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Alert process status must be between 1 and 4");
        }
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String buildDedupKey(String sourceType, Long sourceId, Integer alertType) {
        return StringUtils.hasText(sourceType) && sourceId != null && alertType != null
                ? "alert:" + sourceType.trim() + ":" + sourceId + ":" + alertType
                : null;
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Alert event id is required");
        }
    }
}
