package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.FarmTask;
import com.smart_plant.smart_plant.entity.IotDevice;
import com.smart_plant.smart_plant.entity.Notification;
import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.entity.PumpData;
import com.smart_plant.smart_plant.entity.WaterQualityData;
import com.smart_plant.smart_plant.mapper.FarmTaskMapper;
import com.smart_plant.smart_plant.mapper.GrowthStageMapper;
import com.smart_plant.smart_plant.mapper.IotDeviceMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SensorAbnormalTaskServiceImplTest {

    private final IotDeviceMapper iotDeviceMapper = mock(IotDeviceMapper.class);
    private final PlotMapper plotMapper = mock(PlotMapper.class);
    private final GrowthStageMapper growthStageMapper = mock(GrowthStageMapper.class);
    private final FarmTaskMapper farmTaskMapper = mock(FarmTaskMapper.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final SensorAbnormalTaskServiceImpl service = new SensorAbnormalTaskServiceImpl(
            iotDeviceMapper, plotMapper, growthStageMapper, farmTaskMapper, notificationService);

    @BeforeEach
    void setUp() {
        Plot plot = new Plot();
        plot.setId(19L);
        plot.setUserId(3L);
        plot.setPlotName("水培_02号");
        when(plotMapper.selectById(19L)).thenReturn(plot);

        when(iotDeviceMapper.selectById(118L)).thenReturn(device(118L, "水培_02号-水质检测仪"));
        when(iotDeviceMapper.selectById(116L)).thenReturn(device(116L, "水培_02号-水泵"));
        when(farmTaskMapper.selectBySource(any(), any())).thenReturn(null);

        AtomicLong ids = new AtomicLong(100L);
        doAnswer(invocation -> {
            FarmTask task = invocation.getArgument(0);
            task.setId(ids.getAndIncrement());
            return 1;
        }).when(farmTaskMapper).insert(any(FarmTask.class));
    }

    @Test
    void createsIndependentDeviceMessagesForDifferentAbnormalTaskTypes() {
        WaterQualityData waterQuality = new WaterQualityData();
        waterQuality.setId(51L);
        waterQuality.setDeviceId(118L);
        waterQuality.setAbnormalDetail("水温偏低：12℃，低于适宜下限18℃；PH值偏高：9.2，高于适宜上限6.3；EC值偏高：4.2mS/cm，高于适宜上限0.8mS/cm");

        PumpData pump = new PumpData();
        pump.setId(52L);
        pump.setDeviceId(116L);
        pump.setAbnormalDetail("水流量偏低：0.1m³/h，低于适宜下限1.2m³/h；水压偏低：0.04MPa，低于适宜下限0.12MPa");

        service.createWaterQualityTaskIfAbnormal(waterQuality);
        service.createPumpTaskIfAbnormal(pump);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService, org.mockito.Mockito.times(2)).createIfAbsent(captor.capture());
        List<Notification> messages = captor.getAllValues();

        assertThat(messages).extracting(Notification::getTitle)
                .containsExactly("水培_02水质检测仪数据异常", "水培_02水泵数据异常");
        assertThat(messages.get(0).getContent())
                .isEqualTo("水培_02号地块的水培_02号水质检测仪的水温偏低，PH值和EC值偏高，请及时处理。");
        assertThat(messages.get(1).getContent())
                .isEqualTo("水培_02号地块的水培_02号水泵的水流量和水压偏低，请及时处理。");
        assertThat(messages).extracting(Notification::getRefId).doesNotHaveDuplicates();
    }

    private IotDevice device(Long id, String name) {
        IotDevice device = new IotDevice();
        device.setId(id);
        device.setPlotId(19L);
        device.setUserId(3L);
        device.setName(name);
        device.setDeviceCode("DEVICE_" + id);
        return device;
    }
}
