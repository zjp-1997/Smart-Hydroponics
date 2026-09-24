package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.CameraCapturePlanSaveRequest;
import com.smart_plant.smart_plant.entity.CameraCapturePlan;
import com.smart_plant.smart_plant.entity.CameraDevice;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CameraCapturePlanMapper;
import com.smart_plant.smart_plant.mapper.CameraDeviceMapper;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 验证摄像头计划的数据归属、时间校验和待下发初始化行为。 */
class CameraCapturePlanServiceImplTest {
    private final CameraCapturePlanMapper planMapper = mock(CameraCapturePlanMapper.class);
    private final CameraDeviceMapper cameraMapper = mock(CameraDeviceMapper.class);
    private final DataPermissionService permissionService = mock(DataPermissionService.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private final CameraCapturePlanServiceImpl service = new CameraCapturePlanServiceImpl(
            planMapper, cameraMapper, permissionService, operationLogService);

    /** 每个测试后清理线程登录上下文，避免污染其他服务测试。 */
    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    /** 新增计划的数据所有者和地块必须来自可信摄像头关系。 */
    @Test
    void addDerivesOwnershipAndCalculatesNextCapture() {
        CameraDevice camera = new CameraDevice();
        camera.setId(7L);
        camera.setUserId(11L);
        camera.setPlotId(13L);
        when(cameraMapper.selectById(7L)).thenReturn(camera);
        User operator = new User();
        operator.setId(2L);
        when(permissionService.currentUser()).thenReturn(operator);
        when(planMapper.insert(any(CameraCapturePlan.class))).thenAnswer(invocation -> {
            CameraCapturePlan saved = invocation.getArgument(0);
            saved.setId(19L);
            return 1;
        });
        when(planMapper.selectById(19L)).thenAnswer(invocation -> {
            CameraCapturePlan result = new CameraCapturePlan();
            result.setId(19L);
            return result;
        });

        CameraCapturePlan result = service.add(validRequest());

        assertEquals(19L, result.getId());
        verify(planMapper).insert(argThat(plan -> plan.getUserId().equals(11L)
                && plan.getPlotId().equals(13L) && plan.getNextCaptureTime() != null));
        verify(permissionService).requireFarmManager(11L);
        verify(operationLogService).record("新增摄像头采集计划：作物定时采集");
    }

    /** 结束时间不晚于开始时间时应在访问数据库前失败。 */
    @Test
    void rejectsInvalidDailyWindow() {
        CameraCapturePlanSaveRequest request = validRequest();
        request.setEndTime(LocalTime.of(5, 0));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.add(request));

        assertEquals("每日结束时间必须晚于开始时间", exception.getMessage());
        verifyNoInteractions(cameraMapper, planMapper);
    }

    /** 构造满足服务层约束的最小新增请求。 */
    private CameraCapturePlanSaveRequest validRequest() {
        CameraCapturePlanSaveRequest request = new CameraCapturePlanSaveRequest();
        request.setDeviceId(7L);
        request.setPlanName("作物定时采集");
        request.setIntervalMinutes(60);
        request.setWeekdaysMask(127);
        request.setStartTime(LocalTime.of(6, 0));
        request.setEndTime(LocalTime.of(18, 0));
        request.setTimezone("Asia/Shanghai");
        request.setEnabled(true);
        return request;
    }
}
