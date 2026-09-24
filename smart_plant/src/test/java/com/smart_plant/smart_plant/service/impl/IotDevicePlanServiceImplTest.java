package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.IotDevicePlanSaveRequest;
import com.smart_plant.smart_plant.entity.IotDevice;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.IotDeviceMapper;
import com.smart_plant.smart_plant.mapper.IotDevicePlanMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.OperationLogService;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/** 验证设备计划的类型约束和跨星期运行时段冲突检测。 */
class IotDevicePlanServiceImplTest {
    private final IotDevicePlanMapper planMapper = mock(IotDevicePlanMapper.class);
    private final IotDeviceMapper deviceMapper = mock(IotDeviceMapper.class);
    private final DataPermissionService permissionService = mock(DataPermissionService.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private final IotDevicePlanServiceImpl service = new IotDevicePlanServiceImpl(
            planMapper, deviceMapper, permissionService, operationLogService);

    /** 周日晚跨午夜的时段与周一凌晨时段发生重叠时必须拒绝保存。 */
    @Test
    void rejectsOverlapAcrossWeekBoundary() {
        when(deviceMapper.selectById(7L)).thenReturn(device("GROW_LIGHT"));
        IotDevicePlanSaveRequest request = baseRequest();
        request.setControlEnabled(true);
        request.setSchedules(List.of(
                schedule("周日晚间", 64, LocalTime.of(23, 0), LocalTime.of(1, 0)),
                schedule("周一凌晨", 1, LocalTime.of(0, 30), LocalTime.of(2, 0))));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.save(request));

        assertTrue(exception.getMessage().contains("运行时段存在重叠"));
        verifyNoInteractions(planMapper, operationLogService);
    }

    /** 环境采集设备只能配置采集周期，不能混入补光灯或水泵的自动开关策略。 */
    @Test
    void rejectsActuatorScheduleForEnvironmentSensor() {
        when(deviceMapper.selectById(7L)).thenReturn(device("ENV_SENSOR"));
        IotDevicePlanSaveRequest request = baseRequest();
        request.setControlEnabled(true);
        request.setSchedules(List.of(
                schedule("非法控制时段", 127, LocalTime.of(6, 0), LocalTime.of(18, 0))));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.save(request));

        assertTrue(exception.getMessage().contains("环境和水质设备不能配置自动开关时段"));
        verifyNoInteractions(planMapper, operationLogService);
    }

    /** 构造通用的有效采集计划请求。 */
    private IotDevicePlanSaveRequest baseRequest() {
        IotDevicePlanSaveRequest request = new IotDevicePlanSaveRequest();
        request.setDeviceId(7L);
        request.setCollectionEnabled(true);
        request.setCollectionIntervalMinutes(60);
        request.setControlEnabled(false);
        request.setTimezone("Asia/Shanghai");
        return request;
    }

    /** 构造测试设备，其他归属字段不参与本组校验。 */
    private IotDevice device(String typeCode) {
        IotDevice device = new IotDevice();
        device.setId(7L);
        device.setTypeCode(typeCode);
        device.setDeviceCode("TEST_DEVICE_007");
        return device;
    }

    /** 构造启用的每周运行时段。 */
    private IotDevicePlanSaveRequest.ScheduleItem schedule(
            String name, int weekdaysMask, LocalTime startTime, LocalTime endTime) {
        IotDevicePlanSaveRequest.ScheduleItem item = new IotDevicePlanSaveRequest.ScheduleItem();
        item.setScheduleName(name);
        item.setWeekdaysMask(weekdaysMask);
        item.setStartTime(startTime);
        item.setEndTime(endTime);
        item.setEnabled(true);
        return item;
    }
}
