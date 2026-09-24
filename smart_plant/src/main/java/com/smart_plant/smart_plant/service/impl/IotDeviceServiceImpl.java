package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ClientDeviceListResponse;
import com.smart_plant.smart_plant.entity.CameraDevice;
import com.smart_plant.smart_plant.entity.DeviceType;
import com.smart_plant.smart_plant.entity.IotDevice;
import com.smart_plant.smart_plant.entity.IotDeviceFault;
import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CameraDeviceMapper;
import com.smart_plant.smart_plant.mapper.DeviceTypeMapper;
import com.smart_plant.smart_plant.mapper.IotDeviceFaultMapper;
import com.smart_plant.smart_plant.mapper.IotDeviceMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.MaintenanceMessageService;
import com.smart_plant.smart_plant.service.IotDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 设备信息业务实现类。
 *
 * <p>该类负责设备管理的字段校验、默认值补齐、唯一性校验、删除保护和分页查询。</p>
 */
@Service
@RequiredArgsConstructor
public class IotDeviceServiceImpl implements IotDeviceService {

    /** 业务控制状态：开启。 */
    private static final int CONTROL_STATUS_ON = 1;

    /** 业务控制状态：关闭。 */
    private static final int CONTROL_STATUS_OFF = 0;

    /** 在线状态：在线。 */
    private static final int ONLINE_STATUS_ONLINE = 1;

    /** 在线状态：离线。 */
    private static final int ONLINE_STATUS_OFFLINE = 0;

    /** 健康状态：正常。 */
    private static final int HEALTH_STATUS_NORMAL = 0;

    /** 健康状态：故障。 */
    private static final int HEALTH_STATUS_FAULT = 1;

    /** 健康状态：维护中。 */
    private static final int HEALTH_STATUS_MAINTENANCE = 2;

    /** 自动故障单默认类型：其他故障。 */
    private static final int AUTO_FAULT_TYPE_OTHER = 4;

    /** 自动故障单默认严重程度：高。 */
    private static final int AUTO_FAULT_SEVERITY_HIGH = 3;

    /** 故障单状态：待处理。 */
    private static final int FAULT_STATUS_PENDING = 0;

    /** 故障单状态：处理中。 */
    private static final int FAULT_STATUS_PROCESSING = 1;

    /** 设备类型编码：补光灯。 */
    private static final String TYPE_CODE_GROW_LIGHT = "GROW_LIGHT";

    /** 设备类型编码：水泵。 */
    private static final String TYPE_CODE_WATER_PUMP = "WATER_PUMP";

    /** 设备类型编码：摄像头。 */
    private static final String TYPE_CODE_CAMERA = "CAMERA";

    /** 设备类型编码：水质检测仪。 */
    private static final String TYPE_CODE_WATER_QUALITY = "WATER_QUALITY";

    /** 设备类型编码：环境检测仪。 */
    private static final String TYPE_CODE_ENV_SENSOR = "ENV_SENSOR";

    /** 后台自动生成设备编码的固定前缀。 */
    private static final String GENERATED_DEVICE_CODE_PREFIX = "D_";

    private static final String CLIENT_DEVICE_SOURCE_IOT = "iot";

    private static final String CLIENT_DEVICE_SOURCE_CAMERA = "camera";

    /** 设备 Mapper，负责 iot_device 表的读写。 */
    private final IotDeviceMapper iotDeviceMapper;

    /** 设备故障 Mapper，用于设备健康异常时自动生成故障单。 */
    private final IotDeviceFaultMapper iotDeviceFaultMapper;

    /** 地块 Mapper，用于校验设备关联地块是否存在。 */
    private final PlotMapper plotMapper;

    /** 设备类型 Mapper，用于校验 iot_device.type_id 是否存在且启用。 */
    private final DeviceTypeMapper deviceTypeMapper;

    /** 监控设备 Mapper，用于聚合独立 camera_device 表中的摄像头数据。 */
    private final CameraDeviceMapper cameraDeviceMapper;

    /** 用户 Mapper，用于查找农场主绑定的技术人员。 */
    private final UserMapper userMapper;

    private final DataPermissionService dataPermissionService;

