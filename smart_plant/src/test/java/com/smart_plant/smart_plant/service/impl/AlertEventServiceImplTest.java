package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.AlertEvent;
import com.smart_plant.smart_plant.mapper.AlertEventMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertEventServiceImplTest {

    private final AlertEventMapper mapper = mock(AlertEventMapper.class);
    private final AlertEventServiceImpl service = new AlertEventServiceImpl(
            mapper, mock(DataPermissionService.class));

    @Test
    void concurrentCreateReturnsTheRowThatWonTheUniqueKeyRace() {
        AlertEvent request = new AlertEvent();
        request.setUserId(3L);
        request.setAlertType(2);
        request.setAlertTitle("设备离线");
        request.setSourceType("device_fault");
        request.setSourceId(18L);

        AlertEvent winner = new AlertEvent();
        winner.setId(41L);
        when(mapper.selectBySource("device_fault", 18L, 2)).thenReturn(null);
        when(mapper.selectByDedupKeyForUpdate("alert:device_fault:18:2")).thenReturn(winner);
        doThrow(new DuplicateKeyException("duplicate")).when(mapper).insert(request);

        assertThat(service.createIfAbsent(request)).isSameAs(winner);
        assertThat(request.getDedupKey()).isEqualTo("alert:device_fault:18:2");
        verify(mapper).insert(request);
    }
}
