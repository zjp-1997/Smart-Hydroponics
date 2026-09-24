package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.ClientFarmTaskActionRequest;
import com.smart_plant.smart_plant.dto.ClientFarmTaskDateSummaryResponse;
import com.smart_plant.smart_plant.dto.ClientFarmTaskResponse;
import com.smart_plant.smart_plant.dto.ClientFarmTaskStatisticsResponse;
import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.entity.FarmTask;
import com.smart_plant.smart_plant.entity.FarmTaskRecord;
import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.FarmTaskMapper;
import com.smart_plant.smart_plant.mapper.FarmTaskRecordMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.ClientFarmTaskService;
import com.smart_plant.smart_plant.service.CropImageService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * farm 用户端农事任务服务实现。
 *
 * <p>所有查询都基于登录 token 中的用户 ID 过滤，保证移动端只能看到自己的任务。</p>
 */
@Service
@RequiredArgsConstructor
public class ClientFarmTaskServiceImpl implements ClientFarmTaskService {

    private static final int CLIENT_TASK_DAYS = 7;
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_RUNNING = 2;
    private static final int STATUS_COMPLETED = 3;
    private static final int STATUS_OVERDUE_LEGACY = 4;
    private static final int ACTION_START = 1;
    private static final int ACTION_COMPLETE = 2;
    private static final int ACTION_PROGRESS = 3;
    private static final String SOURCE_FARM_APP = "FARM_APP";
    private static final String SOURCE_SYSTEM = "SYSTEM";
    private static final String COMPLETION_IMAGE_PREFIX = "[\"/uploads/task-completion-images/";

    private final FarmTaskMapper farmTaskMapper;
    private final FarmTaskRecordMapper farmTaskRecordMapper;
    private final PlotMapper plotMapper;
    private final DataPermissionService dataPermissionService;
    private final CropImageService cropImageService;

    @Override
    public List<ClientFarmTaskDateSummaryResponse> listDateSummaries(LocalDate startDate) {
        LocalDate firstDate = startDate == null ? LocalDate.now() : startDate;
        LocalDate lastDate = firstDate.plusDays(CLIENT_TASK_DAYS - 1L);
        Long currentUserId = requireCurrentUserId();

        // 先按日期聚合数据库已有任务数量，再补齐没有任务的日期，确保前端永远展示 7 天。
        Map<LocalDate, ClientFarmTaskDateSummaryResponse> countMap = farmTaskMapper
                .selectClientDateCounts(currentUserId, firstDate.atStartOfDay(), lastDate.atTime(LocalTime.MAX))
                .stream()
                .collect(Collectors.toMap(ClientFarmTaskDateSummaryResponse::getTaskDate, Function.identity()));

        List<ClientFarmTaskDateSummaryResponse> summaries = new ArrayList<>(CLIENT_TASK_DAYS);
        for (int index = 0; index < CLIENT_TASK_DAYS; index++) {
            LocalDate date = firstDate.plusDays(index);
            ClientFarmTaskDateSummaryResponse summary = countMap.get(date);
            int taskCount = summary == null || summary.getTaskCount() == null ? 0 : summary.getTaskCount();
            summaries.add(new ClientFarmTaskDateSummaryResponse(
                    date,
                    toChineseWeekday(date),
                    String.valueOf(date.getDayOfMonth()),
                    taskCount));
        }
        return summaries;
    }

    @Override
    public List<ClientFarmTaskResponse> listTasks(LocalDate taskDate, Integer taskType) {
        LocalDate currentDate = taskDate == null ? LocalDate.now() : taskDate;
        validateClientTaskType(taskType);
        return farmTaskMapper.selectClientTasks(
                requireCurrentUserId(),
                taskType,
                currentDate.atStartOfDay(),
                currentDate.atTime(LocalTime.MAX));
    }

    @Override
    public List<ClientFarmTaskResponse> listMyTasks(Integer status) {
        validateClientTaskStatus(status);
        return farmTaskMapper.selectClientTasksByStatus(requireCurrentUserId(), status);
    }

    /** 地块任务查询始终带入登录用户 ID，避免通过修改 plotId 读取他人任务。 */
    @Override
    public List<ClientFarmTaskResponse> listTasksByPlot(Long plotId) {
        if (plotId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块ID不能为空");
        }
        return farmTaskMapper.selectClientTaskViewsByPlotId(requireCurrentUserId(), plotId);
    }

    /** 单任务详情沿用统一归属校验，防止通过枚举任务 ID 越权读取他人数据。 */
    @Override
    public ClientFarmTaskResponse getTaskDetail(Long id) {
        FarmTask task = requireClientTask(id);
        return getClientTask(task.getId(), task.getUserId());
    }

