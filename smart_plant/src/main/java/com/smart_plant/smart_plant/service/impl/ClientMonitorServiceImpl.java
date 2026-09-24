package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.CameraPtzResponse;
import com.smart_plant.smart_plant.dto.ClientMonitorResponse;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CameraDeviceMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.ClientMonitorService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * farm 用户端实时监控服务实现。
 *
 * <p>查询按 token 对应的农场主或绑定农场范围过滤；云台控制仅允许农场主。</p>
 */
@Service
@RequiredArgsConstructor
public class ClientMonitorServiceImpl implements ClientMonitorService {

    /** 前端方向按钮允许下发的标准指令集合。 */
    private static final Set<String> PTZ_DIRECTIONS = Set.of("UP", "DOWN", "LEFT", "RIGHT", "STOP");

    /** 监控设备 Mapper，负责用户端列表、详情和最近控制指令更新。 */
    private final CameraDeviceMapper cameraDeviceMapper;

    /** 数据权限服务，负责从 token 取得可信的当前用户。 */
    private final DataPermissionService dataPermissionService;

    @Override
    public List<ClientMonitorResponse> listCurrentClientMonitors(Long plotId, String keyword) {
        Long currentUserId = currentUserId();
        return cameraDeviceMapper.selectClientMonitors(currentUserId, plotId, normalizeKeyword(keyword));
    }

    @Override
    public ClientMonitorResponse getCurrentClientMonitor(Long id) {
        return findCurrentClientMonitor(id, currentUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CameraPtzResponse controlCurrentClientMonitor(Long id, String direction) {
        Long currentUserId = currentUserId();
        // 云台属于设备控制，普通用户仅可查看实时画面。
        dataPermissionService.requireFarmManager(currentUserId);
        ClientMonitorResponse monitor = findCurrentClientMonitor(id, currentUserId);
        if (!Integer.valueOf(1).equals(monitor.getOnlineStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "监控设备离线，无法控制云台");
        }
        String normalizedDirection = normalizeDirection(direction);
        // 当前 camera_device.direction 用于保存最近一次下发指令；接入厂商网关时可在此处追加 ONVIF/SDK 调用。
        if (cameraDeviceMapper.updateDirection(id, currentUserId, normalizedDirection) != 1) {
            throw new BusinessException(ResponseCode.FAIL, "云台控制指令下发失败");
        }
        return new CameraPtzResponse(id, normalizedDirection, LocalDateTime.now());
    }

    /** 从安全上下文解析可见农场主ID，并对异常登录态提供明确错误。 */
    private Long currentUserId() {
        User currentUser = dataPermissionService.currentUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED);
        }
        return dataPermissionService.currentClientOwnerId();
    }

    /** 在指定用户范围内查询监控，用于事务中的控制校验，避免控制前后二次解析用户。 */
    private ClientMonitorResponse findCurrentClientMonitor(Long id, Long currentUserId) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "监控设备ID不能为空");
        }
        ClientMonitorResponse monitor = cameraDeviceMapper.selectClientMonitorById(id, currentUserId);
        if (monitor == null) {
            // 使用统一的“不存在”响应隐藏其他用户的资源，避免通过 ID 探测数据归属。
            throw new BusinessException(ResponseCode.NOT_FOUND, "监控设备不存在");
        }
        return monitor;
    }

    /** 统一清理可选搜索词，空白内容不进入 SQL 条件。 */
    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    /** 将方向转换为大写标准值，并拒绝任意未定义指令。 */
    private String normalizeDirection(String direction) {
        if (!StringUtils.hasText(direction)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "云台控制方向不能为空");
        }
        String normalizedDirection = direction.trim().toUpperCase(Locale.ROOT);
        if (!PTZ_DIRECTIONS.contains(normalizedDirection)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "云台控制方向不正确");
        }
        return normalizedDirection;
    }
}
