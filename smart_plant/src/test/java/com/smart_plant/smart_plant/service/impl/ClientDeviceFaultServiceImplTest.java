package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ClientDeviceFaultResponse;
import com.smart_plant.smart_plant.dto.ClientDeviceFaultRecordResponse;
import com.smart_plant.smart_plant.entity.IotDeviceFault;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.IotDeviceFaultMapper;
import com.smart_plant.smart_plant.service.CropImageService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.IotDeviceFaultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 farm 移动端故障服务的动作权限映射和状态机委托行为。
 */
class ClientDeviceFaultServiceImplTest {

    private IotDeviceFaultService faultService;
    private DataPermissionService dataPermissionService;
    private IotDeviceFaultMapper faultMapper;
    private ClientDeviceFaultServiceImpl service;

    @BeforeEach
    void setUp() {
        faultService = mock(IotDeviceFaultService.class);
        dataPermissionService = mock(DataPermissionService.class);
        faultMapper = mock(IotDeviceFaultMapper.class);
        service = new ClientDeviceFaultServiceImpl(
                faultService, dataPermissionService, mock(CropImageService.class), faultMapper);

        User technician = new User();
        technician.setId(8L);
        when(dataPermissionService.currentUser()).thenReturn(technician);
        when(dataPermissionService.isTechnician()).thenReturn(true);
    }

    @Test
    void exposesAcceptActionForPendingAssignment() {
        IotDeviceFault fault = assignedFault(0, 1);
        when(faultService.listFaults(null, null, null, null, null, null, 0, null, 1, 500))
                .thenReturn(new PageInfo<>(List.of(fault)));

        ClientDeviceFaultResponse response = service.listFaults(0).getFirst();

        assertTrue(response.getCanAccept());
        assertFalse(response.getCanComplete());
    }

    @Test
    void technicianHomeContainsAllAssignmentsOrderedByStatusAndTime() {
        IotDeviceFault pending = assignedFault(0, 1);
        pending.setStartTime(LocalDateTime.of(2026, 9, 20, 8, 0));
        IotDeviceFault newerPending = assignedFault(0, 1);
        newerPending.setId(8L);
        newerPending.setStartTime(LocalDateTime.of(2026, 9, 21, 9, 0));
        IotDeviceFault processing = assignedFault(1, 2);
        processing.setId(9L);
        processing.setStartTime(LocalDateTime.of(2026, 9, 21, 8, 0));
        IotDeviceFault completed = assignedFault(2, 2);
        completed.setId(10L);
        completed.setStartTime(LocalDateTime.of(2026, 9, 21, 10, 0));
        when(faultService.listFaults(null, null, null, null, null, null, null, null, 1, 500))
                .thenReturn(new PageInfo<>(List.of(completed, processing, pending, newerPending)));

        List<ClientDeviceFaultResponse> result = service.listTechnicianFaults();

        assertEquals(List.of(8L, 7L, 9L, 10L), result.stream().map(ClientDeviceFaultResponse::getId).toList());
        verify(faultService).listFaults(null, null, null, null, null, null, null, null, 1, 500);
    }

    @Test
    void technicianHomeRejectsOtherRoles() {
        when(dataPermissionService.isTechnician()).thenReturn(false);

        assertThrows(BusinessException.class, service::listTechnicianFaults);
        verify(faultService, never()).listFaults(null, null, null, null, null, null, null, null, 1, 500);
    }

    @Test
    void exposesCompletionOnlyForAcceptedProcessingFault() {
        IotDeviceFault fault = assignedFault(1, 2);
        when(faultService.listFaults(null, null, null, null, null, null, 1, null, 1, 500))
                .thenReturn(new PageInfo<>(List.of(fault)));

        ClientDeviceFaultResponse response = service.listFaults(1).getFirst();

        // 页面只给真正处于处理中且已经接单的处理人展示完成按钮。
        assertTrue(response.getCanComplete());
        assertFalse(response.getCanAccept());
    }

    @Test
    void rejectsCompletionWithoutUploadedImage() {
        when(faultService.getFaultById(7L)).thenReturn(assignedFault(1, 2));

        // 图片是完成故障的必填凭证，缺失时不能调用状态更新入口。
        assertThrows(BusinessException.class, () -> service.completeFault(7L, "", null));
        verify(faultService, never()).updateStatus(7L, 2, "已上传现场图片，故障处理完成");
    }

    @Test
    void ownerCanStartOwnedPendingFault() {
        User owner = new User();
        owner.setId(9L);
        owner.setRoleCode("farm_owner");
        when(dataPermissionService.currentUser()).thenReturn(owner);
        when(dataPermissionService.isTechnician()).thenReturn(false);
        when(faultMapper.startOwnedFault(7L, 9L)).thenReturn(1);
        IotDeviceFault fault = assignedFault(0, 1);
        fault.setDeviceOwnerId(9L);
        when(faultService.getFaultById(7L)).thenReturn(fault);

        // 农场主点击处理故障时，应通过归属校验并原子切换为处理中。
        service.acceptFault(7L);

        verify(faultMapper).startOwnedFault(7L, 9L);
    }

    @Test
    void recordShowsCompletionEvidenceWithoutInventingOldAcceptanceTime() {
        IotDeviceFault fault = assignedFault(2, 2);
        LocalDateTime reportedAt = LocalDateTime.of(2026, 9, 19, 8, 0);
        LocalDateTime completedAt = reportedAt.plusHours(2);
        fault.setStartTime(reportedAt);
        // 旧版完成操作会覆盖接单时间；维护记录应明确标为时间未知。
        fault.setHandleTime(completedAt);
        fault.setEndTime(completedAt);
        fault.setHandleResult("更换了滤芯");
        fault.setCompletionImageUrl("/uploads/fault-completion-images/example.jpg");
        when(faultService.getFaultById(7L)).thenReturn(fault);

        ClientDeviceFaultRecordResponse response = service.getFaultRecord(7L);

        assertEquals(3, response.records().size());
        assertEquals(reportedAt, response.records().getFirst().executeTime());
        assertNull(response.records().get(1).executeTime());
        assertEquals("更换了滤芯", response.records().get(2).content());
        assertEquals(fault.getCompletionImageUrl(), response.records().get(2).imageUrl());
        verify(faultService).getFaultById(7L);
    }

    private IotDeviceFault assignedFault(int status, int assignStatus) {
        IotDeviceFault fault = new IotDeviceFault();
        fault.setId(7L);
        fault.setStatus(status);
        fault.setAssignStatus(assignStatus);
        fault.setHandleUserId(8L);
        return fault;
    }
}
