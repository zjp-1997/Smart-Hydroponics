package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.ClientFarmTaskResponse;
import com.smart_plant.smart_plant.dto.ClientFarmTaskStatisticsResponse;
import com.smart_plant.smart_plant.dto.ClientFarmTaskActionRequest;
import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.entity.FarmTask;
import com.smart_plant.smart_plant.entity.FarmTaskRecord;
import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.mapper.FarmTaskMapper;
import com.smart_plant.smart_plant.mapper.FarmTaskRecordMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.CropImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientFarmTaskServiceImplTest {

    @Mock
    private FarmTaskMapper farmTaskMapper;

    @Mock
    private FarmTaskRecordMapper farmTaskRecordMapper;

    @Mock
    private PlotMapper plotMapper;

    @Mock
    private DataPermissionService dataPermissionService;

    @Mock
    private CropImageService cropImageService;

    private ClientFarmTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClientFarmTaskServiceImpl(
                farmTaskMapper, farmTaskRecordMapper, plotMapper, dataPermissionService, cropImageService);
    }

    @Test
    void statisticsUsesCurrentUserScope() {
        User currentUser = new User();
        currentUser.setId(12L);
        when(dataPermissionService.currentUser()).thenReturn(currentUser);
        when(farmTaskMapper.selectClientStatistics(12L)).thenReturn(Map.of(
                "pendingCount", 3L,
                "runningCount", 2L,
                "completedCount", 7L,
                "overdueCount", 1L));

        ClientFarmTaskStatisticsResponse actual = service.statistics();

        assertEquals(3L, actual.getPendingCount());
        assertEquals(2L, actual.getRunningCount());
        assertEquals(7L, actual.getCompletedCount());
        assertEquals(1L, actual.getOverdueCount());
        verify(farmTaskMapper).selectClientStatistics(12L);
    }

    @Test
    void statisticsReturnsZerosWhenMapperReturnsNoRow() {
        User currentUser = new User();
        currentUser.setId(12L);
        when(dataPermissionService.currentUser()).thenReturn(currentUser);
        when(farmTaskMapper.selectClientStatistics(12L)).thenReturn(null);

        ClientFarmTaskStatisticsResponse actual = service.statistics();

        assertEquals(0L, actual.getPendingCount());
        assertEquals(0L, actual.getRunningCount());
        assertEquals(0L, actual.getCompletedCount());
        assertEquals(0L, actual.getOverdueCount());
    }

    @Test
    void myTasksUseCurrentUserAndStatusScope() {
        User currentUser = new User();
        currentUser.setId(12L);
        ClientFarmTaskResponse expected = new ClientFarmTaskResponse();
        expected.setId(88L);
        when(dataPermissionService.currentUser()).thenReturn(currentUser);
        when(farmTaskMapper.selectClientTasksByStatus(12L, 4)).thenReturn(List.of(expected));

        List<ClientFarmTaskResponse> actual = service.listMyTasks(4);

        assertEquals(List.of(expected), actual);
        verify(farmTaskMapper).selectClientTasksByStatus(12L, 4);
    }

    @Test
    void myTasksRejectRolesOutsideBoundFarmWorkspace() {
        // 专家等角色即使持有旧绑定关系，也不能通过农事列表读取农场主任务。
        User expert = new User();
        expert.setId(12L);
        expert.setRoleCode("expert");
        when(dataPermissionService.currentUser()).thenReturn(expert);
        when(dataPermissionService.currentClientOwnerId())
                .thenThrow(new BusinessException(ResponseCode.FORBIDDEN));

        assertThrows(BusinessException.class, () -> service.listMyTasks(null));
        verifyNoInteractions(farmTaskMapper);
    }

    @Test
    void myTasksRejectUnsupportedStatus() {
        assertThrows(BusinessException.class, () -> service.listMyTasks(5));
    }

    @Test
    void plotTaskTimelineUsesCurrentUserScope() {
        User currentUser = new User();
        currentUser.setId(12L);
        ClientFarmTaskResponse expected = new ClientFarmTaskResponse();
        expected.setId(88L);
        when(dataPermissionService.currentUser()).thenReturn(currentUser);
        when(farmTaskMapper.selectClientTaskViewsByPlotId(12L, 9L)).thenReturn(List.of(expected));

        List<ClientFarmTaskResponse> actual = service.listTasksByPlot(9L);

        assertEquals(List.of(expected), actual);
        // 查询条件必须同时包含登录用户和地块，保证地块参数不可用于越权读取。
        verify(farmTaskMapper).selectClientTaskViewsByPlotId(12L, 9L);
    }

    @Test
    void taskDetailChecksOwnershipBeforeReturningClientView() {
        User currentUser = new User();
        currentUser.setId(12L);
        when(dataPermissionService.currentUser()).thenReturn(currentUser);
        FarmTask ownedTask = new FarmTask();
        ownedTask.setId(88L);
        ownedTask.setUserId(12L);
        ClientFarmTaskResponse expected = new ClientFarmTaskResponse();
        expected.setId(88L);
        expected.setTaskTitle("灌溉任务");
        when(farmTaskMapper.selectById(88L)).thenReturn(ownedTask);
        when(farmTaskMapper.selectClientTaskById(88L, 12L)).thenReturn(expected);

        ClientFarmTaskResponse actual = service.getTaskDetail(88L);

        assertEquals("灌溉任务", actual.getTaskTitle());
        // 先执行统一数据权限校验，再使用任务所属用户作为 SQL 查询边界。
        verify(dataPermissionService).requireAgriculturalOperator(12L);
        verify(farmTaskMapper).selectClientTaskById(88L, 12L);
    }

    @Test
    void assignedWorkerCanReadTaskFromBoundFarm() {
        User worker = new User();
        worker.setId(22L);
        when(dataPermissionService.currentUser()).thenReturn(worker);
        FarmTask assigned = new FarmTask();
        assigned.setId(88L);
        assigned.setUserId(8L);
        assigned.setExecutorId(22L);
        when(farmTaskMapper.selectById(88L)).thenReturn(assigned);
        ClientFarmTaskResponse expected = new ClientFarmTaskResponse();
        expected.setId(88L);
        when(farmTaskMapper.selectClientTaskById(88L, 8L)).thenReturn(expected);

        assertSame(expected, service.getTaskDetail(88L));
        verify(dataPermissionService).requireClientFarmReader(8L);
    }

    @Test
    void unassignedWorkerCannotReadTask() {
        User worker = new User();
        worker.setId(22L);
        when(dataPermissionService.currentUser()).thenReturn(worker);
        FarmTask otherTask = new FarmTask();
        otherTask.setId(88L);
        otherTask.setUserId(8L);
        otherTask.setExecutorId(23L);
        when(farmTaskMapper.selectById(88L)).thenReturn(otherTask);

        assertThrows(BusinessException.class, () -> service.getTaskDetail(88L));
    }

    @Test
    void plotTimelineIncludesCreatedStateAndSortsAllOperatorsChronologically() {
        User currentUser = new User();
        currentUser.setId(12L);
        when(dataPermissionService.currentUser()).thenReturn(currentUser);
        Plot plot = new Plot();
        plot.setId(9L);
        plot.setUserId(12L);
        FarmTask task = new FarmTask();
        task.setId(88L);
        task.setUserId(12L);
        task.setPlotId(9L);
        task.setTaskTitle("浇水");
        task.setCreateTime(LocalDateTime.of(2026, 9, 9, 8, 36));
        FarmTaskRecord action = new FarmTaskRecord();
        action.setId(5L);
        action.setTaskId(88L);
        action.setOperatorName("张三");
        action.setExecuteTime(LocalDateTime.of(2026, 9, 9, 8, 39));
        when(plotMapper.selectById(9L)).thenReturn(plot);
        when(farmTaskMapper.selectClientTasksByPlotId(12L, 9L)).thenReturn(List.of(task));
        when(farmTaskRecordMapper.selectListByPlotId(12L, 9L)).thenReturn(List.of(action));

        List<FarmTaskRecord> timeline = service.listPlotTimeline(9L);

        assertEquals(List.of(-88L, 5L), timeline.stream().map(FarmTaskRecord::getId).toList());
        assertEquals("系统记录", timeline.get(0).getOperatorName());
        assertEquals("张三", timeline.get(1).getOperatorName());
        verify(dataPermissionService).requireClientFarmReader(12L);
    }

    @Test
    void completionImageUploadRequiresRunningOwnedTask() {
        User currentUser = new User();
        currentUser.setId(12L);
        when(dataPermissionService.currentUser()).thenReturn(currentUser);
        FarmTask runningTask = new FarmTask();
        runningTask.setId(88L);
        runningTask.setUserId(12L);
        runningTask.setStatus(2);
        MockMultipartFile image = new MockMultipartFile("image", "done.jpg", "image/jpeg", new byte[]{1});
        CropImageUploadResult expected = new CropImageUploadResult("/uploads/task-completion-images/2026-09-08/done.jpg", 1L);
        when(farmTaskMapper.selectById(88L)).thenReturn(runningTask);
        when(cropImageService.uploadTaskCompletionImage(image)).thenReturn(expected);

        CropImageUploadResult actual = service.uploadCompletionImage(88L, image);

        assertSame(expected, actual);
        verify(dataPermissionService).requireAgriculturalOperator(12L);
    }

    @Test
    void completionRejectsRequestWithoutUploadedEvidence() {
        FarmTask runningTask = new FarmTask();
        runningTask.setId(88L);
        runningTask.setUserId(12L);
        runningTask.setStatus(2);
        User currentUser = new User();
        currentUser.setId(12L);
        ClientFarmTaskActionRequest request = new ClientFarmTaskActionRequest();
        request.setFeedbackDetail("任务已经完成");
        when(farmTaskMapper.selectById(88L)).thenReturn(runningTask);
        when(dataPermissionService.currentUser()).thenReturn(currentUser);

        // 即使有完成说明，没有上传凭证也不能将任务改为已完成。
        assertThrows(BusinessException.class, () -> service.completeTask(88L, request));
    }

    @Test
    void ownerCanStartAssignedTaskWithoutOverwritingExecutor() {
        User owner = new User();
        owner.setId(12L);
        FarmTask assignedTask = new FarmTask();
        assignedTask.setId(88L);
        assignedTask.setUserId(12L);
        assignedTask.setExecutorId(22L);
        assignedTask.setStatus(1);
        ClientFarmTaskResponse runningTask = new ClientFarmTaskResponse();
        runningTask.setId(88L);
        runningTask.setStatus(2);
        when(dataPermissionService.currentUser()).thenReturn(owner);
        when(farmTaskMapper.selectById(88L)).thenReturn(assignedTask);
        when(farmTaskMapper.startTask(eq(88L), eq(22L), any(LocalDateTime.class))).thenReturn(1);
        when(farmTaskRecordMapper.insert(any(FarmTaskRecord.class))).thenReturn(1);
        when(farmTaskMapper.selectClientTaskById(88L, 12L)).thenReturn(runningTask);

        ClientFarmTaskResponse actual = service.executeTask(88L, new ClientFarmTaskActionRequest());

        assertSame(runningTask, actual);
        verify(farmTaskMapper).startTask(eq(88L), eq(22L), any(LocalDateTime.class));
    }
}
