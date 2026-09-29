package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.FarmTask;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.mapper.FarmTaskMapper;
import com.smart_plant.smart_plant.mapper.FarmTaskRecordMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FarmTaskServiceImplTest {

    @Test
    void deleteCancelsTaskAndKeepsItsAuditTrail() {
        FarmTaskMapper taskMapper = mock(FarmTaskMapper.class);
        FarmTaskRecordMapper recordMapper = mock(FarmTaskRecordMapper.class);
        PlotMapper plotMapper = mock(PlotMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);
        FarmTaskAuditService auditService = mock(FarmTaskAuditService.class);
        FarmTaskServiceImpl service = new FarmTaskServiceImpl(
                taskMapper, recordMapper, plotMapper, userMapper, permissionService, auditService);
        FarmTask task = new FarmTask();
        task.setId(88L);
        task.setUserId(12L);
        task.setTaskTitle("浇水");
        task.setStatus(1);
        User operator = new User();
        operator.setId(12L);
        when(taskMapper.selectById(88L)).thenReturn(task);
        when(taskMapper.cancelTask(88L)).thenReturn(1);
        when(permissionService.currentUser()).thenReturn(operator);

        service.deleteFarmTask(88L);

        verify(taskMapper).cancelTask(88L);
        verify(auditService).record(eq(task), eq(operator), eq(FarmTaskAuditService.ACTION_CANCEL),
                eq("取消农事任务：浇水"), eq(1), eq(5), eq("SMART_FARM"));
    }
}
