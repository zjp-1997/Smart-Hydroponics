package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.FarmTask;
import com.smart_plant.smart_plant.entity.FarmTaskRecord;
import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.AiSolutionMapper;
import com.smart_plant.smart_plant.mapper.FarmTaskMapper;
import com.smart_plant.smart_plant.mapper.FarmTaskRecordMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.FarmTaskService;
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
public class FarmTaskServiceImpl implements FarmTaskService {

    private final FarmTaskMapper farmTaskMapper;
    private final FarmTaskRecordMapper farmTaskRecordMapper;
    private final AiSolutionMapper aiSolutionMapper;
    private final PlotMapper plotMapper;
    private final UserMapper userMapper;
    private final DataPermissionService dataPermissionService;

    /** 新增农事：校验必填字段后，根据地块归属自动绑定发布人和当前种植批次。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmTask addFarmTask(FarmTask farmTask) {
        validateCreate(farmTask);
        normalizeCreate(farmTask);
        farmTaskMapper.insert(farmTask);
        return farmTaskMapper.selectById(farmTask.getId());
    }

    /** 编辑农事：先读取旧数据做权限校验，再用新旧字段合并后的完整对象更新数据库。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmTask updateFarmTask(FarmTask farmTask) {
        if (farmTask == null || farmTask.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Farm task id is required");
        }
        FarmTask oldTask = getFarmTaskById(farmTask.getId());
        normalizeUpdate(farmTask, oldTask);
        int rows = farmTaskMapper.updateById(farmTask);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "Failed to update farm task");
        }
        return farmTaskMapper.selectById(farmTask.getId());
    }

    /** 删除农事：先清理反馈记录和 AI 方案反向关联，避免留下孤立业务数据。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFarmTask(Long id) {
        FarmTask task = getFarmTaskById(id);
        farmTaskRecordMapper.deleteByTaskId(task.getId());
        aiSolutionMapper.clearTaskLinkByFarmTaskId(task.getId());
        int rows = farmTaskMapper.deleteById(task.getId());
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "Farm task does not exist");
        }
    }

    /** 批量删除农事：逐条校验数据权限后再批量删除，防止越权删除其他用户数据。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteFarmTasks(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Please select farm tasks to delete");
        }
        for (Long id : ids) {
            getFarmTaskById(id);
        }
        farmTaskRecordMapper.deleteByTaskIds(ids);
        aiSolutionMapper.clearTaskLinkByFarmTaskIds(ids);
        return farmTaskMapper.deleteBatchByIds(ids);
    }

    /** 查询详情：所有详情入口都统一做数据权限校验。 */
    @Override
    public FarmTask getFarmTaskById(Long id) {
        requireId(id);
        FarmTask task = farmTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "Farm task does not exist");
        }
        dataPermissionService.requireOwnedResource(task.getUserId());
        return task;
    }

    /** 管理端状态流转也写入统一执行时间线，避免出现只有状态而没有过程记录的数据。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmTask updateTaskStatus(Long id, Integer status, String completeRemark) {
        FarmTask task = getFarmTaskById(id);
        if (!Integer.valueOf(2).equals(status) && !Integer.valueOf(3).equals(status)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Only start or complete actions are allowed");
        }
        if (Integer.valueOf(3).equals(status) && !Integer.valueOf(2).equals(task.getStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "The task must be started before completion");
        }
        User operator = dataPermissionService.currentUser();
        LocalDateTime now = LocalDateTime.now();
        int rows = Integer.valueOf(2).equals(status)
                ? farmTaskMapper.startTask(task.getId(), operator.getId(), now)
                : farmTaskMapper.completeTask(task.getId(), now,
                        firstNonBlank(completeRemark, "管理员确认任务完成"));
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "Task status changed, please refresh and retry");
        }
        FarmTaskRecord record = new FarmTaskRecord();
        record.setTaskId(task.getId());
        record.setOperatorId(operator.getId());
        record.setOperatorNameSnapshot(firstNonBlank(operator.getNickname(), operator.getUsername()));
        record.setActionType(Integer.valueOf(2).equals(status) ? 1 : 2);
        record.setActionContent(Integer.valueOf(2).equals(status) ? "管理端开始执行任务" : "管理端完成任务");
        record.setFeedbackDetail(normalizeOptionalText(completeRemark));
        record.setBeforeStatus(task.getStatus());
        record.setAfterStatus(status);
        record.setSourceClient("SMART_FARM");
        record.setExecuteTime(now);
        farmTaskRecordMapper.insert(record);
        return farmTaskMapper.selectById(task.getId());
    }

    /** 分页查询：支持农事标题、农事内容、地块名称以及兼容性扩展条件。 */
    @Override
    public PageInfo<FarmTask> listFarmTasks(String plotName, String taskTitle, String taskContent,
                                            Integer taskType, Integer status, String sourceType,
                                            LocalDate startDate, LocalDate endDate,
                                            Integer pageNum, Integer pageSize) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Start date cannot be after end date");
        }
        validateStatus(status);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        LocalDateTime deadlineStart = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime deadlineEnd = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(farmTaskMapper.selectList(
                scopedUserId,
                normalizeOptionalText(plotName),
                normalizeOptionalText(taskTitle),
                normalizeOptionalText(taskContent),
                taskType,
                status,
                normalizeOptionalText(sourceType),
                deadlineStart,
                deadlineEnd));
    }

    /** 统计当前权限范围内的农事数量，用于管理端顶部统计卡片。 */
    @Override
    public Map<String, Object> statisticsFarmTasks() {
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        Map<String, Object> statistics = farmTaskMapper.selectStatistics(scopedUserId);
        return statistics == null ? new HashMap<>() : statistics;
    }

    private void validateCreate(FarmTask farmTask) {
        if (farmTask == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Farm task is required");
        }
        if (farmTask.getPlotId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Plot is required");
        }
        if (!StringUtils.hasText(farmTask.getTaskTitle())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Task title is required");
        }
        if (farmTask.getTaskType() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Task type is required");
        }
        if (farmTask.getDeadlineTime() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Deadline time is required");
        }
    }

    private void normalizeCreate(FarmTask farmTask) {
        normalizeCommon(farmTask);
        Plot plot = requirePlot(farmTask.getPlotId());
        dataPermissionService.requireFarmManager(plot.getUserId());
        farmTask.setUserId(plot.getUserId());
        validateExecutor(farmTask.getExecutorId(), plot.getUserId());
        if (farmTask.getBatchId() == null) {
            farmTask.setBatchId(farmTaskMapper.selectActiveBatchIdByPlotId(plot.getId()));
        }
        if (farmTask.getStatus() == null) {
            farmTask.setStatus(1);
        }
        if (farmTask.getPriority() == null) {
            farmTask.setPriority(2);
        }
    }

    private void normalizeUpdate(FarmTask farmTask, FarmTask oldTask) {
        farmTask.setPlotId(farmTask.getPlotId() == null ? oldTask.getPlotId() : farmTask.getPlotId());
        farmTask.setTaskTitle(farmTask.getTaskTitle() == null ? oldTask.getTaskTitle() : farmTask.getTaskTitle());
        farmTask.setTaskType(farmTask.getTaskType() == null ? oldTask.getTaskType() : farmTask.getTaskType());
        farmTask.setTaskContent(farmTask.getTaskContent() == null ? oldTask.getTaskContent() : farmTask.getTaskContent());
        farmTask.setPriority(farmTask.getPriority() == null ? oldTask.getPriority() : farmTask.getPriority());
        farmTask.setDeadlineTime(farmTask.getDeadlineTime() == null ? oldTask.getDeadlineTime() : farmTask.getDeadlineTime());
        // 普通编辑只修改任务定义；状态与实际执行时间必须由专用状态机接口维护。
        farmTask.setActualStartTime(oldTask.getActualStartTime());
        farmTask.setActualEndTime(oldTask.getActualEndTime());
        farmTask.setStatus(oldTask.getStatus());
        farmTask.setExecutorId(farmTask.getExecutorId() == null ? oldTask.getExecutorId() : farmTask.getExecutorId());
        farmTask.setCompleteRemark(oldTask.getCompleteRemark());
        farmTask.setRemark(farmTask.getRemark() == null ? oldTask.getRemark() : farmTask.getRemark());
        farmTask.setSourceType(farmTask.getSourceType() == null ? oldTask.getSourceType() : farmTask.getSourceType());
        farmTask.setSourceId(farmTask.getSourceId() == null ? oldTask.getSourceId() : farmTask.getSourceId());
        farmTask.setAiSolutionId(farmTask.getAiSolutionId() == null ? oldTask.getAiSolutionId() : farmTask.getAiSolutionId());
        normalizeCommon(farmTask);
        Plot plot = requirePlot(farmTask.getPlotId());
        dataPermissionService.requireFarmManager(plot.getUserId());
        farmTask.setUserId(plot.getUserId());
        validateExecutor(farmTask.getExecutorId(), plot.getUserId());
        farmTask.setBatchId(farmTask.getBatchId() == null ? farmTaskMapper.selectActiveBatchIdByPlotId(plot.getId()) : farmTask.getBatchId());
    }

    private void normalizeCommon(FarmTask farmTask) {
        farmTask.setTaskTitle(normalizeRequiredText(farmTask.getTaskTitle(), "Task title is required"));
        farmTask.setTaskContent(normalizeOptionalText(farmTask.getTaskContent()));
        farmTask.setCompleteRemark(normalizeOptionalText(farmTask.getCompleteRemark()));
        farmTask.setRemark(normalizeOptionalText(farmTask.getRemark()));
        farmTask.setSourceType(normalizeOptionalText(farmTask.getSourceType()));
        validateTaskType(farmTask.getTaskType());
        validatePriority(farmTask.getPriority());
        validateStatus(farmTask.getStatus());
        if (farmTask.getActualStartTime() != null
                && farmTask.getActualEndTime() != null
                && farmTask.getActualStartTime().isAfter(farmTask.getActualEndTime())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Actual start time cannot be after actual end time");
        }
    }

    private Plot requirePlot(Long plotId) {
        Plot plot = plotMapper.selectById(plotId);
        if (plot == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "Plot does not exist");
        }
        return plot;
    }

    private void validateExecutor(Long executorId, Long ownerId) {
        if (executorId == null) {
            return;
        }
        User user = userMapper.selectById(executorId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "执行人不存在或已停用");
        }
        // 农场主可自行执行；普通用户必须在注册绑定表中属于该农场主。
        if (ownerId.equals(executorId) && "farm_owner".equalsIgnoreCase(user.getRoleCode())) {
            return;
        }
        if (!"user".equalsIgnoreCase(user.getRoleCode())
                || userMapper.countBoundUserByOwnerId(ownerId, executorId) != 1) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "执行人未绑定到该农场");
        }
    }

    private void validateTaskType(Integer taskType) {
        if (taskType == null || taskType < 1 || taskType > 8) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Task type must be between 1 and 8");
        }
    }

    private void validatePriority(Integer priority) {
        if (priority != null && (priority < 1 || priority > 4)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Priority must be between 1 and 4");
        }
    }

    private void validateStatus(Integer status) {
        if (status != null && (status < 1 || status > 5)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Farm task status must be between 1 and 5");
        }
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 返回第一个非空文本，用于为执行记录提供稳定的展示兜底。 */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalizeRequiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
        return value.trim();
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Farm task id is required");
        }
    }
}
