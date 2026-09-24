package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientWarehouseOverviewResponse;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.WarehouseService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 验证 farm 仓库首页接口会正确聚合统计信息和启用物资列表。 */
class ClientWarehouseControllerTest {

    private final WarehouseService warehouseService = mock(WarehouseService.class);
    private final ClientWarehouseController controller = new ClientWarehouseController(warehouseService);

    /** 聚合响应必须完整保留库存、种类、今日入库和物资数据。 */
    @Test
    void returnsWarehouseOverview() {
        ClientWarehouseOverviewResponse.Item item = new ClientWarehouseOverviewResponse.Item(
                7L, 8L, "农场主", "营养液", "F001", null, 2, "瓶装", "瓶",
                new BigDecimal("18.5"), BigDecimal.ONE, null);
        when(warehouseService.getClientOverview(null, null, null, null)).thenReturn(new ClientWarehouseOverviewResponse(
                3L, new BigDecimal("18.50"), new BigDecimal("4.00"), List.of(item), false));

        R<ClientWarehouseOverviewResponse> result = controller.getOverview(null, null, null, null);

        assertEquals(3L, result.getData().totalItems());
        assertEquals(new BigDecimal("18.50"), result.getData().stockQuantity());
        assertEquals(new BigDecimal("4.00"), result.getData().todayInboundQuantity());
        assertEquals("营养液", result.getData().items().getFirst().itemName());
    }
}
