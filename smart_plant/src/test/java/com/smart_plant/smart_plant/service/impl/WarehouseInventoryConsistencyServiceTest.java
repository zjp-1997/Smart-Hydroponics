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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WarehouseInventoryConsistencyServiceTest {

    private final WarehouseItemMapper itemMapper = mock(WarehouseItemMapper.class);
    private final WarehouseRecordMapper recordMapper = mock(WarehouseRecordMapper.class);
    private final IotDeviceFaultMapper faultMapper = mock(IotDeviceFaultMapper.class);
    private final DataPermissionService permissions = mock(DataPermissionService.class);
    private final WarehouseServiceImpl service = new WarehouseServiceImpl(
            itemMapper, recordMapper, faultMapper, mock(UserMapper.class), mock(PlotMapper.class),
            mock(FarmTaskMapper.class), permissions, mock(OperationLogService.class));

    @BeforeEach
    void setUp() {
        User operator = new User();
        operator.setId(2L);
        when(permissions.currentUser()).thenReturn(operator);
    }

    @Test
    void calculatesSnapshotsFromLockedInventoryAndChecksBothWrites() {
        WarehouseItem item = enabledItem(new BigDecimal("10"));
        when(itemMapper.selectByIdForUpdate(7L)).thenReturn(item);
        when(faultMapper.countActiveOwnedFault(9L, 1L)).thenReturn(1);
        when(itemMapper.updateStock(7L, new BigDecimal("3"))).thenReturn(1);
        when(recordMapper.insert(any(WarehouseRecord.class))).thenAnswer(invocation -> {
            invocation.<WarehouseRecord>getArgument(0).setId(88L);
            return 1;
        });
        WarehouseRecord request = outbound("request-lock-001", new BigDecimal("7"));
        when(recordMapper.selectById(88L)).thenReturn(request);

        service.addRecord(request);

        ArgumentCaptor<WarehouseRecord> record = ArgumentCaptor.forClass(WarehouseRecord.class);
        verify(itemMapper).selectByIdForUpdate(7L);
        verify(itemMapper).updateStock(7L, new BigDecimal("3"));
        verify(recordMapper).insert(record.capture());
        assertEquals(new BigDecimal("10"), record.getValue().getBeforeQty());
        assertEquals(new BigDecimal("3"), record.getValue().getAfterQty());
    }

    @Test
    void returnsOriginalRecordWhenSameRequestIsRetried() {
        WarehouseItem item = enabledItem(new BigDecimal("3"));
        item.setStatus(0);
        when(itemMapper.selectByIdForUpdate(7L)).thenReturn(item);
        WarehouseRecord existing = outbound("request-retry-001", new BigDecimal("7"));
        existing.setId(88L);
        existing.setOperatorId(2L);
        when(recordMapper.selectByRequestIdForUpdate("request-retry-001")).thenReturn(existing);
        when(recordMapper.selectById(88L)).thenReturn(existing);

        WarehouseRecord result = service.addRecord(outbound("request-retry-001", new BigDecimal("7")));

        assertSame(existing, result);
        verify(itemMapper, never()).updateStock(any(), any());
        verify(recordMapper, never()).insert(any());
    }

    @Test
    void rejectsReusingRequestIdForDifferentQuantity() {
        when(itemMapper.selectByIdForUpdate(7L)).thenReturn(enabledItem(new BigDecimal("10")));
        WarehouseRecord existing = outbound("request-reused-001", new BigDecimal("2"));
        existing.setId(88L);
        existing.setOperatorId(2L);
        when(recordMapper.selectByRequestIdForUpdate("request-reused-001")).thenReturn(existing);

        assertThrows(BusinessException.class,
                () -> service.addRecord(outbound("request-reused-001", new BigDecimal("3"))));

        verify(itemMapper, never()).updateStock(any(), any());
        verify(recordMapper, never()).insert(any());
    }

    @Test
    void doesNotWriteLedgerWhenInventoryUpdateDidNotAffectOneRow() {
        when(itemMapper.selectByIdForUpdate(7L)).thenReturn(enabledItem(new BigDecimal("10")));
        when(faultMapper.countActiveOwnedFault(9L, 1L)).thenReturn(1);
        when(itemMapper.updateStock(7L, new BigDecimal("9"))).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.addRecord(outbound("request-update-001", BigDecimal.ONE)));

        verify(recordMapper, never()).insert(any());
    }

    @Test
    void rejectsDisabledItemBeforeWritingInventoryOrLedger() {
        WarehouseItem item = enabledItem(new BigDecimal("10"));
        item.setStatus(0);
        when(itemMapper.selectByIdForUpdate(7L)).thenReturn(item);

        assertThrows(BusinessException.class,
                () -> service.addRecord(outbound("request-disabled-001", BigDecimal.ONE)));

        verify(itemMapper, never()).updateStock(any(), any());
        verify(recordMapper, never()).insert(any());
    }

    private WarehouseItem enabledItem(BigDecimal stock) {
        WarehouseItem item = new WarehouseItem();
        item.setId(7L);
        item.setFarmOwnerId(1L);
        item.setItemName("水泵滤芯");
        item.setStockQty(stock);
        item.setStatus(1);
        return item;
    }

    private WarehouseRecord outbound(String requestId, BigDecimal quantity) {
        WarehouseRecord record = new WarehouseRecord();
        record.setRequestId(requestId);
        record.setItemId(7L);
        record.setRecordType(2);
        record.setQuantity(quantity);
        record.setRecipient("维修人员");
        record.setRelatedFaultId(9L);
        return record;
    }
}
