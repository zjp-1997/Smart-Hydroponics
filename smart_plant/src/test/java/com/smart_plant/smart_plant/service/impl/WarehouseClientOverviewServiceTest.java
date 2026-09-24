package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.smart_plant.smart_plant.dto.ClientWarehouseOverviewResponse;
import com.smart_plant.smart_plant.entity.WarehouseItem;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.mapper.FarmTaskMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.mapper.WarehouseItemMapper;
import com.smart_plant.smart_plant.mapper.WarehouseRecordMapper;
import com.smart_plant.smart_plant.mapper.IotDeviceFaultMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.OperationLogService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/** 验证 farm 仓库概览只返回已分配物资，农场主按本人归属查询。 */
class WarehouseClientOverviewServiceTest {

    @Test
    void scopesFarmOwnerOverviewToCurrentOwner() {
        WarehouseItemMapper itemMapper = mock(WarehouseItemMapper.class);
        WarehouseRecordMapper recordMapper = mock(WarehouseRecordMapper.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);
        User owner = new User();
        owner.setId(8L);
        owner.setRoleCode("farm_owner");
        when(permissionService.currentUser()).thenReturn(owner);
        when(permissionService.currentClientOwnerId()).thenReturn(8L);
        when(itemMapper.countClientItems(8L)).thenReturn(2L);
        WarehouseServiceImpl service = new WarehouseServiceImpl(
                itemMapper, recordMapper, mock(IotDeviceFaultMapper.class), mock(UserMapper.class),
                mock(PlotMapper.class), mock(FarmTaskMapper.class), permissionService,
                mock(OperationLogService.class));

        try {
            assertEquals(2L, service.getClientOverview(null, null, 1, 20).totalItems());
            verify(itemMapper).selectClientList(8L, null, null);
            verify(recordMapper).sumClientQuantityByTypeBetween(
                    org.mockito.ArgumentMatchers.eq(8L), org.mockito.ArgumentMatchers.eq(1), any(), any());
        } finally {
            PageHelper.clearPage();
        }
    }

    @Test
    void returnsSharedEnabledWarehouseItems() {
        WarehouseItemMapper itemMapper = mock(WarehouseItemMapper.class);
        WarehouseRecordMapper recordMapper = mock(WarehouseRecordMapper.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);
        User viewer = new User();
        viewer.setRoleCode("user");
        when(permissionService.currentClientOwnerId()).thenReturn(8L);
        WarehouseItem item = new WarehouseItem();
        item.setItemName("共享物资");
        when(itemMapper.selectClientList(8L, "共享", 2)).thenReturn(List.of(item));
        when(itemMapper.countClientItems(8L)).thenReturn(1L);
        when(itemMapper.sumClientStockQty(8L)).thenReturn(new BigDecimal("12"));
        // 今日入库由记录表按自然日实时汇总，测试值用于确认结果没有被静态常量替代。
        when(recordMapper.sumClientQuantityByTypeBetween(
                org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.eq(1),
                any(),
                any())).thenReturn(new BigDecimal("5"));

        WarehouseServiceImpl service = new WarehouseServiceImpl(
                itemMapper,
                recordMapper,
                mock(IotDeviceFaultMapper.class),
                mock(UserMapper.class),
                mock(PlotMapper.class),
                mock(FarmTaskMapper.class),
                permissionService,
                mock(OperationLogService.class));

        try {
            ClientWarehouseOverviewResponse result = service.getClientOverview("共享", 2, 1, 20);

            assertEquals("共享物资", result.items().getFirst().itemName());
            assertEquals(1L, result.totalItems());
            assertEquals(new BigDecimal("12"), result.stockQuantity());
            assertEquals(new BigDecimal("5"), result.todayInboundQuantity());
            verify(itemMapper).selectClientList(8L, "共享", 2);
        } finally {
            // Mockito 不会触发 MyBatis 分页插件，主动清理线程变量避免影响其他测试。
            PageHelper.clearPage();
        }
    }
}