    /** 上传前先校验任务归属和状态，避免给无权访问或已结束的任务写入孤立凭证。 */
    @Override
    public CropImageUploadResult uploadCompletionImage(Long id, MultipartFile image) {
        FarmTask task = requireClientTask(id);
        if (!Integer.valueOf(STATUS_RUNNING).equals(task.getStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "只有进行中的任务可以上传完成凭证");
        }
        return cropImageService.uploadTaskCompletionImage(image);
    }

    @Override
    public ClientFarmTaskStatisticsResponse statistics() {
        Map<String, Object> statistics = farmTaskMapper.selectClientStatistics(requireCurrentUserId());
        return new ClientFarmTaskStatisticsResponse(
                readCount(statistics, "pendingCount"),
                readCount(statistics, "runningCount"),
                readCount(statistics, "completedCount"),
                readCount(statistics, "overdueCount"));
    }

    /** 开始任务时原子更新状态、锁定执行人并写入开始事件。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientFarmTaskResponse executeTask(Long id, ClientFarmTaskActionRequest request) {
        FarmTask task = requireClientTask(id);
        Long currentUserId = requireCurrentUserId();
        String requestId = normalizeRequestId(request);
        if (isRequestProcessed(requestId, id, currentUserId)) {
            return getClientTask(id, currentUserId);
        }
        if (Integer.valueOf(STATUS_RUNNING).equals(task.getStatus())) {
            return getClientTask(id, currentUserId);
        }
        if (!Integer.valueOf(STATUS_PENDING).equals(task.getStatus())
                && !Integer.valueOf(STATUS_OVERDUE_LEGACY).equals(task.getStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "当前任务状态不可开始执行");
        }

        LocalDateTime executeTime = LocalDateTime.now();
        // 已指派任务保留原执行人；农场主代为启动时不应因执行人不同而误报状态冲突。
        Long executorId = task.getExecutorId() == null ? currentUserId : task.getExecutorId();
        int rows = farmTaskMapper.startTask(id, executorId, executeTime);
        if (rows == 0) {
            // 两个客户端同时开始同一任务时，后到请求直接返回最新快照，保持开始命令幂等。
            ClientFarmTaskResponse latestTask = getClientTask(id, currentUserId);
            if (Integer.valueOf(STATUS_RUNNING).equals(latestTask.getStatus())) {
                return latestTask;
            }
            throw new BusinessException(ResponseCode.FAIL, "任务状态已发生变化，请刷新后重试");
        }
        insertTimelineRecord(task, currentUserId, ACTION_START, STATUS_RUNNING, executeTime,
                request, requestId, "开始执行农事任务");
        return getClientTask(id, currentUserId);
    }

    /** 提交进度只追加时间线事件，确保过程描述不会覆盖任务当前快照。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientFarmTaskResponse submitProgress(Long id, ClientFarmTaskActionRequest request) {
        FarmTask task = requireClientTask(id);
        Long currentUserId = requireCurrentUserId();
        validateActionRequest(request, true, false);
        String requestId = normalizeRequestId(request);
        if (isRequestProcessed(requestId, id, currentUserId)) {
            return getClientTask(id, currentUserId);
        }
        if (!Integer.valueOf(STATUS_RUNNING).equals(task.getStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "只有进行中的任务可以提交进度");
        }
        insertTimelineRecord(task, currentUserId, ACTION_PROGRESS, STATUS_RUNNING, LocalDateTime.now(),
                request, requestId, "提交任务进度");
        return getClientTask(id, currentUserId);
    }

    /** 完成动作使用条件更新和事件插入组成同一事务，任一步失败都会整体回滚。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientFarmTaskResponse completeTask(Long id, ClientFarmTaskActionRequest request) {
        FarmTask task = requireClientTask(id);
        Long currentUserId = requireCurrentUserId();
        validateActionRequest(request, false, true);
        String requestId = normalizeRequestId(request);
        if (isRequestProcessed(requestId, id, currentUserId)
                || Integer.valueOf(STATUS_COMPLETED).equals(task.getStatus())) {
            return getClientTask(id, currentUserId);
        }
        if (!Integer.valueOf(STATUS_RUNNING).equals(task.getStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "任务必须先开始执行才能完成");
        }

        LocalDateTime executeTime = LocalDateTime.now();
        String completion = firstNonBlank(request.getFeedbackDetail(), request.getActionContent(), "任务已完成");
        int rows = farmTaskMapper.completeTask(id, executeTime, completion);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "任务状态已发生变化，请刷新后重试");
        }
        insertTimelineRecord(task, currentUserId, ACTION_COMPLETE, STATUS_COMPLETED, executeTime,
                request, requestId, completion);
        return getClientTask(id, currentUserId);
    }

    /** 时间线查询先验证任务归属，再按发生时间正序返回全部业务事件。 */
    @Override
    public List<FarmTaskRecord> listTimeline(Long id) {
        FarmTask task = requireClientTask(id);
        return farmTaskRecordMapper.selectTimeline(task.getId(), task.getUserId());
    }