    /** 故障落库后在同一事务中生成维护消息及角色通知。 */
    private final MaintenanceMessageService maintenanceMessageService;

    /** 新增设备时先校验必填字段，再补齐默认状态并检查设备编码唯一性。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public IotDevice addDevice(IotDevice iotDevice) {
        validateCreateDevice(iotDevice);
        normalizeDefaults(iotDevice);
        validateUpdateDevice(iotDevice);
        Plot plot = checkPlotExists(iotDevice.getPlotId());
        iotDevice.setUserId(plot.getUserId());
        dataPermissionService.requireDeviceManager(iotDevice);
        fillLocationFromPlotIfBlank(iotDevice, plot);
        checkUniqueDeviceCode(iotDevice);
        iotDeviceMapper.insert(iotDevice);
        IotDevice savedDevice = iotDeviceMapper.selectById(iotDevice.getId());
        ensureOpenFaultForFaultDevice(savedDevice);
        return savedDevice;
    }

    /** 删除单台设备前先确认设备存在且未被业务数据引用。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDevice(Long id) {
        requireId(id);
        IotDevice oldDevice = getDeviceById(id);
        dataPermissionService.requireDeviceManager(oldDevice);
        checkNoReferences(id);
        int rows = iotDeviceMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备不存在");
        }
    }

    /** 批量删除设备时逐个做删除保护，避免部分设备存在业务引用。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDevices(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的设备");
        }
        for (Long id : ids) {
            requireId(id);
            IotDevice oldDevice = getDeviceById(id);
            dataPermissionService.requireDeviceManager(oldDevice);
            checkNoReferences(id);
        }
        return iotDeviceMapper.deleteBatchByIds(ids);
    }

    /** 修改设备信息时允许部分字段更新，并对状态枚举和设备编码唯一性做校验。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public IotDevice updateDevice(IotDevice iotDevice) {
        if (iotDevice == null || iotDevice.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备ID不能为空");
        }
        IotDevice oldDevice = iotDeviceMapper.selectById(iotDevice.getId());
        if (oldDevice == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备不存在");
        }
        dataPermissionService.requireDeviceManager(oldDevice);
        normalizeUpdateFields(iotDevice, oldDevice);
        validateUpdateDevice(iotDevice);
        Plot plot = checkPlotExists(iotDevice.getPlotId());
        iotDevice.setUserId(plot.getUserId());
        dataPermissionService.requireDeviceManager(iotDevice);
        fillLocationFromPlotIfBlank(iotDevice, plot);
        checkUniqueDeviceCode(iotDevice);
        int rows = iotDeviceMapper.updateById(iotDevice);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "设备修改失败");
        }
        IotDevice savedDevice = iotDeviceMapper.selectById(iotDevice.getId());
        ensureOpenFaultForFaultDevice(savedDevice);
        return savedDevice;
    }

    /** 单独修改控制状态，用于前端开关设备。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateControlStatus(Long id, Integer controlStatus) {
        requireId(id);
        IotDevice oldDevice = getDeviceById(id);
        requireInteger(controlStatus, "设备控制状态不能为空");
        dataPermissionService.requireDeviceManager(oldDevice);
        validateControlStatus(controlStatus);
        if (CONTROL_STATUS_ON == controlStatus && isUnhealthy(oldDevice.getHealthStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "故障或维护中的设备不能开启");
        }
        int rows = iotDeviceMapper.updateControlStatus(id, controlStatus);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备不存在");
        }
    }

    /** 单独修改在线状态，用于设备连接状态维护。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOnlineStatus(Long id, Integer onlineStatus) {
        requireId(id);
        requireInteger(onlineStatus, "设备在线状态不能为空");
        IotDevice oldDevice = getDeviceById(id);
        dataPermissionService.requireDeviceManager(oldDevice);
        validateOnlineStatus(onlineStatus);
        if (ONLINE_STATUS_ONLINE == onlineStatus && isUnhealthy(oldDevice.getHealthStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "故障或维护中的设备在线状态只能为离线");
        }
        int rows = iotDeviceMapper.updateOnlineStatus(id, onlineStatus);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备不存在");
        }
    }

    /** 单独修改健康状态，用于故障和维护状态标记。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHealthStatus(Long id, Integer healthStatus) {
        requireId(id);
        requireInteger(healthStatus, "设备健康状态不能为空");
        IotDevice oldDevice = getDeviceById(id);
        dataPermissionService.requireDeviceManager(oldDevice);
        validateHealthStatus(healthStatus);
        int rows;
        if (isUnhealthy(healthStatus)) {
            rows = iotDeviceMapper.updateById(buildUnhealthyStatusUpdate(id, healthStatus));
        } else {
            rows = iotDeviceMapper.updateHealthStatus(id, healthStatus);
        }
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备不存在");
        }
        if (HEALTH_STATUS_FAULT == healthStatus) {
            ensureOpenFaultForFaultDevice(iotDeviceMapper.selectById(id));
        }
    }

    /** 更新心跳时间，若调用方不传时间则默认使用当前服务器时间。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void heartbeat(Long id, LocalDateTime lastHeartbeatTime) {
        requireId(id);
        IotDevice oldDevice = getDeviceById(id);
        dataPermissionService.requireDeviceManager(oldDevice);
        LocalDateTime heartbeatTime = lastHeartbeatTime == null ? LocalDateTime.now() : lastHeartbeatTime;
        int rows;
        if (isUnhealthy(oldDevice.getHealthStatus())) {
            IotDevice iotDevice = new IotDevice();
            iotDevice.setId(id);
            iotDevice.setControlStatus(CONTROL_STATUS_OFF);
            iotDevice.setOnlineStatus(ONLINE_STATUS_OFFLINE);
            iotDevice.setLastHeartbeatTime(heartbeatTime);
            rows = iotDeviceMapper.updateById(iotDevice);
        } else {
            rows = iotDeviceMapper.updateHeartbeat(id, heartbeatTime);
        }
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备不存在");
        }
    }

    /** 查询设备详情，统一处理ID为空和设备不存在的错误。 */
    @Override
    public IotDevice getDeviceById(Long id) {
        requireId(id);
        IotDevice iotDevice = iotDeviceMapper.selectById(id);
        if (iotDevice == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备不存在");
        }
        dataPermissionService.requireOwnedResource(iotDevice.getUserId());
        return iotDevice;
    }

