package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.CameraDevice;
import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CameraDeviceMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.CameraDeviceService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 监控设备业务实现类。
 *
 * <p>该类负责 camera_device 表的字段校验、默认值补齐、权限控制、删除保护和分页查询。</p>
 */
@Service
@RequiredArgsConstructor
public class CameraDeviceServiceImpl implements CameraDeviceService {

    private static final int ONLINE_STATUS_OFFLINE = 0;
    private static final int ONLINE_STATUS_ONLINE = 1;
    private static final Set<String> STREAM_PROTOCOLS = Set.of("RTSP", "GB28181", "HTTP");

    private final CameraDeviceMapper cameraDeviceMapper;
    private final PlotMapper plotMapper;
    private final DataPermissionService dataPermissionService;

    /** 新增监控设备时校验必填字段，补齐默认值，并检查地块权限和来源设备编号唯一性。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CameraDevice addCameraDevice(CameraDevice cameraDevice) {
        validateCreate(cameraDevice);
        normalizeDefaults(cameraDevice);
        Plot plot = checkPlotExists(cameraDevice.getPlotId());
        dataPermissionService.requireFarmManager(plot.getUserId());
        checkUniqueDeviceId(cameraDevice);
        cameraDeviceMapper.insert(cameraDevice);
        return cameraDeviceMapper.selectById(cameraDevice.getId());
    }

    /** 删除监控设备前检查业务引用，避免误删已有抓拍计划或抓拍记录的摄像头。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCameraDevice(Long id) {
        CameraDevice oldCamera = getCameraDeviceById(id);
        checkNoReferences(oldCamera.getId());
        int rows = cameraDeviceMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "监控设备不存在");
        }
    }

    /** 批量删除时逐条检查权限和引用，保证不会出现部分删除。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCameraDevices(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的监控设备");
        }
        for (Long id : ids) {
            CameraDevice oldCamera = getCameraDeviceById(id);
            checkNoReferences(oldCamera.getId());
        }
        return cameraDeviceMapper.deleteBatchByIds(ids);
    }

    /** 修改监控设备时支持局部更新，未传字段沿用数据库旧值。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CameraDevice updateCameraDevice(CameraDevice cameraDevice) {
        if (cameraDevice == null || cameraDevice.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "监控设备ID不能为空");
        }
        CameraDevice oldCamera = cameraDeviceMapper.selectById(cameraDevice.getId());
        if (oldCamera == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "监控设备不存在");
        }
        dataPermissionService.requireFarmManager(oldCamera.getUserId());
        normalizeUpdateFields(cameraDevice, oldCamera);
        validateCommon(cameraDevice);
        Plot plot = checkPlotExists(cameraDevice.getPlotId());
        dataPermissionService.requireFarmManager(plot.getUserId());
        checkUniqueDeviceId(cameraDevice);
        int rows = cameraDeviceMapper.updateById(cameraDevice);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "监控设备修改失败");
        }
        return cameraDeviceMapper.selectById(cameraDevice.getId());
    }

    /** 单独维护在线状态，便于前端列表快速切换。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOnlineStatus(Long id, Integer onlineStatus) {
        CameraDevice oldCamera = getCameraDeviceById(id);
        validateOnlineStatusRequired(onlineStatus);
        int rows = cameraDeviceMapper.updateOnlineStatus(oldCamera.getId(), onlineStatus);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "监控设备不存在");
        }
    }

    /** 查询详情时统一处理ID为空、记录不存在和数据权限。 */
    @Override
    public CameraDevice getCameraDeviceById(Long id) {
        requireId(id);
        CameraDevice cameraDevice = cameraDeviceMapper.selectById(id);
        if (cameraDevice == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "监控设备不存在");
        }
        dataPermissionService.requireFarmManager(cameraDevice.getUserId());
        return cameraDevice;
    }

    /** 分页查询监控设备，并按当前用户角色收敛可见数据范围。 */
    @Override
    public PageInfo<CameraDevice> listCameraDevices(Long plotId, String plotName, String name,
                                                    String streamProtocol, Integer onlineStatus,
                                                    Integer pageNum, Integer pageSize) {
        if (plotId != null) {
            Plot plot = checkPlotExists(plotId);
            dataPermissionService.requireFarmManager(plot.getUserId());
        }
        validateOnlineStatus(onlineStatus);
        String normalizedProtocol = normalizeProtocol(streamProtocol);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(cameraDeviceMapper.selectList(
                scopedUserId,
                plotId,
                normalizeOptionalText(plotName),
                normalizeOptionalText(name),
                normalizedProtocol,
                onlineStatus));
    }

    private void validateCreate(CameraDevice cameraDevice) {
        if (cameraDevice == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "监控设备信息不能为空");
        }
        if (cameraDevice.getDeviceId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "来源设备编号不能为空");
        }
        if (cameraDevice.getPlotId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属地块不能为空");
        }
        if (!StringUtils.hasText(cameraDevice.getName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "监控设备名称不能为空");
        }
    }

    private void normalizeDefaults(CameraDevice cameraDevice) {
        cameraDevice.setName(normalizeRequiredText(cameraDevice.getName(), "监控设备名称不能为空"));
        cameraDevice.setStreamProtocol(normalizeProtocolOrDefault(cameraDevice.getStreamProtocol()));
        cameraDevice.setStreamUrl(normalizeOptionalText(cameraDevice.getStreamUrl()));
        cameraDevice.setSnapshotUrl(normalizeOptionalText(cameraDevice.getSnapshotUrl()));
        cameraDevice.setResolution(normalizeOptionalText(cameraDevice.getResolution()));
        cameraDevice.setOnlineStatus(cameraDevice.getOnlineStatus() == null ? ONLINE_STATUS_OFFLINE : cameraDevice.getOnlineStatus());
        cameraDevice.setDirection(normalizeOptionalText(cameraDevice.getDirection()));
        validateCommon(cameraDevice);
    }

    private void normalizeUpdateFields(CameraDevice cameraDevice, CameraDevice oldCamera) {
        cameraDevice.setDeviceId(cameraDevice.getDeviceId() == null ? oldCamera.getDeviceId() : cameraDevice.getDeviceId());
        cameraDevice.setPlotId(cameraDevice.getPlotId() == null ? oldCamera.getPlotId() : cameraDevice.getPlotId());
        cameraDevice.setName(StringUtils.hasText(cameraDevice.getName())
                ? cameraDevice.getName().trim() : oldCamera.getName());
        cameraDevice.setStreamProtocol(cameraDevice.getStreamProtocol() == null
                ? oldCamera.getStreamProtocol() : normalizeProtocol(cameraDevice.getStreamProtocol()));
        cameraDevice.setStreamUrl(cameraDevice.getStreamUrl() == null
                ? oldCamera.getStreamUrl() : normalizeOptionalText(cameraDevice.getStreamUrl()));
        cameraDevice.setSnapshotUrl(cameraDevice.getSnapshotUrl() == null
                ? oldCamera.getSnapshotUrl() : normalizeOptionalText(cameraDevice.getSnapshotUrl()));
        cameraDevice.setResolution(cameraDevice.getResolution() == null
                ? oldCamera.getResolution() : normalizeOptionalText(cameraDevice.getResolution()));
        cameraDevice.setOnlineStatus(cameraDevice.getOnlineStatus() == null ? oldCamera.getOnlineStatus() : cameraDevice.getOnlineStatus());
        cameraDevice.setDirection(cameraDevice.getDirection() == null
                ? oldCamera.getDirection() : normalizeOptionalText(cameraDevice.getDirection()));
    }

    private void validateCommon(CameraDevice cameraDevice) {
        validateOnlineStatusRequired(cameraDevice.getOnlineStatus());
        validateLength(cameraDevice.getName(), 100, "监控设备名称不能超过100个字符");
        validateLength(cameraDevice.getStreamProtocol(), 20, "视频流协议不能超过20个字符");
        validateLength(cameraDevice.getStreamUrl(), 500, "视频流地址不能超过500个字符");
        validateLength(cameraDevice.getSnapshotUrl(), 500, "截图地址不能超过500个字符");
        validateLength(cameraDevice.getResolution(), 50, "分辨率不能超过50个字符");
        validateLength(cameraDevice.getDirection(), 50, "监控方向不能超过50个字符");
    }

    private Plot checkPlotExists(Long plotId) {
        if (plotId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属地块不能为空");
        }
        Plot plot = plotMapper.selectById(plotId);
        if (plot == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "所属地块不存在");
        }
        return plot;
    }

    private void checkUniqueDeviceId(CameraDevice cameraDevice) {
        if (cameraDeviceMapper.countByDeviceId(cameraDevice.getDeviceId(), cameraDevice.getId()) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "来源设备编号已存在");
        }
    }

    private void checkNoReferences(Long id) {
        if (cameraDeviceMapper.countReferencesByCameraId(id) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "监控设备已存在抓拍计划或记录，不能删除");
        }
    }

    private void validateOnlineStatusRequired(Integer onlineStatus) {
        if (onlineStatus == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "在线状态不能为空");
        }
        validateOnlineStatus(onlineStatus);
    }

    private void validateOnlineStatus(Integer onlineStatus) {
        if (onlineStatus != null && onlineStatus != ONLINE_STATUS_OFFLINE && onlineStatus != ONLINE_STATUS_ONLINE) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "在线状态只能为0或1");
        }
    }

    private String normalizeProtocolOrDefault(String value) {
        String protocol = normalizeProtocol(value);
        return protocol == null ? "RTSP" : protocol;
    }

    private String normalizeProtocol(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String protocol = value.trim().toUpperCase(Locale.ROOT);
        if (!STREAM_PROTOCOLS.contains(protocol)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "视频流协议只能为RTSP、GB28181或HTTP");
        }
        return protocol;
    }

    private String normalizeRequiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "监控设备ID不能为空");
        }
    }
}
