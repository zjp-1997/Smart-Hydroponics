package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.ClientMonitorResponse;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CameraDeviceMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证 farm 实时监控接口的数据隔离、在线校验和方向指令。 */
class ClientMonitorServiceImplTest {

    private final CameraDeviceMapper cameraDeviceMapper = mock(CameraDeviceMapper.class);
    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);
    private final ClientMonitorServiceImpl service = new ClientMonitorServiceImpl(
            cameraDeviceMapper, dataPermissionService);

    @BeforeEach
    void setUpCurrentUser() {
        User currentUser = new User();
        currentUser.setId(18L);
        when(dataPermissionService.currentUser()).thenReturn(currentUser);
        when(dataPermissionService.currentClientOwnerId()).thenReturn(18L);
    }

    /** 列表查询必须使用 token 中的用户 ID，不能接受前端伪造用户。 */
    @Test
    void listsOnlyCurrentUserMonitors() {
        ClientMonitorResponse monitor = onlineMonitor(7L);
        when(cameraDeviceMapper.selectClientMonitors(18L, 3L, "一号")).thenReturn(List.of(monitor));

        List<ClientMonitorResponse> result = service.listCurrentClientMonitors(3L, " 一号 ");

        assertEquals(1, result.size());
        assertEquals(7L, result.getFirst().getId());
        verify(cameraDeviceMapper).selectClientMonitors(18L, 3L, "一号");
    }

    /** 不属于当前用户的 ID 在 Mapper 中查不到时统一返回资源不存在。 */
    @Test
    void hidesMonitorOutsideCurrentUserScope() {
        when(cameraDeviceMapper.selectClientMonitorById(99L, 18L)).thenReturn(null);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.getCurrentClientMonitor(99L));

        assertEquals(404, error.getCode());
    }

    /** 在线设备接收方向指令后应保存标准大写值并返回控制结果。 */
    @Test
    void controlsOnlineMonitorWithNormalizedDirection() {
        when(cameraDeviceMapper.selectClientMonitorById(7L, 18L)).thenReturn(onlineMonitor(7L));
        when(cameraDeviceMapper.updateDirection(7L, 18L, "LEFT")).thenReturn(1);

        var result = service.controlCurrentClientMonitor(7L, " left ");

        assertEquals(7L, result.getCameraId());
        assertEquals("LEFT", result.getDirection());
        verify(cameraDeviceMapper).updateDirection(7L, 18L, "LEFT");
    }

    /** 离线设备不得接收云台控制，防止前端误显示操作成功。 */
    @Test
    void rejectsPtzControlForOfflineMonitor() {
        ClientMonitorResponse monitor = onlineMonitor(7L);
        monitor.setOnlineStatus(0);
        when(cameraDeviceMapper.selectClientMonitorById(7L, 18L)).thenReturn(monitor);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.controlCurrentClientMonitor(7L, "UP"));

        assertEquals(400, error.getCode());
    }

    /** 创建测试所需的最小在线监控对象。 */
    private ClientMonitorResponse onlineMonitor(Long id) {
        ClientMonitorResponse monitor = new ClientMonitorResponse();
        monitor.setId(id);
        monitor.setCameraName("01号摄像头");
        monitor.setOnlineStatus(1);
        return monitor;
    }
}