    /** 分页查询设备列表，并复用 PageHelper 生成分页元数据。 */
    @Override
    public PageInfo<IotDevice> listDevices(Long plotId, String plotName, String deviceCode, String name, Long typeId,
                                           Integer controlStatus, Integer onlineStatus, Integer healthStatus,
                                           Integer pageNum, Integer pageSize) {
        if (plotId != null) {
            checkPlotExists(plotId);
        }
        validateTypeExists(typeId);
        validateControlStatus(controlStatus);
        validateOnlineStatus(onlineStatus);
        validateHealthStatus(healthStatus);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(iotDeviceMapper.selectList(
                scopedUserId,
                plotId,
                normalizeOptionalText(plotName),
                normalizeOptionalText(deviceCode),
                normalizeOptionalText(name),
                typeId,
                controlStatus,
                onlineStatus,
                healthStatus));
    }

    /**
     * 查询当前登录用户的全部设备，并按用户端页面需要的设备类别分组。
     *
     * <p>这里不允许前端传 userId，查询范围由 token 中的当前用户决定，避免越权访问其他用户设备。</p>
     */
    @Override
    public ClientDeviceListResponse listCurrentClientDeviceGroups() {
        // 工作人员只能查看所属农场设备状态，设备管理动作仍由农场主接口校验。
        Long currentUserId = dataPermissionService.currentClientOwnerId();
        List<IotDevice> devices = iotDeviceMapper.selectList(
                currentUserId, null, null, null, null, null, null, null, null);
        ClientDeviceListResponse response = new ClientDeviceListResponse();
        for (IotDevice device : devices) {
            addClientDeviceToGroup(response, device);
        }
        cameraDeviceMapper.selectList(currentUserId, null, null, null, null, null)
                .stream()
                .map(this::toClientCameraItem)
                .forEach(response.getCameraList()::add);
        return response;
    }

