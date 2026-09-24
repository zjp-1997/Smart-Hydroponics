package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.IotDevice;
import com.smart_plant.smart_plant.entity.IotDeviceFault;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.IotDeviceFaultMapper;
import com.smart_plant.smart_plant.mapper.IotDeviceMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.MaintenanceMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IotDeviceFaultLifecycleServiceTest {

    private IotDeviceFaultMapper faultMapper;
    private IotDeviceMapper deviceMapper;
    private UserMapper userMapper;
    private IotDeviceFaultServiceImpl service;

    @BeforeEach
    void setUp() {
        faultMapper = mock(IotDeviceFaultMapper.class);
        userMapper = mock(UserMapper.class);
        deviceMapper = mock(IotDeviceMapper.class);
        IotDevice device = new IotDevice();
        device.setId(9L);
        device.setUserId(1L);
        when(deviceMapper.selectById(9L)).thenReturn(device);
        service = new IotDeviceFaultServiceImpl(
                faultMapper,
                deviceMapper,
                userMapper,
                mock(DataPermissionService.class),
                mock(MaintenanceMessageService.class));
    }

    @Test
    void rejectsCompletionWithoutResult() {
        when(faultMapper.selectById(7L)).thenReturn(fault(1, 2, 10L, null));

        assertThrows(BusinessException.class, () -> service.updateStatus(7L, 2, " "));

        verify(faultMapper, never()).updateStatus(7L, 2, null);
    }

    @Test
    void completesAcceptedFaultWithResult() {
        when(faultMapper.selectById(7L)).thenReturn(fault(1, 2, 10L, null));
        when(faultMapper.updateStatus(7L, 2, "更换传感器后恢复正常")).thenReturn(1);
        when(deviceMapper.restoreOnlineAfterFaultResolved(9L)).thenReturn(1);

        service.updateStatus(7L, 2, "  更换传感器后恢复正常  ");

        verify(faultMapper).updateStatus(7L, 2, "更换传感器后恢复正常");
        verify(deviceMapper).restoreOnlineAfterFaultResolved(9L);
    }

    @Test
    void keepsDeviceOfflineWhenAnotherFaultIsStillOpen() {
        when(faultMapper.selectById(7L)).thenReturn(fault(1, 2, 10L, null));
        when(faultMapper.updateStatus(7L, 2, "已修复")).thenReturn(1);
        when(deviceMapper.restoreOnlineAfterFaultResolved(9L)).thenReturn(0);
        when(faultMapper.selectOpenByDeviceId(9L)).thenReturn(fault(0, 0, null, null));

        service.updateStatus(7L, 2, "已修复");

        verify(deviceMapper).restoreOnlineAfterFaultResolved(9L);
    }

    @Test
    void rejectsCompletionWhenResolvedDeviceCannotBeRestored() {
        when(faultMapper.selectById(7L)).thenReturn(fault(1, 2, 10L, null));
        when(faultMapper.updateStatus(7L, 2, "已修复")).thenReturn(1);
        when(deviceMapper.restoreOnlineAfterFaultResolved(9L)).thenReturn(0);
        when(faultMapper.selectOpenByDeviceId(9L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.updateStatus(7L, 2, "已修复"));
    }

    @Test
    void restoresDeviceWhenGenericUpdateCompletesFault() {
        IotDeviceFault oldFault = fault(1, 2, 10L, null);
        IotDeviceFault request = fault(2, 2, null, "已修复");
        when(faultMapper.selectById(7L)).thenReturn(oldFault);
        when(faultMapper.updateById(request)).thenReturn(1);
        when(deviceMapper.restoreOnlineAfterFaultResolved(9L)).thenReturn(1);

        service.updateFault(request);

        verify(deviceMapper).restoreOnlineAfterFaultResolved(9L);
    }

    @Test
    void onlyHandledFaultCanBeClosed() {
        when(faultMapper.selectById(7L)).thenReturn(fault(1, 2, 10L, "已修复"));

        assertThrows(BusinessException.class, () -> service.updateStatus(7L, 3, null));

        verify(faultMapper, never()).updateStatus(7L, 3, null);
    }

    @Test
    void closesHandledFaultAndPreventsReopening() {
        when(faultMapper.selectById(7L)).thenReturn(fault(2, 2, 10L, "已修复"));
        when(faultMapper.updateStatus(7L, 3, null)).thenReturn(1);

        service.updateStatus(7L, 3, null);

        verify(faultMapper).updateStatus(7L, 3, null);
        verify(deviceMapper, never()).restoreOnlineAfterFaultResolved(9L);
        assertThrows(BusinessException.class, () -> service.updateStatus(7L, 0, null));
    }

    @Test
    void acceptsEveryFaultClassificationExposedByAdminForm() {
        IotDeviceFault request = new IotDeviceFault();
        request.setDeviceId(9L);
        request.setFaultCode("FAULT-6");
        request.setFaultName("其他设备故障");
        request.setFaultType(6);
        request.setSeverity(4);
        // 本用例只验证分类边界，不模拟自动分配技术人员。
        when(userMapper.selectBoundTechnicianIdByOwnerId(1L)).thenReturn(null);
        doAnswer(invocation -> {
            ((IotDeviceFault) invocation.getArgument(0)).setId(20L);
            return 1;
        }).when(faultMapper).insert(any(IotDeviceFault.class));
        when(faultMapper.selectById(20L)).thenReturn(request);

        // 类型6和严重程度4来自 smart_farm 下拉框，服务端必须在落库前认可这两个边界值。
        service.addFault(request);

        verify(faultMapper).insert(request);
    }

    @Test
    void rejectsClassificationOutsideDatabaseConstraintBeforeInsert() {
        IotDeviceFault request = new IotDeviceFault();
        request.setDeviceId(9L);
        request.setFaultCode("FAULT-7");
        request.setFaultName("非法故障类型");
        request.setFaultType(7);
        request.setSeverity(1);

        assertThrows(BusinessException.class, () -> service.addFault(request));
        verify(faultMapper, never()).insert(any(IotDeviceFault.class));
    }

    private IotDeviceFault fault(int status, int assignStatus, Long handlerId, String result) {
        IotDeviceFault fault = new IotDeviceFault();
        fault.setId(7L);
        fault.setDeviceId(9L);
        fault.setStatus(status);
        fault.setAssignStatus(assignStatus);
        fault.setHandleUserId(handlerId);
        fault.setHandleResult(result);
        return fault;
    }
}