    /** 地块时间线把任务创建状态与后续不可变事件合并，避免前端逐个任务发起请求。 */
    @Override
    public List<FarmTaskRecord> listPlotTimeline(Long plotId) {
        Plot plot = requireClientPlot(plotId);
        List<FarmTaskRecord> timeline = new ArrayList<>();
        Long viewerId = requireCurrentUserId();
        List<FarmTask> visibleTasks = farmTaskMapper.selectClientTasksByPlotId(plot.getUserId(), plot.getId())
                .stream()
                .filter(task -> viewerId.equals(plot.getUserId()) || viewerId.equals(task.getExecutorId()))
                .toList();
        Set<Long> visibleTaskIds = visibleTasks.stream().map(FarmTask::getId).collect(Collectors.toSet());
        visibleTasks
                .forEach(task -> timeline.add(toCreatedRecord(task)));
        // 工作人员只能读取自己受派任务的操作记录，不能通过地块时间线浏览其他人的任务。
        farmTaskRecordMapper.selectListByPlotId(plot.getUserId(), plot.getId()).stream()
                .filter(record -> visibleTaskIds.contains(record.getTaskId()))
                .forEach(timeline::add);
        timeline.sort(Comparator
                .comparing(ClientFarmTaskServiceImpl::recordTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(FarmTaskRecord::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return timeline;
    }

    private FarmTaskRecord toCreatedRecord(FarmTask task) {
        FarmTaskRecord record = new FarmTaskRecord();
        record.setId(-task.getId());
        record.setTaskId(task.getId());
        record.setTaskTitle(task.getTaskTitle());
        record.setUserId(task.getUserId());
        record.setPlotId(task.getPlotId());
        record.setPlotName(task.getPlotName());
        record.setOperatorNameSnapshot("系统记录");
        record.setOperatorName("系统记录");
        record.setActionType(0);
        record.setActionContent("农事任务已创建，等待开始执行");
        record.setSourceClient(SOURCE_SYSTEM);
        record.setAfterStatus(STATUS_PENDING);
        record.setExecuteTime(task.getCreateTime());
        record.setCreateTime(task.getCreateTime());
        return record;
    }

    private static LocalDateTime recordTime(FarmTaskRecord record) {
        return record.getExecuteTime() == null ? record.getCreateTime() : record.getExecuteTime();
    }

    /** 读取任务并统一执行移动端资源权限校验。 */
    private FarmTask requireClientTask(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农事任务ID不能为空");
        }
        FarmTask task = farmTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "农事任务不存在");
        }
        Long viewerId = requireCurrentUserId();
        if (!viewerId.equals(task.getUserId())) {
            // 只允许注册绑定到该农场且被明确指定为执行人的普通用户操作。
            if (!viewerId.equals(task.getExecutorId())) {
                throw new BusinessException(ResponseCode.FORBIDDEN, "任务未分配给当前用户");
            }
            dataPermissionService.requireClientFarmReader(task.getUserId());
        } else {
            dataPermissionService.requireAgriculturalOperator(task.getUserId());
        }
        return task;
    }

