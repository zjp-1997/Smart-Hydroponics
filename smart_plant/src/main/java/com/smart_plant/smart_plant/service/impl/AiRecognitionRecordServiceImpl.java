package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AiRecognitionRecord;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.AiSolutionMapper;
import com.smart_plant.smart_plant.mapper.AiRecognitionRecordMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.AiRecognitionRecordService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
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
public class AiRecognitionRecordServiceImpl implements AiRecognitionRecordService {

    private static final int SOURCE_TYPE_MANUAL = 1;
    private static final int SOURCE_TYPE_CAMERA = 2;
    private static final int RECORD_STATUS_PENDING = 1;
    private static final int RECORD_STATUS_RUNNING = 2;
    private static final int RECORD_STATUS_COMPLETED = 3;
    private static final int RECORD_STATUS_FAILED = 4;
    private static final int RESULT_STATUS_SUCCESS = 1;
    private static final int RESULT_STATUS_FAILED = 2;
    private static final int RESULT_STATUS_RUNNING = 3;

    private final AiRecognitionRecordMapper aiRecognitionRecordMapper;
    private final AiSolutionMapper aiSolutionMapper;
    private final DataPermissionService dataPermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAiRecognitionRecord(Long id) {
        AiRecognitionRecord oldRecord = getAiRecognitionRecordById(id);
        aiSolutionMapper.deleteByRecordId(oldRecord.getId());
        aiRecognitionRecordMapper.clearChatSessionResultLinksByRecordId(oldRecord.getId());
        aiRecognitionRecordMapper.deleteResultsByRecordId(oldRecord.getId());
        int rows = aiRecognitionRecordMapper.deleteById(oldRecord.getId());
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "AI识别记录不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAiRecognitionRecords(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的AI识别记录");
        }
        for (Long id : ids) {
            getAiRecognitionRecordById(id);
        }
        aiSolutionMapper.deleteByRecordIds(ids);
        aiRecognitionRecordMapper.clearChatSessionResultLinksByRecordIds(ids);
        aiRecognitionRecordMapper.deleteResultsByRecordIds(ids);
        return aiRecognitionRecordMapper.deleteBatchByIds(ids);
    }

    @Override
    public AiRecognitionRecord getAiRecognitionRecordById(Long id) {
        requireId(id);
        AiRecognitionRecord record = aiRecognitionRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "AI识别记录不存在");
        }
        dataPermissionService.requireOwnedResource(record.getUserId());
        return record;
    }

    @Override
    public PageInfo<AiRecognitionRecord> listAiRecognitionRecords(String username, Integer sourceType,
                                                                  Integer status, Integer resultStatus,
                                                                  String cameraName, String plotName,
                                                                  Long recognitionType, String resultName,
                                                                  LocalDate startDate, LocalDate endDate,
                                                                  Integer pageNum, Integer pageSize) {
        QueryParams params = normalizeQueryParams(
                username, sourceType, status, resultStatus, cameraName, plotName,
                recognitionType, resultName, startDate, endDate);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(aiRecognitionRecordMapper.selectList(
                scopedUserId,
                params.username(),
                params.sourceType(),
                params.status(),
                params.resultStatus(),
                params.cameraName(),
                params.plotName(),
                params.recognitionType(),
                params.resultName(),
                params.startTime(),
                params.endTime()));
    }

    @Override
    public Map<String, Object> statisticsAiRecognitionRecords(String username, Integer sourceType,
                                                              Integer status, Integer resultStatus,
                                                              String cameraName, String plotName,
                                                              Long recognitionType, String resultName,
                                                              LocalDate startDate, LocalDate endDate) {
        QueryParams params = normalizeQueryParams(
                username, sourceType, status, resultStatus, cameraName, plotName,
                recognitionType, resultName, startDate, endDate);
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        Map<String, Object> statistics = aiRecognitionRecordMapper.selectStatistics(
                scopedUserId,
                params.username(),
                params.sourceType(),
                params.status(),
                params.resultStatus(),
                params.cameraName(),
                params.plotName(),
                params.recognitionType(),
                params.resultName(),
                params.startTime(),
                params.endTime());
        return statistics == null ? new HashMap<>() : statistics;
    }

    private QueryParams normalizeQueryParams(String username, Integer sourceType, Integer status,
                                             Integer resultStatus, String cameraName, String plotName,
                                             Long recognitionType, String resultName,
                                             LocalDate startDate, LocalDate endDate) {
        validateSourceType(sourceType);
        validateRecordStatus(status);
        validateResultStatus(resultStatus);
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }
        return new QueryParams(
                normalizeOptionalText(username),
                sourceType,
                status,
                resultStatus,
                normalizeOptionalText(cameraName),
                normalizeOptionalText(plotName),
                recognitionType,
                normalizeOptionalText(resultName),
                startDate == null ? null : startDate.atStartOfDay(),
                endDate == null ? null : endDate.atTime(LocalTime.MAX));
    }

    private void validateSourceType(Integer sourceType) {
        if (sourceType != null && sourceType != SOURCE_TYPE_MANUAL && sourceType != SOURCE_TYPE_CAMERA) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "识别来源只能为1或2");
        }
    }

    private void validateRecordStatus(Integer status) {
        if (status != null
                && status != RECORD_STATUS_PENDING
                && status != RECORD_STATUS_RUNNING
                && status != RECORD_STATUS_COMPLETED
                && status != RECORD_STATUS_FAILED) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "识别记录状态只能为1、2、3或4");
        }
    }

    private void validateResultStatus(Integer status) {
        if (status != null
                && status != RESULT_STATUS_SUCCESS
                && status != RESULT_STATUS_FAILED
                && status != RESULT_STATUS_RUNNING) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "识别结果状态只能为1、2或3");
        }
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "AI识别记录ID不能为空");
        }
    }

    private record QueryParams(String username,
                               Integer sourceType,
                               Integer status,
                               Integer resultStatus,
                               String cameraName,
                               String plotName,
                               Long recognitionType,
                               String resultName,
                               LocalDateTime startTime,
                               LocalDateTime endTime) {
    }
}
