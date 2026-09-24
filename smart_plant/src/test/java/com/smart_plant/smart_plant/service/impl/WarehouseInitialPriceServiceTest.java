package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.entity.WarehouseItem;
import com.smart_plant.smart_plant.entity.WarehouseRecord;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.FarmTaskMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.mapper.WarehouseItemMapper;
import com.smart_plant.smart_plant.mapper.WarehouseRecordMapper;
import com.smart_plant.smart_plant.mapper.IotDeviceFaultMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.OperationLogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WarehouseInitialPriceServiceTest {

    @Test
    void writesInitialUnitPriceAndAmountToFirstInboundRecord() {
        WarehouseItemMapper itemMapper = mock(WarehouseItemMapper.class);
        WarehouseRecordMapper recordMapper = mock(WarehouseRecordMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        DataPermissionService dataPermissionService = mock(DataPermissionService.class);
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRoleCode("farm_owner");
        User operator = new User();
        operator.setId(2L);
        operator.setStatus(1);
        User owner = new User();
        owner.setId(1L);
        owner.setStatus(1);
        owner.setRoleCode("farm_owner");
        when(dataPermissionService.currentUser()).thenReturn(currentUser);
        when(userMapper.selectById(1L)).thenReturn(owner);
        when(userMapper.selectById(2L)).thenReturn(operator);
        doAnswer(invocation -> {
            invocation.<WarehouseItem>getArgument(0).setId(7L);
            return 1;
        }).when(itemMapper).insert(any(WarehouseItem.class));
        when(itemMapper.updateGeneratedItemCode(7L, "DF_007")).thenReturn(1);
        when(itemMapper.selectById(7L)).thenAnswer(invocation -> item());
        WarehouseServiceImpl service = new WarehouseServiceImpl(
                itemMapper,
                recordMapper,
                mock(IotDeviceFaultMapper.class),
                userMapper,
                mock(PlotMapper.class),
                mock(FarmTaskMapper.class),
                dataPermissionService,
                mock(OperationLogService.class));

        service.addItem(item());

        ArgumentCaptor<WarehouseRecord> recordCaptor = ArgumentCaptor.forClass(WarehouseRecord.class);
        verify(recordMapper).insert(recordCaptor.capture());
        assertEquals(1L, recordCaptor.getValue().getUserId());
        assertEquals(new BigDecimal("12.34"), recordCaptor.getValue().getPrice());
        assertEquals(new BigDecimal("129.57"), recordCaptor.getValue().getTotalAmount());
    }

    @Test
    void generatesItemCodeFromDatabaseIdAndIgnoresClientCode() {
        WarehouseItemMapper itemMapper = mock(WarehouseItemMapper.class);
        WarehouseRecordMapper recordMapper = mock(WarehouseRecordMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        DataPermissionService permissions = mock(DataPermissionService.class);
        User owner = new User();
        owner.setId(1L);
        owner.setStatus(1);
        owner.setRoleCode("farm_owner");
        User operator = new User();
        operator.setId(2L);
        operator.setStatus(1);
        when(permissions.currentUser()).thenReturn(owner);
        when(userMapper.selectById(1L)).thenReturn(owner);
        when(userMapper.selectById(2L)).thenReturn(operator);
        doAnswer(invocation -> {
            invocation.<WarehouseItem>getArgument(0).setId(12L);
            return 1;
        }).when(itemMapper).insert(any(WarehouseItem.class));
        when(itemMapper.updateGeneratedItemCode(12L, "DF_012")).thenReturn(1);
        WarehouseItem request = item();
        request.setItemCode("USER_INPUT");
        when(itemMapper.selectById(12L)).thenReturn(request);
        WarehouseServiceImpl service = new WarehouseServiceImpl(
                itemMapper, recordMapper, mock(IotDeviceFaultMapper.class), userMapper,
                mock(PlotMapper.class), mock(FarmTaskMapper.class), permissions,
                mock(OperationLogService.class));

        service.addItem(request);

        // 服务端丢弃客户端编码，并使用新增记录的唯一主键生成至少三位编号。
        assertEquals("DF_012", request.getItemCode());
        verify(itemMapper).updateGeneratedItemCode(12L, "DF_012");
    }

    @Test
    void updatesInitialInboundPriceWhenEditingItem() {
        WarehouseItemMapper itemMapper = mock(WarehouseItemMapper.class);
        WarehouseRecordMapper recordMapper = mock(WarehouseRecordMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        DataPermissionService dataPermissionService = mock(DataPermissionService.class);
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRoleCode("farm_owner");
        User operator = new User();
        operator.setStatus(1);
        User owner = new User();
        owner.setId(1L);
        owner.setStatus(1);
        owner.setRoleCode("farm_owner");
        WarehouseItem item = item();
        item.setId(7L);
        item.setUserId(1L);
        item.setInitialUnitPrice(new BigDecimal("15.20"));
        when(dataPermissionService.currentUser()).thenReturn(currentUser);
        when(itemMapper.selectById(7L)).thenReturn(item);
        when(itemMapper.updateById(item)).thenReturn(1);
        when(userMapper.selectById(2L)).thenReturn(operator);
        when(userMapper.selectById(1L)).thenReturn(owner);
        WarehouseServiceImpl service = new WarehouseServiceImpl(
                itemMapper,
                recordMapper,
                mock(IotDeviceFaultMapper.class),
                userMapper,
                mock(PlotMapper.class),
                mock(FarmTaskMapper.class),
                dataPermissionService,
                mock(OperationLogService.class));

        service.updateItem(item);

        verify(recordMapper).updateInitialInbound(7L, 2L, new BigDecimal("15.20"));
    }

    @Test
    void assignsLegacyAdminItemToFarmOwnerAndUpdatesRecordScope() {
        WarehouseItemMapper itemMapper = mock(WarehouseItemMapper.class);
        WarehouseRecordMapper recordMapper = mock(WarehouseRecordMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);
        WarehouseItem oldItem = item();
        oldItem.setId(7L);
        oldItem.setUserId(1L);
        oldItem.setFarmOwnerId(null);
        WarehouseItem update = item();
        update.setId(7L);
        update.setFarmOwnerId(3L);
        User owner = new User();
        owner.setId(3L);
        owner.setStatus(1);
        owner.setRoleCode("farm_owner");
        User operator = new User();
        operator.setId(2L);
        operator.setStatus(1);
        User admin = new User();
        admin.setId(1L);
        admin.setRoleCode("admin");
        when(permissionService.currentUser()).thenReturn(admin);
        when(permissionService.isAdmin()).thenReturn(true);
        when(itemMapper.selectById(7L)).thenReturn(oldItem);
        when(itemMapper.updateById(update)).thenReturn(1);
        when(userMapper.selectById(3L)).thenReturn(owner);
        when(userMapper.selectById(2L)).thenReturn(operator);
        WarehouseServiceImpl service = new WarehouseServiceImpl(
                itemMapper, recordMapper, mock(IotDeviceFaultMapper.class), userMapper,
                mock(PlotMapper.class), mock(FarmTaskMapper.class), permissionService,
                mock(OperationLogService.class));

        service.updateItem(update);

        assertEquals(1L, update.getUserId());
        verify(recordMapper).updateOwnerByItemId(7L, 3L);
    }

    @Test
    void rejectsItemWithoutImage() {
        DataPermissionService dataPermissionService = mock(DataPermissionService.class);
        User currentUser = new User();
        currentUser.setId(1L);
        when(dataPermissionService.currentUser()).thenReturn(currentUser);
        WarehouseServiceImpl service = new WarehouseServiceImpl(
                mock(WarehouseItemMapper.class),
                mock(WarehouseRecordMapper.class),
                mock(IotDeviceFaultMapper.class),
                mock(UserMapper.class),
                mock(PlotMapper.class),
                mock(FarmTaskMapper.class),
                dataPermissionService,
                mock(OperationLogService.class));
        WarehouseItem item = item();
        item.setImageUrl(" ");

        assertThrows(BusinessException.class, () -> service.addItem(item));
    }

    @Test
    void logicallyDeletesItemWithoutDeletingInboundAndOutboundHistory() {
        WarehouseItemMapper itemMapper = mock(WarehouseItemMapper.class);
        WarehouseRecordMapper recordMapper = mock(WarehouseRecordMapper.class);
        DataPermissionService dataPermissionService = mock(DataPermissionService.class);
        WarehouseItem storedItem = item();
        storedItem.setId(7L);
        storedItem.setUserId(1L);
        when(itemMapper.selectById(7L)).thenReturn(storedItem);
        when(itemMapper.softDeleteById(7L)).thenReturn(1);
        WarehouseServiceImpl service = new WarehouseServiceImpl(
                itemMapper,
                recordMapper,
                mock(IotDeviceFaultMapper.class),
                mock(UserMapper.class),
                mock(PlotMapper.class),
                mock(FarmTaskMapper.class),
                dataPermissionService,
                mock(OperationLogService.class));

        service.deleteItem(7L);

        verify(itemMapper).softDeleteById(7L);
        verifyNoInteractions(recordMapper);
    }

    private WarehouseItem item() {
        WarehouseItem item = new WarehouseItem();
        item.setItemName("水泵滤芯");
        item.setCategory(5);
        item.setUnit("个");
        item.setImageUrl("/uploads/warehouse-images/filter.jpg");
        item.setStockQty(new BigDecimal("10.50"));
        item.setInitialUnitPrice(new BigDecimal("12.34"));
        item.setWarningQty(BigDecimal.ZERO);
        item.setInboundOperatorId(2L);
        item.setFarmOwnerId(1L);
        item.setStatus(1);
        return item;
    }
}