    private Plot requireClientPlot(Long plotId) {
        if (plotId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块ID不能为空");
        }
        Plot plot = plotMapper.selectById(plotId);
        if (plot == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "地块不存在");
        }
        dataPermissionService.requireClientFarmReader(plot.getUserId());
        return plot;
    }

    /** 构造不可变的业务事件，操作人、状态和来源均由服务端写入。 */
    private void insertTimelineRecord(FarmTask task, Long operatorId, int actionType, int afterStatus,
                                      LocalDateTime executeTime, ClientFarmTaskActionRequest request,
                                      String requestId, String defaultContent) {
        User operator = dataPermissionService.currentUser();
        FarmTaskRecord record = new FarmTaskRecord();
        record.setTaskId(task.getId());
        record.setOperatorId(operatorId);
        record.setOperatorNameSnapshot(resolveUserName(operator));
        record.setActionType(actionType);
        record.setActionContent(firstNonBlank(request == null ? null : request.getActionContent(), defaultContent));
        record.setResultStatus(request == null ? null : request.getResultStatus());
        record.setProgressPercent(request == null ? null : request.getProgressPercent());
        record.setFeedbackDetail(normalizeText(request == null ? null : request.getFeedbackDetail()));
        record.setAttachments(normalizeText(request == null ? null : request.getAttachments()));
        record.setRequestId(requestId);
        record.setSourceClient(SOURCE_FARM_APP);
        record.setBeforeStatus(task.getStatus());
        record.setAfterStatus(afterStatus);
        record.setExecuteTime(executeTime);
        if (farmTaskRecordMapper.insert(record) == 0) {
            throw new BusinessException(ResponseCode.FAIL, "农事执行记录保存失败");
        }
    }

    private void validateActionRequest(ClientFarmTaskActionRequest request,
                                       boolean progressRequired,
                                       boolean completionRequired) {
        if (request == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "执行参数不能为空");
        }
        if (progressRequired && (request.getProgressPercent() == null
                || request.getProgressPercent() < 0 || request.getProgressPercent() > 100)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "执行进度必须在0到100之间");
        }
        if (completionRequired && !StringUtils.hasText(request.getFeedbackDetail())
                && !StringUtils.hasText(request.getActionContent())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请填写完成说明");
        }
        if (completionRequired && !hasValidCompletionImage(request.getAttachments())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请先上传任务完成凭证图片");
        }
        if (request.getResultStatus() != null
                && (request.getResultStatus() < 1 || request.getResultStatus() > 3)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "执行结果状态不正确");
        }
    }

    /**
     * 完成动作只接受本系统任务凭证目录中的单张图片 JSON，防止客户端伪造任意外链。
     * 文件本身已在上传接口中完成大小和 MIME 类型校验。
     */
    private boolean hasValidCompletionImage(String attachments) {
        if (!StringUtils.hasText(attachments) || attachments.length() > 1000) {
            return false;
        }
        String normalized = attachments.trim();
        return normalized.startsWith(COMPLETION_IMAGE_PREFIX)
                && normalized.endsWith("\"]")
                && normalized.indexOf("\",\"") < 0;
    }

    private String normalizeRequestId(ClientFarmTaskActionRequest request) {
        String requestId = normalizeText(request == null ? null : request.getRequestId());
        if (requestId != null && requestId.length() > 64) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "幂等请求号长度不能超过64个字符");
        }
        return requestId;
    }

    /** 幂等号只能复用于同一用户的同一任务，避免跨任务请求号碰撞导致动作被错误吞掉。 */
    private boolean isRequestProcessed(String requestId, Long taskId, Long operatorId) {
        if (requestId == null) {
            return false;
        }
        FarmTaskRecord existing = farmTaskRecordMapper.selectByRequestId(requestId);
        if (existing == null) {
            return false;
        }
        if (!taskId.equals(existing.getTaskId()) || !operatorId.equals(existing.getOperatorId())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "幂等请求号已被其他任务使用");
        }
        return true;
    }

    private String resolveUserName(User user) {
        if (user == null) {
            return null;
        }
        return firstNonBlank(user.getNickname(), user.getUsername());
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private ClientFarmTaskResponse getClientTask(Long id, Long currentUserId) {
        ClientFarmTaskResponse task = farmTaskMapper.selectClientTaskById(id, currentUserId);
        if (task == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "农事任务不存在");
        }
        return task;
    }

    private Long requireCurrentUserId() {
        User user = dataPermissionService.currentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED);
        }
        // 农事客户端仅向农场主及仍绑定农场的普通用户开放，防止其他角色复用旧绑定查询任务。
        dataPermissionService.currentClientOwnerId();
        return user.getId();
    }

    private Long readCount(Map<String, Object> statistics, String key) {
        if (statistics == null) {
            return 0L;
        }
        Object value = statistics.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private void validateClientTaskType(Integer taskType) {
        // farm 首页任务导航当前只展示四类生产任务，空值表示全部。
        if (taskType != null && (taskType < 1 || taskType > 4)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农事任务类型不正确");
        }
    }

    private void validateClientTaskStatus(Integer status) {
        if (status != null && (status < STATUS_PENDING || status > STATUS_OVERDUE_LEGACY)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农事任务状态不正确");
        }
    }

    private String toChineseWeekday(LocalDate date) {
        String[] weekdays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return weekdays[date.getDayOfWeek().getValue() - 1];
    }
}
