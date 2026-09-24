package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.ClientDeviceListResponse;
import com.smart_plant.smart_plant.entity.CameraDevice;
import com.smart_plant.smart_plant.entity.IotDevice;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CameraDeviceMapper;
import com.smart_plant.smart_plant.mapper.DeviceTypeMapper;
import com.smart_plant.smart_plant.mapper.IotDeviceFaultMapper;
import com.smart_plant.smart_plant.mapper.IotDeviceMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.MaintenanceMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IotDeviceClientOnlineStatusServiceTest {

    private IotDeviceMapper deviceMapper;
    private CameraDeviceMapper cameraMapper;
    private DataPermissionService dataPermissionService;
    private IotDeviceServiceImpl service;

    @BeforeEach
    void setUp() {
        deviceMapper = mock(IotDeviceMapper.class);
        cameraMapper = mock(CameraDeviceMapper.class);
        dataPermissionService = mock(DataPermissionService.class);
        service = new IotDeviceServiceImpl(
                deviceMapper,
                mock(IotDeviceFaultMapper.class),
                mock(PlotMapper.class),
                mock(DeviceTypeMapper.class),
                cameraMapper,
                mock(UserMapper.class),
                dataPermissionService,
                mock(MaintenanceMessageService.class));
    }

    @Test
    void switchesOwnedIotDeviceOnlineAndReturnsLatestState() {
        IotDevice offline = iotDevice(9L, 3L, 0, 0);
        IotDevice online = iotDevice(9L, 3L, 1, 0);
        when(deviceMapper.selectById(9L)).thenReturn(offline, online);
        when(deviceMapper.updateOnlineStatus(9L, 1)).thenReturn(1);

        ClientDeviceListResponse.DeviceItem result =
                service.updateCurrentClientDeviceOnlineStatus("iot", 9L, 1);

        assertEquals("iot", result.getSource());
        assertEquals("在线", result.getStatus());
        assertTrue(result.getEnabled());
        verify(dataPermissionService).requireDeviceManager(offline);
        verify(deviceMapper).updateOnlineStatus(9L, 1);
    }

    @Test
    void rejectsPuttingFaultDeviceOnline() {
        IotDevice faultDevice = iotDevice(9L, 3L, 0, 1);
        when(deviceMapper.selectById(9L)).thenReturn(faultDevice);

        assertThrows(BusinessException.class,
                () -> service.updateCurrentClientDeviceOnlineStatus("iot", 9L, 1));

        verify(deviceMapper, never()).updateOnlineStatus(9L, 1);
    }

    @Test
    void switchesOwnedCameraOfflineAndReturnsLatestState() {
        CameraDevice online = camera(5L, 3L, 1);
        CameraDevice offline = camera(5L, 3L, 0);
        when(cameraMapper.selectById(5L)).thenReturn(online, offline);
        when(cameraMapper.updateOnlineStatus(5L, 0)).thenReturn(1);

        ClientDeviceListResponse.DeviceItem result =
                service.updateCurrentClientDeviceOnlineStatus("camera", 5L, 0);

        assertEquals("camera", result.getSource());
        assertEquals("离线", result.getStatus());
        assertFalse(result.getEnabled());
        verify(dataPermissionService).requireFarmManager(3L);
        verify(cameraMapper).updateOnlineStatus(5L, 0);
    }

    @Test
    void rejectsUnknownDeviceSourceBeforeUpdating() {
        assertThrows(BusinessException.class,
                () -> service.updateCurrentClientDeviceOnlineStatus("unknown", 9L, 1));

        verify(deviceMapper, never()).updateOnlineStatus(9L, 1);
        verify(cameraMapper, never()).updateOnlineStatus(9L, 1);
    }

    private IotDevice iotDevice(Long id, Long userId, int onlineStatus, int healthStatus) {
        IotDevice device = new IotDevice();
        device.setId(id);
        device.setUserId(userId);
        device.setName("测试设备");
        device.setPlotId(19L);
        device.setPlotName("测试地块");
        device.setOnlineStatus(onlineStatus);
        device.setHealthStatus(healthStatus);
        return device;
    }

    private CameraDevice camera(Long id, Long userId, int onlineStatus) {
        CameraDevice camera = new CameraDevice();
        camera.setId(id);
        camera.setUserId(userId);
        camera.setName("测试摄像头");
        camera.setPlotId(19L);
        camera.setPlotName("测试地块");
        camera.setOnlineStatus(onlineStatus);
        return camera;
    }
}
