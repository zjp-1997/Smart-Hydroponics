package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.entity.WarehouseItem;
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

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WarehouseOutboundFaultServiceTest {

    @Test
    void requiresOwnedDeviceFaultAndPersistsItsReference() {
        WarehouseItemMapper itemMapper = mock(WarehouseItemMapper.class);
        WarehouseRecordMapper recordMapper = mock(WarehouseRecordMapper.class);
        IotDeviceFaultMapper faultMapper = mock(IotDeviceFaultMapper.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);
        WarehouseItem item = new WarehouseItem();
        item.setId(7L);
        item.setUserId(1L);
        item.setFarmOwnerId(1L);
        item.setItemName("水泵滤芯");
        item.setStockQty(new BigDecimal("10"));
        item.setStatus(1);
        when(itemMapper.selectByIdForUpdate(7L)).thenReturn(item);
        when(itemMapper.updateStock(7L, new BigDecimal("9"))).thenReturn(1);
        when(recordMapper.insert(org.mockito.ArgumentMatchers.any(WarehouseRecord.class))).thenReturn(1);
        when(faultMapper.countActiveOwnedFault(9L, 1L)).thenReturn(1);
        User operator = new User();
        operator.setId(2L);
        when(permissionService.currentUser()).thenReturn(operator);
        WarehouseServiceImpl service = new WarehouseServiceImpl(
                itemMapper, recordMapper, faultMapper, mock(UserMapper.class), mock(PlotMapper.class),
                mock(FarmTaskMapper.class), permissionService, mock(OperationLogService.class));

        WarehouseRecord missingFault = outboundRecord(null);
        assertThrows(BusinessException.class, () -> service.addRecord(missingFault));

        WarehouseRecord valid = outboundRecord(9L);
        service.addRecord(valid);

        verify(recordMapper).insert(valid);
        verify(faultMapper).countActiveOwnedFault(9L, 1L);
    }

    private WarehouseRecord outboundRecord(Long faultId) {
        WarehouseRecord record = new WarehouseRecord();
        record.setItemId(7L);
        record.setRecordType(2);
        record.setQuantity(BigDecimal.ONE);
        record.setRecipient("维修人员");
        record.setRelatedFaultId(faultId);
        record.setRequestId("outbound-request-" + (faultId == null ? "missing" : faultId));
        return record;
    }
}
