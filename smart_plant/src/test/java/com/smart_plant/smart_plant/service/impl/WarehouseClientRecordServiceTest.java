package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.smart_plant.smart_plant.dto.ClientWarehouseRecordPage;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.entity.WarehouseRecord;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.FarmTaskMapper;
import com.smart_plant.smart_plant.mapper.IotDeviceFaultMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.mapper.WarehouseItemMapper;
import com.smart_plant.smart_plant.mapper.WarehouseRecordMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.OperationLogService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证 farm 仓库流水的角色和物资归属限制。 */
class WarehouseClientRecordServiceTest {

    private final WarehouseRecordMapper recordMapper = mock(WarehouseRecordMapper.class);
    private final DataPermissionService permissions = mock(DataPermissionService.class);
    private final WarehouseServiceImpl service = new WarehouseServiceImpl(
            mock(WarehouseItemMapper.class), recordMapper, mock(IotDeviceFaultMapper.class),
            mock(UserMapper.class), mock(PlotMapper.class), mock(FarmTaskMapper.class),
            permissions, mock(OperationLogService.class));

    @Test
    void farmOwnerQueriesOnlyOwnInboundRecords() {
        User owner = user(8L, "farm_owner");
        when(permissions.currentUser()).thenReturn(owner);
        when(permissions.currentClientOwnerId()).thenReturn(8L);
        WarehouseRecord inbound = new WarehouseRecord();
        inbound.setId(21L);
        inbound.setItemName("营养液");
        inbound.setQuantity(new BigDecimal("3"));
        inbound.setPrice(new BigDecimal("99"));
        when(recordMapper.selectClientList(8L, 1)).thenReturn(List.of(inbound));
        try {
            ClientWarehouseRecordPage page = service.listClientRecords(1, 1, 10);
            verify(recordMapper).selectClientList(8L, 1);
            // farm 专用响应只包含展示字段，不把入库单价返回给普通页面。
            org.junit.jupiter.api.Assertions.assertEquals("营养液", page.list().getFirst().itemName());
            org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("3"), page.list().getFirst().quantity());
        } finally {
            // Mockito 查询不经过分页拦截器，清除 PageHelper 线程变量。
            PageHelper.clearPage();
        }
    }

    @Test
    void ordinaryUserUsesAssignedSharedView() {
        when(permissions.currentClientOwnerId()).thenReturn(8L);
        when(recordMapper.selectClientList(8L, 2)).thenReturn(List.of());
        try {
            service.listClientRecords(2, 1, 10);
            verify(recordMapper).selectClientList(8L, 2);
        } finally {
            PageHelper.clearPage();
        }
    }

    @Test
    void expertCannotReadWarehouseRecords() {
        when(permissions.currentClientOwnerId()).thenThrow(new BusinessException(
                com.smart_plant.smart_plant.response.ResponseCode.FORBIDDEN, "当前角色无权访问农场"));
        assertThrows(BusinessException.class, () -> service.listClientRecords(null, 1, 10));
        verify(recordMapper, never()).selectClientList(null, null);
    }

    /** 只设置权限判断实际使用的用户字段，避免测试依赖完整用户资料。 */
    private User user(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setRoleCode(role);
        return user;
    }
}
