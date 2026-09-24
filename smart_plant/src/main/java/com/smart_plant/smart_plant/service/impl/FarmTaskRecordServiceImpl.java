package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.FarmTask;
import com.smart_plant.smart_plant.entity.FarmTaskRecord;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.FarmTaskMapper;
import com.smart_plant.smart_plant.mapper.FarmTaskRecordMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.FarmTaskRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FarmTaskRecordServiceImpl implements FarmTaskRecordService {

    private final FarmTaskRecordMapper farmTaskRecordMapper;
    private final FarmTaskMapper farmTaskMapper;
    private final DataPermissionService dataPermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmTaskRecord addFarmTaskRecord(FarmTaskRecord record) {
        validateCreate(record);
        record.setRequestId(normalizeOptionalText(record.getRequestId()));
        FarmTask task = farmTaskMapper.selectById(record.getTaskId());
        if (task == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "Farm task does not exist");
        }
        dataPermissionService.requireAgriculturalOperator(task.getUserId());
        User currentUser = dataPermissionService.currentUser();
        if (record.getRequestId() != null) {
            FarmTaskRecord existing = farmTaskRecordMapper.selectByRequestId(record.getRequestId());
            if (existing != null) {
                if (!record.getTaskId().equals(existing.getTaskId())
                        || !currentUser.getId().equals(existing.getOperatorId())) {
                    throw new BusinessException(ResponseCode.PARAM_ERROR, "Request id is already used by another task");
                }
                return existing;
            }
        }
        LocalDateTime executeTime = record.getExecuteTime() == null ? LocalDateTime.now() : record.getExecuteTime();
        record.setOperatorId(currentUser.getId());
        // 姓名快照保证用户改名或删除后，历史过程仍能正确显示当时的操作人。
        record.setOperatorNameSnapshot(firstNonBlank(currentUser.getNickname(), currentUser.getUsername()));
        record.setBeforeStatus(task.getStatus());
        record.setAfterStatus(resolveAfterStatus(record.getActionType(), task.getStatus()));
        record.setExecuteTime(executeTime);
        record.setActionContent(normalizeOptionalText(record.getActionContent()));
        record.setFeedbackDetail(normalizeOptionalText(record.getFeedbackDetail()));
        record.setOptimizeSuggestion(normalizeOptionalText(record.getOptimizeSuggestion()));
        record.setAttachments(normalizeOptionalText(record.getAttachments()));
        // 管理端通用记录接口的来源由服务端固定，禁止调用方伪造事件来源。
        record.setSourceClient("SMART_FARM");

        // 开始和完成动作必须使用带前置状态条件的 SQL，防止并发请求重复流转。
        int transitionRows = 1;
        if (Integer.valueOf(1).equals(record.getActionType())) {
            transitionRows = farmTaskMapper.startTask(task.getId(), currentUser.getId(), executeTime);
        } else if (Integer.valueOf(2).equals(record.getActionType())) {
            transitionRows = farmTaskMapper.completeTask(task.getId(), executeTime,
                    firstNonBlank(record.getFeedbackDetail(), record.getActionContent(), "任务已完成"));
        }
        if (transitionRows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "Task status changed, please refresh and retry");
        }
        if (farmTaskRecordMapper.insert(record) == 0) {
            throw new BusinessException(ResponseCode.FAIL, "Failed to save farm task record");
        }
        return farmTaskRecordMapper.selectById(record.getId());
    }

    @Override
    public FarmTaskRecord getFarmTaskRecordById(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Farm task record id is required");
        }
        FarmTaskRecord record = farmTaskRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "Farm task record does not exist");
        }
        dataPermissionService.requireOwnedResource(record.getUserId());
        return record;
    }

    @Override
    public PageInfo<FarmTaskRecord> listFarmTaskRecords(Long taskId, Integer pageNum, Integer pageSize) {
        if (taskId != null) {
            FarmTask task = farmTaskMapper.selectById(taskId);
            if (task == null) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "Farm task does not exist");
            }
            dataPermissionService.requireOwnedResource(task.getUserId());
        }
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(farmTaskRecordMapper.selectList(taskId, scopedUserId));
    }

    private void validateCreate(FarmTaskRecord record) {
        if (record == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Farm task record is required");
        }
        if (record.getTaskId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Farm task id is required");
        }
        if (record.getActionType() == null || record.getActionType() < 1 || record.getActionType() > 4) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Action type must be between 1 and 4");
        }
        if (record.getFeedbackScore() != null && (record.getFeedbackScore() < 1 || record.getFeedbackScore() > 5)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Feedback score must be between 1 and 5");
        }
        if (record.getProgressPercent() != null
                && (record.getProgressPercent() < 0 || record.getProgressPercent() > 100)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Progress percent must be between 0 and 100");
        }
        if (StringUtils.hasText(record.getRequestId()) && record.getRequestId().trim().length() > 64) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Request id cannot exceed 64 characters");
        }
    }

    private Integer resolveAfterStatus(Integer actionType, Integer beforeStatus) {
        return switch (actionType) {
            case 1 -> 2;
            case 2 -> 3;
            default -> beforeStatus == null ? 1 : beforeStatus;
        };
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 返回第一个非空文本，为操作人和完成说明提供稳定的展示兜底。 */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