    /**
     * 更新 farm 设备管理页开关状态。设备来源参与定位，避免两张设备表主键相同时误更新。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientDeviceListResponse.DeviceItem updateCurrentClientDeviceOnlineStatus(
            String source, Long id, Integer onlineStatus) {
        String normalizedSource = normalizeClientDeviceSource(source);
        requireId(id);
        requireInteger(onlineStatus, "设备在线状态不能为空");
        validateOnlineStatus(onlineStatus);
        if (CLIENT_DEVICE_SOURCE_IOT.equals(normalizedSource)) {
            updateOnlineStatus(id, onlineStatus);
            return toClientDeviceItem(iotDeviceMapper.selectById(id));
        }

        CameraDevice cameraDevice = cameraDeviceMapper.selectById(id);
        if (cameraDevice == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "监控设备不存在");
        }
        dataPermissionService.requireFarmManager(cameraDevice.getUserId());
        if (cameraDeviceMapper.updateOnlineStatus(id, onlineStatus) == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "监控设备不存在");
        }
        return toClientCameraItem(cameraDeviceMapper.selectById(id));
    }

    /** 校验新增设备的必填字段。 */
    private void validateCreateDevice(IotDevice iotDevice) {
        if (iotDevice == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备信息不能为空");
        }
        if (!StringUtils.hasText(iotDevice.getName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备名称不能为空");
        }
        if (iotDevice.getTypeId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备类型不能为空");
        }
        if (iotDevice.getPlotId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "关联地块不能为空");
        }
    }

    /** 根据设备类型编码或设备名称兜底识别分类，并放入对应的用户端分组列表。 */
    private void addClientDeviceToGroup(ClientDeviceListResponse response, IotDevice device) {
        ClientDeviceListResponse.DeviceItem item = toClientDeviceItem(device);
        String typeCode = device.getTypeCode();
        String deviceName = normalizeOptionalText(device.getName());
        if (TYPE_CODE_GROW_LIGHT.equals(typeCode) || contains(deviceName, "灯")) {
            response.getGrowLightList().add(item);
            return;
        }
        if (TYPE_CODE_WATER_PUMP.equals(typeCode) || contains(deviceName, "泵")) {
            response.getPumpList().add(item);
            return;
        }
        if (TYPE_CODE_CAMERA.equals(typeCode) || contains(deviceName, "摄像")) {
            response.getCameraList().add(item);
            return;
        }
        if (TYPE_CODE_WATER_QUALITY.equals(typeCode) || contains(deviceName, "水质") || contains(deviceName, "检测仪")) {
            response.getWaterQualityList().add(item);
            return;
        }
        if (TYPE_CODE_ENV_SENSOR.equals(typeCode) || contains(deviceName, "环境")) {
            response.getEnvironmentList().add(item);
        }
    }

    /** 将后台设备实体转换为用户端列表行，只保留页面展示需要的字段。 */
    private ClientDeviceListResponse.DeviceItem toClientDeviceItem(IotDevice device) {
        return new ClientDeviceListResponse.DeviceItem(
                device.getId(),
                defaultText(device.getName(), "未知设备"),
                device.getPlotId(),
                defaultText(device.getPlotName(), "-"),
                resolveClientDeviceStatus(device),
                Integer.valueOf(ONLINE_STATUS_ONLINE).equals(device.getOnlineStatus()),
                CLIENT_DEVICE_SOURCE_IOT
        );
    }

    /** 将监控设备表数据转换为用户端摄像头列表行，保持与普通设备相同的响应结构。 */
    private ClientDeviceListResponse.DeviceItem toClientCameraItem(CameraDevice cameraDevice) {
        return new ClientDeviceListResponse.DeviceItem(
                cameraDevice.getId(),
                defaultText(cameraDevice.getName(), "摄像头"),
                cameraDevice.getPlotId(),
                defaultText(cameraDevice.getPlotName(), "-"),
                Integer.valueOf(ONLINE_STATUS_ONLINE).equals(cameraDevice.getOnlineStatus()) ? "在线" : "离线",
                Integer.valueOf(ONLINE_STATUS_ONLINE).equals(cameraDevice.getOnlineStatus()),
                CLIENT_DEVICE_SOURCE_CAMERA
        );
    }

