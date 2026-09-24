package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AiRecognitionRecord;
import com.smart_plant.smart_plant.entity.AiSolution;
import com.smart_plant.smart_plant.entity.AlertEvent;
import com.smart_plant.smart_plant.entity.FarmTask;
import com.smart_plant.smart_plant.entity.Notification;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.AiSolutionMapper;
import com.smart_plant.smart_plant.mapper.FarmTaskMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import com.smart_plant.smart_plant.service.AiRecognitionRecordService;
import com.smart_plant.smart_plant.service.AiSolutionService;
import com.smart_plant.smart_plant.service.AlertEventService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.NotificationService;
import com.smart_plant.smart_plant.service.RecognitionDecision;
import com.smart_plant.smart_plant.service.RecognitionDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiSolutionServiceImpl implements AiSolutionService {

    private static final int RESULT_STATUS_SUCCESS = 1;
    private static final int TASK_NOT_GENERATED = 0;
    private static final int TASK_GENERATED = 1;
    private static final int SOLUTION_ACTIVE = 1;
    private static final int SOLUTION_TASK_CREATED = 2;
    private static final String SOURCE_TYPE_AI_SOLUTION = "AI_SOLUTION";
    private static final String SOURCE_TYPE_AI_RECOGNITION_RESULT = "ai_recognition_result";
    private static final String REF_TYPE_ALERT_EVENT = "alert_event";
    private static final String REF_TYPE_FARM_TASK = "farm_task";
    private static final int ALERT_TYPE_AI_RECOGNITION = 5;
    private static final int NOTICE_TYPE_FARM_TASK = 2;
    private static final int NOTICE_TYPE_AI_RECOGNITION = 6;

    private final AiSolutionMapper aiSolutionMapper;
    private final FarmTaskMapper farmTaskMapper;
    private final AiRecognitionRecordService aiRecognitionRecordService;
    private final DataPermissionService dataPermissionService;
    private final RecognitionDecisionService recognitionDecisionService;
    private final AlertEventService alertEventService;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSolution generateSolution(Long recordId, Boolean generateTask) {
        AiRecognitionRecord record = aiRecognitionRecordService.getAiRecognitionRecordById(recordId);
        if (record.getResultId() == null || !Integer.valueOf(RESULT_STATUS_SUCCESS).equals(record.getResultStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Only successful recognition records can generate solutions");
        }

        AiSolution solution = aiSolutionMapper.selectLatestByRecordId(record.getId());
        if (solution == null) {
            solution = buildSolution(record, Boolean.TRUE.equals(generateTask));
            aiSolutionMapper.insert(solution);
        } else {
            dataPermissionService.requireOwnedResource(solution.getUserId());
            if (Boolean.TRUE.equals(generateTask) && !Integer.valueOf(1).equals(solution.getGenerateTask())) {
                solution.setGenerateTask(1);
                aiSolutionMapper.updateById(solution);
            }
        }

        solution = aiSolutionMapper.selectById(solution.getId());
        RecognitionDecision decision = recognitionDecisionService.decide(record);
        if (decision.createAlert()) {
            AlertEvent alertEvent = ensureAiRecognitionAlert(record, solution, decision);
            ensureAlertNotification(record, alertEvent, decision);
        }
        if (decision.createFarmTask() && (Boolean.TRUE.equals(generateTask) || decision.automatic())) {
            generateFarmTask(solution.getId(), null);
        }
        return aiSolutionMapper.selectById(solution.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmTask generateFarmTask(Long solutionId, FarmTask request) {
        AiSolution solution = getAiSolutionById(solutionId);
        AiRecognitionRecord record = aiRecognitionRecordService.getAiRecognitionRecordById(solution.getRecordId());
        RecognitionDecision decision = recognitionDecisionService.decide(record);

        AlertEvent alertEvent = null;
        if (decision.createAlert()) {
            alertEvent = ensureAiRecognitionAlert(record, solution, decision);
            ensureAlertNotification(record, alertEvent, decision);
        }

        if (Integer.valueOf(TASK_GENERATED).equals(solution.getTaskGenerated()) && solution.getFarmTaskId() != null) {
            FarmTask existingTask = farmTaskMapper.selectById(solution.getFarmTaskId());
            ensureTaskNotification(existingTask, alertEvent, decision);
            return existingTask;
        }
        if (solution.getPlotId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Manual recognition has no plot context and cannot generate a farm task automatically");
        }

        FarmTask existingTask = farmTaskMapper.selectBySource(SOURCE_TYPE_AI_SOLUTION, solution.getId());
        if (existingTask != null) {
            aiSolutionMapper.updateTaskLink(solution.getId(), existingTask.getId(), TASK_GENERATED, SOLUTION_TASK_CREATED);
            ensureTaskNotification(existingTask, alertEvent, decision);
            return farmTaskMapper.selectById(existingTask.getId());
        }

        FarmTask farmTask = buildFarmTask(solution, request, decision);
        farmTaskMapper.insert(farmTask);
        aiSolutionMapper.updateTaskLink(solution.getId(), farmTask.getId(), TASK_GENERATED, SOLUTION_TASK_CREATED);
        FarmTask savedTask = farmTaskMapper.selectById(farmTask.getId());
        ensureTaskNotification(savedTask, alertEvent, decision);
        return savedTask;
    }

    @Override
    public AiSolution getAiSolutionById(Long id) {
        requireId(id, "AI solution id is required");
        AiSolution solution = aiSolutionMapper.selectById(id);
        if (solution == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "AI solution does not exist");
        }
        dataPermissionService.requireOwnedResource(solution.getUserId());
        return solution;
    }

    @Override
    public AiSolution getLatestByRecordId(Long recordId) {
        requireId(recordId, "Recognition record id is required");
        AiSolution solution = aiSolutionMapper.selectLatestByRecordId(recordId);
        if (solution != null) {
            dataPermissionService.requireOwnedResource(solution.getUserId());
        }
        return solution;
    }

    @Override
    public PageInfo<AiSolution> listAiSolutions(String plotName, String cameraName, String solutionTitle,
                                                Integer priorityLevel, Integer taskGenerated, Integer status,
                                                LocalDate startDate, LocalDate endDate, Integer pageNum,
                                                Integer pageSize) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Start date cannot be after end date");
        }
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(aiSolutionMapper.selectList(
                scopedUserId,
                normalizeOptionalText(plotName),
                normalizeOptionalText(cameraName),
                normalizeOptionalText(solutionTitle),
                priorityLevel,
                taskGenerated,
                status,
                startTime,
                endTime));
    }

    @Override
    public Map<String, Object> statisticsAiSolutions() {
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        Map<String, Object> statistics = aiSolutionMapper.selectStatistics(scopedUserId);
        return statistics == null ? new HashMap<>() : statistics;
    }

    private AiSolution buildSolution(AiRecognitionRecord record, boolean generateTask) {
        AiSolution solution = new AiSolution();
        solution.setRecordId(record.getId());
        solution.setResultId(record.getResultId());
        solution.setUserId(record.getUserId());
        solution.setPlotId(record.getPlotId());
        solution.setCameraId(record.getCameraId());
        solution.setSolutionTitle(buildSolutionTitle(record));
        solution.setSolutionSummary(firstNonBlank(record.getResultSummary(), limit(record.getSuggestion(), 480), "AI recognition solution"));
        solution.setSolutionDetail(buildSolutionDetail(record));
        solution.setPriorityLevel(priorityFromSeverity(record.getSeverityLevel()));
        solution.setGenerateTask(generateTask ? 1 : 0);
        solution.setTaskGenerated(TASK_NOT_GENERATED);
        solution.setStatus(SOLUTION_ACTIVE);
        solution.setRemark(record.getResultRemark());
        return solution;
    }

    private FarmTask buildFarmTask(AiSolution solution, FarmTask request, RecognitionDecision decision) {
        FarmTask farmTask = new FarmTask();
        farmTask.setUserId(solution.getUserId());
        farmTask.setPlotId(solution.getPlotId());
        farmTask.setBatchId(farmTaskMapper.selectActiveBatchIdByPlotId(solution.getPlotId()));
        farmTask.setTaskTitle(firstNonBlank(request == null ? null : request.getTaskTitle(), solution.getSolutionTitle()));
        farmTask.setTaskType(request == null || request.getTaskType() == null ? inferTaskType(solution, decision) : request.getTaskType());
        farmTask.setTaskContent(firstNonBlank(request == null ? null : request.getTaskContent(), solution.getSolutionDetail(), solution.getSolutionSummary()));
        farmTask.setPriority(request == null || request.getPriority() == null ? defaultTaskPriority(solution, decision) : request.getPriority());
        // AI 任务只需要明确最晚处理时间，避免计划起止区间给执行人员造成歧义。
        farmTask.setDeadlineTime(request == null ? null : request.getDeadlineTime());
        if (farmTask.getDeadlineTime() == null) {
            farmTask.setDeadlineTime(LocalDateTime.now().plusDays(farmTask.getPriority() >= 4 ? 1 : 3));
        }
        farmTask.setStatus(1);
        farmTask.setExecutorId(request == null ? null : request.getExecutorId());
        if (farmTask.getExecutorId() == null && CurrentUserContext.get() != null) {
            farmTask.setExecutorId(CurrentUserContext.get().getId());
        }
        farmTask.setRemark(firstNonBlank(request == null ? null : request.getRemark(), "Generated from AI recognition solution"));
        farmTask.setSourceType(SOURCE_TYPE_AI_SOLUTION);
        farmTask.setSourceId(solution.getId());
        farmTask.setAiSolutionId(solution.getId());
        return farmTask;
    }

    private AlertEvent ensureAiRecognitionAlert(AiRecognitionRecord record, AiSolution solution, RecognitionDecision decision) {
        AlertEvent alertEvent = new AlertEvent();
        alertEvent.setUserId(solution.getUserId());
        alertEvent.setPlotId(solution.getPlotId());
        alertEvent.setBatchId(solution.getPlotId() == null ? null : farmTaskMapper.selectActiveBatchIdByPlotId(solution.getPlotId()));
        alertEvent.setAlertType(ALERT_TYPE_AI_RECOGNITION);
        alertEvent.setAlertTitle(limit("AI recognition abnormal: " + firstNonBlank(record.getResultName(), record.getRecognitionTypeName(), "unknown"), 100));
        alertEvent.setAlertContent(buildAlertContent(record, solution, decision));
        alertEvent.setMetricCode("ai_recognition");
        alertEvent.setMetricName(firstNonBlank(record.getRecognitionTypeName(), "AI recognition"));
        alertEvent.setAlertLevel(decision.alertLevel());
        alertEvent.setSourceType(SOURCE_TYPE_AI_RECOGNITION_RESULT);
        alertEvent.setSourceId(solution.getResultId());
        alertEvent.setProcessStatus(1);
        alertEvent.setStatus(1);
        alertEvent.setRemark("Generated from AI recognition solution " + solution.getId());
        return alertEventService.createIfAbsent(alertEvent);
    }

    private void ensureAlertNotification(AiRecognitionRecord record, AlertEvent alertEvent, RecognitionDecision decision) {
        if (alertEvent == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(alertEvent.getUserId());
        notification.setTitle(limit("AI recognition alert: " + firstNonBlank(record.getResultName(), "abnormal result"), 100));
        notification.setContent(firstNonBlank(alertEvent.getAlertContent(), decision.reason()));
        notification.setNoticeType(NOTICE_TYPE_AI_RECOGNITION);
        notification.setRefType(REF_TYPE_ALERT_EVENT);
        notification.setRefId(alertEvent.getId());
        notification.setAlertId(alertEvent.getId());
        notification.setLevel(alertEvent.getAlertLevel());
        notification.setIsRead(0);
        notification.setStatus(1);
        notificationService.createIfAbsent(notification);
    }

    private void ensureTaskNotification(FarmTask farmTask, AlertEvent alertEvent, RecognitionDecision decision) {
        if (farmTask == null || farmTask.getId() == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(farmTask.getUserId());
        notification.setTitle(limit("Farm task generated: " + firstNonBlank(farmTask.getTaskTitle(), "AI task"), 100));
        notification.setContent(firstNonBlank(farmTask.getTaskContent(), decision.reason()));
        notification.setNoticeType(NOTICE_TYPE_FARM_TASK);
        notification.setRefType(REF_TYPE_FARM_TASK);
        notification.setRefId(farmTask.getId());
        notification.setTaskId(farmTask.getId());
        notification.setAlertId(alertEvent == null ? null : alertEvent.getId());
        notification.setLevel(priorityToNotificationLevel(farmTask.getPriority()));
        notification.setIsRead(0);
        notification.setStatus(1);
        notificationService.createIfAbsent(notification);
    }

    private String buildAlertContent(AiRecognitionRecord record, AiSolution solution, RecognitionDecision decision) {
        StringBuilder content = new StringBuilder();
        appendLine(content, "Reason", decision.reason());
        appendLine(content, "Plot", solution.getPlotName());
        appendLine(content, "Camera", solution.getCameraName());
        appendLine(content, "Result", record.getResultName());
        appendLine(content, "Summary", record.getResultSummary());
        appendLine(content, "Suggestion", record.getSuggestion());
        if (record.getConfidence() != null) {
            appendLine(content, "Confidence", record.getConfidence() + "%");
        }
        if (record.getSeverityLevel() != null) {
            appendLine(content, "Severity", String.valueOf(record.getSeverityLevel()));
        }
        return content.isEmpty() ? decision.reason() : content.toString().trim();
    }

    private String buildSolutionTitle(AiRecognitionRecord record) {
        String resultName = firstNonBlank(record.getResultName(), record.getRecognitionTypeName(), "AI recognition");
        return limit("AI solution: " + resultName, 120);
    }

    private String buildSolutionDetail(AiRecognitionRecord record) {
        StringBuilder detail = new StringBuilder();
        appendLine(detail, "Recognition type", record.getRecognitionTypeName());
        appendLine(detail, "Result", record.getResultName());
        appendLine(detail, "Summary", record.getResultSummary());
        appendLine(detail, "Suggestion", record.getSuggestion());
        appendLine(detail, "Detail", record.getResultDetail());
        if (record.getConfidence() != null) {
            appendLine(detail, "Confidence", record.getConfidence() + "%");
        }
        if (record.getSeverityLevel() != null) {
            appendLine(detail, "Severity", String.valueOf(record.getSeverityLevel()));
        }
        return detail.isEmpty() ? "No detailed solution content" : detail.toString().trim();
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(label).append(": ").append(value.trim()).append(System.lineSeparator());
        }
    }

    private Integer priorityFromSeverity(Integer severityLevel) {
        if (severityLevel == null) {
            return 2;
        }
        return switch (severityLevel) {
            case 3 -> 4;
            case 2 -> 3;
            case 1 -> 2;
            default -> 2;
        };
    }

    private Integer inferTaskType(AiSolution solution, RecognitionDecision decision) {
        if (decision != null && decision.taskType() != null) {
            return decision.taskType();
        }
        String text = (solution.getSolutionTitle() + " " + solution.getSolutionDetail()).toLowerCase();
        if (text.contains("pest") || text.contains("disease")) {
            return 3;
        }
        return 5;
    }

    private Integer defaultTaskPriority(AiSolution solution, RecognitionDecision decision) {
        if (decision != null && decision.taskPriority() != null) {
            return decision.taskPriority();
        }
        return solution.getPriorityLevel();
    }

    private Integer priorityToNotificationLevel(Integer priority) {
        if (priority == null) {
            return 1;
        }
        if (priority >= 4) {
            return 3;
        }
        if (priority >= 3) {
            return 2;
        }
        return 1;
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void requireId(Long id, String message) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
    }
}
