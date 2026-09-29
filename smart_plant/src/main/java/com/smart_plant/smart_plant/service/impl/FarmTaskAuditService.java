package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.FarmTask;
import com.smart_plant.smart_plant.entity.FarmTaskRecord;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.FarmTaskRecordMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/** 农事任务服务端审计事件统一写入口。 */
@Service
@RequiredArgsConstructor
public class FarmTaskAuditService {

    public static final int ACTION_CREATE = 5;
    public static final int ACTION_EDIT = 6;
    public static final int ACTION_CANCEL = 7;

    private final FarmTaskRecordMapper farmTaskRecordMapper;

    public void record(FarmTask task, User operator, int actionType, String actionContent,
                       Integer beforeStatus, Integer afterStatus, String sourceClient) {
        FarmTaskRecord record = new FarmTaskRecord();
        record.setTaskId(task.getId());
        if (operator != null) {
            record.setOperatorId(operator.getId());
            record.setOperatorNameSnapshot(firstNonBlank(operator.getNickname(), operator.getUsername()));
        } else {
            record.setOperatorNameSnapshot("系统自动处理");
        }
        record.setActionType(actionType);
        record.setActionContent(limit(actionContent, 500));
        record.setSourceClient(sourceClient);
        record.setBeforeStatus(beforeStatus);
        record.setAfterStatus(afterStatus);
        record.setExecuteTime(LocalDateTime.now());
        if (farmTaskRecordMapper.insert(record) == 0) {
            throw new BusinessException(ResponseCode.FAIL, "Failed to save farm task audit record");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