    private String normalizeClientDeviceSource(String source) {
        if (!StringUtils.hasText(source)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备来源不能为空");
        }
        String normalizedSource = source.trim().toLowerCase(Locale.ROOT);
        if (!CLIENT_DEVICE_SOURCE_IOT.equals(normalizedSource)
                && !CLIENT_DEVICE_SOURCE_CAMERA.equals(normalizedSource)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备来源只能为iot或camera");
        }
        return normalizedSource;
    }

    /** 健康状态优先于在线状态，故障或维护统一在用户端展示为故障。 */
    private String resolveClientDeviceStatus(IotDevice device) {
        if (isUnhealthy(device.getHealthStatus())) {
            return "故障";
        }
        if (Integer.valueOf(ONLINE_STATUS_ONLINE).equals(device.getOnlineStatus())) {
            return "在线";
        }
        return "离线";
    }

    /** 安全判断文本是否包含指定关键字，避免空指针。 */
    private boolean contains(String source, String keyword) {
        return source != null && source.contains(keyword);
    }

    /** 用户端展示文本兜底，防止空字符串直接透传到页面。 */
    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    /** 校验新增和修改共用的状态字段枚举。 */
    private void validateUpdateDevice(IotDevice iotDevice) {
        validateControlStatus(iotDevice.getControlStatus());
        validateOnlineStatus(iotDevice.getOnlineStatus());
        validateHealthStatus(iotDevice.getHealthStatus());
        validateEnabledTypeId(iotDevice.getTypeId());
        applyUnhealthyControlRule(iotDevice);
    }

    /** 新增时补齐默认状态，并清理文本两侧空白。 */
    private void normalizeDefaults(IotDevice iotDevice) {
        iotDevice.setDeviceCode(StringUtils.hasText(iotDevice.getDeviceCode())
                ? iotDevice.getDeviceCode().trim()
                : generateDeviceCode());
        if (iotDevice.getPlotId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "关联地块不能为空");
        }
        iotDevice.setName(normalizeRequiredText(iotDevice.getName(), "设备名称不能为空"));
        if (iotDevice.getTypeId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备类型不能为空");
        }
        iotDevice.setControlStatus(defaultInteger(iotDevice.getControlStatus(), CONTROL_STATUS_OFF));
        iotDevice.setOnlineStatus(defaultInteger(iotDevice.getOnlineStatus(), ONLINE_STATUS_OFFLINE));
        iotDevice.setHealthStatus(defaultInteger(iotDevice.getHealthStatus(), HEALTH_STATUS_NORMAL));
        iotDevice.setInstallTime(iotDevice.getInstallTime() == null ? LocalDateTime.now() : iotDevice.getInstallTime());
        iotDevice.setOfflineTime(Integer.valueOf(ONLINE_STATUS_OFFLINE).equals(iotDevice.getOnlineStatus())
                ? iotDevice.getInstallTime()
                : null);
        iotDevice.setFaultTime(isUnhealthy(iotDevice.getHealthStatus()) ? iotDevice.getInstallTime() : null);
        iotDevice.setLastOnlineTime(Integer.valueOf(ONLINE_STATUS_ONLINE).equals(iotDevice.getOnlineStatus())
                ? iotDevice.getInstallTime()
                : null);
        iotDevice.setOnlineDuration(0L);
        iotDevice.setLocation(normalizeOptionalText(iotDevice.getLocation()));
    }

    /** 修改时如果关键必填字段未传，则沿用数据库旧值，支持局部更新。 */
    private void normalizeUpdateFields(IotDevice iotDevice, IotDevice oldDevice) {
        Long newPlotId = iotDevice.getPlotId() == null ? oldDevice.getPlotId() : iotDevice.getPlotId();
        boolean plotChanged = !newPlotId.equals(oldDevice.getPlotId());
        iotDevice.setDeviceCode(StringUtils.hasText(iotDevice.getDeviceCode())
                ? iotDevice.getDeviceCode().trim() : oldDevice.getDeviceCode());
        iotDevice.setName(StringUtils.hasText(iotDevice.getName())
                ? iotDevice.getName().trim() : oldDevice.getName());
        iotDevice.setTypeId(iotDevice.getTypeId() == null ? oldDevice.getTypeId() : iotDevice.getTypeId());
        iotDevice.setPlotId(newPlotId);
        iotDevice.setControlStatus(iotDevice.getControlStatus() == null ? oldDevice.getControlStatus() : iotDevice.getControlStatus());
        iotDevice.setOnlineStatus(iotDevice.getOnlineStatus() == null ? oldDevice.getOnlineStatus() : iotDevice.getOnlineStatus());
        iotDevice.setHealthStatus(iotDevice.getHealthStatus() == null ? oldDevice.getHealthStatus() : iotDevice.getHealthStatus());
        iotDevice.setInstallTime(iotDevice.getInstallTime() == null ? oldDevice.getInstallTime() : iotDevice.getInstallTime());
        iotDevice.setLocation(iotDevice.getLocation() == null && !plotChanged
                ? oldDevice.getLocation()
                : normalizeOptionalText(iotDevice.getLocation()));
    }

    /** 校验设备编码在全表唯一。 */
    private void checkUniqueDeviceCode(IotDevice iotDevice) {
        if (StringUtils.hasText(iotDevice.getDeviceCode())
                && iotDeviceMapper.countByDeviceCode(iotDevice.getDeviceCode(), iotDevice.getId()) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备编码已存在");
        }
    }

    /** 为故障设备生成一条未完成故障单；已有待处理或处理中故障单时不重复生成。 */
    private void ensureOpenFaultForFaultDevice(IotDevice device) {
        if (device == null || device.getId() == null || !Integer.valueOf(HEALTH_STATUS_FAULT).equals(device.getHealthStatus())) {
            return;
        }
        if (iotDeviceFaultMapper.selectOpenByDeviceId(device.getId()) != null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = device.getFaultTime() == null ? now : device.getFaultTime();
        Long handleUserId = userMapper.selectBoundTechnicianIdByOwnerId(device.getUserId());
        IotDeviceFault fault = new IotDeviceFault();
        fault.setDeviceId(device.getId());
        fault.setFaultCode(buildAutoFaultCode(device.getId(), startTime));
        fault.setFaultName(defaultText(device.getName(), "未知设备") + "设备故障");
        fault.setFaultType(AUTO_FAULT_TYPE_OTHER);
        fault.setSeverity(AUTO_FAULT_SEVERITY_HIGH);
        fault.setFaultDesc(buildAutoFaultDesc(device));
        fault.setStartTime(startTime);
        fault.setStatus(FAULT_STATUS_PENDING);
        fault.setAssignStatus(handleUserId == null ? 0 : 1);
        fault.setHandleUserId(handleUserId);
        iotDeviceFaultMapper.insert(fault);
        maintenanceMessageService.publishForFault(fault.getId());
    }

    /** 自动故障编码使用设备ID和故障开始时间，保证可读且长度稳定。 */
    private String buildAutoFaultCode(Long deviceId, LocalDateTime startTime) {
        return "DF_" + deviceId + "_" + startTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String buildAutoFaultDesc(IotDevice device) {
        return "设备健康状态已标记为故障，系统自动生成故障记录";
    }

    /** 生成 D_001、D_002 这类设备编码，并保留唯一性兜底检查。 */
    private String generateDeviceCode() {
        int nextNumber = iotDeviceMapper.selectMaxGeneratedDeviceCodeNumber() + 1;
        String candidate;
        do {
            candidate = GENERATED_DEVICE_CODE_PREFIX + String.format("%03d", nextNumber);
            nextNumber++;
        } while (iotDeviceMapper.countByDeviceCode(candidate, null) > 0);
        return candidate;
    }

    /** 确认设备存在，供删除前置校验使用。 */
    private void checkDeviceExists(Long id) {
        if (iotDeviceMapper.selectById(id) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备不存在");
        }
    }

    /** 校验设备关联地块是否存在，并返回地块信息供位置补齐使用。 */
    private Plot checkPlotExists(Long plotId) {
        if (plotId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "关联地块不能为空");
        }
        Plot plot = plotMapper.selectById(plotId);
        if (plot == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "关联地块不存在");
        }
        return plot;
    }

    /** 如果调用方没有填写安装位置，则默认使用关联地块名称，保证设备位置可展示。 */
    private void fillLocationFromPlotIfBlank(IotDevice iotDevice, Plot plot) {
        if (!StringUtils.hasText(iotDevice.getLocation()) && plot != null) {
            iotDevice.setLocation(plot.getPlotName());
        }
    }

    /** 删除前检查业务表引用，保护历史监测数据和摄像头记录。 */
    private void checkNoReferences(Long id) {
        if (iotDeviceMapper.countReferencesByDeviceId(id) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备已存在业务数据引用，不能删除");
        }
    }

    /** 校验设备控制状态枚举。 */
    private void validateControlStatus(Integer controlStatus) {
        if (controlStatus != null && controlStatus != CONTROL_STATUS_OFF && controlStatus != CONTROL_STATUS_ON) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备控制状态只能为0或1");
        }
    }

    /** 故障和维护状态下强制关闭控制状态并置为离线，保证页面和设备控制链路状态一致。 */
    private void applyUnhealthyControlRule(IotDevice iotDevice) {
        if (isUnhealthy(iotDevice.getHealthStatus())) {
            iotDevice.setControlStatus(CONTROL_STATUS_OFF);
            iotDevice.setOnlineStatus(ONLINE_STATUS_OFFLINE);
        }
    }

    /** 判断健康状态是否属于不能开启设备的状态。 */
    private boolean isUnhealthy(Integer healthStatus) {
        return healthStatus != null && (healthStatus == HEALTH_STATUS_FAULT || healthStatus == HEALTH_STATUS_MAINTENANCE);
    }

    /** 构造健康异常联动关闭设备的局部更新对象。 */
    private IotDevice buildUnhealthyStatusUpdate(Long id, Integer healthStatus) {
        IotDevice iotDevice = new IotDevice();
        iotDevice.setId(id);
        iotDevice.setHealthStatus(healthStatus);
        iotDevice.setControlStatus(CONTROL_STATUS_OFF);
        iotDevice.setOnlineStatus(ONLINE_STATUS_OFFLINE);
        return iotDevice;
    }

    /** 校验设备在线状态枚举。 */
    private void validateOnlineStatus(Integer onlineStatus) {
        if (onlineStatus != null && onlineStatus != ONLINE_STATUS_OFFLINE && onlineStatus != ONLINE_STATUS_ONLINE) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备在线状态只能为0或1");
        }
    }

    /** 校验设备健康状态枚举。 */
    private void validateHealthStatus(Integer healthStatus) {
        if (healthStatus != null
                && healthStatus != HEALTH_STATUS_NORMAL
                && healthStatus != HEALTH_STATUS_FAULT
                && healthStatus != HEALTH_STATUS_MAINTENANCE) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备健康状态只能为0、1或2");
        }
    }

    /** 列表筛选时校验设备类型是否存在，避免传入不存在的类型ID。 */
    private void validateTypeExists(Long typeId) {
        if (typeId != null && deviceTypeMapper.selectById(typeId) == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备类型不存在");
        }
    }

    /** 新增和修改设备时要求设备类型存在且已启用。 */
    private void validateEnabledTypeId(Long typeId) {
        if (typeId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备类型不能为空");
        }
        DeviceType deviceType = deviceTypeMapper.selectById(typeId);
        if (deviceType == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备类型不存在");
        }
        if (deviceType.getStatus() == null || deviceType.getStatus() != 1) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备类型已禁用");
        }
    }

    /** 必填整数校验，空值会抛出业务异常。 */
    private void requireInteger(Integer value, String message) {
        if (value == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
    }

    /** 必填字符串标准化，空字符串会抛出业务异常。 */
    private String normalizeRequiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
        return value.trim();
    }

    /** 可选字符串标准化，空字符串统一转成 null，方便动态 SQL 忽略空筛选。 */
    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 默认整数值补齐，调用方未传时使用指定默认值。 */
    private Integer defaultInteger(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    /** 公共ID非空校验。 */
    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备ID不能为空");
        }
    }
}
