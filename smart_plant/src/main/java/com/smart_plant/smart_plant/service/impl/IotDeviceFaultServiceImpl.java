package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.IotDevice;
import com.smart_plant.smart_plant.entity.IotDeviceFault;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.IotDeviceFaultMapper;
import com.smart_plant.smart_plant.mapper.IotDeviceMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.MaintenanceMessageService;
import com.smart_plant.smart_plant.service.IotDeviceFaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 设备故障管理业务实现。
 */
@Service
@RequiredArgsConstructor
public class IotDeviceFaultServiceImpl implements IotDeviceFaultService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_PROCESSING = 1;
    private static final int STATUS_HANDLED = 2;
    private static final int STATUS_CLOSED = 3;
    private static final int ASSIGN_STATUS_UNASSIGNED = 0;
    private static final int ASSIGN_STATUS_ASSIGNED = 1;
    private static final int ASSIGN_STATUS_ACCEPTED = 2;
    private static final int ASSIGN_STATUS_REJECTED = 3;
    private static final int CONTROL_STATUS_OFF = 0;
    private static final int ONLINE_STATUS_OFFLINE = 0;
    private static final int HEALTH_STATUS_FAULT = 1;
    /** 自动生成的设备故障使用管理端枚举中的“其他故障”。 */
    private static final int AUTO_FAULT_TYPE_OTHER = 6;
    private static final int AUTO_FAULT_SEVERITY_HIGH = 3;

    private final IotDeviceFaultMapper faultMapper;
    private final IotDeviceMapper deviceMapper;
    private final UserMapper userMapper;
    private final DataPermissionService dataPermissionService;

    /** 故障落库后在同一事务中生成维护消息及角色通知。 */
    private final MaintenanceMessageService maintenanceMessageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IotDeviceFault addFault(IotDeviceFault fault) {
        validateCreate(fault);
        normalizeDefaults(fault);
        IotDevice device = getDeviceRaw(fault.getDeviceId());
        dataPermissionService.requireFaultManager(device);
        autoAssignHandlerIfBlank(fault, device.getUserId());
        validateHandlerIfPresent(fault.getHandleUserId(), device.getUserId());
        faultMapper.insert(fault);
        maintenanceMessageService.publishForFault(fault.getId());
        markDeviceFault(fault.getDeviceId());
        return faultMapper.selectById(fault.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFault(Long id) {
        requireId(id);
        IotDeviceFault oldFault = getFaultById(id);
        dataPermissionService.requireFaultManager(getDeviceRaw(oldFault.getDeviceId()));
        int rows = faultMapper.softDeleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备故障不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteFaults(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的故障记录");
        }
        for (Long id : ids) {
            requireId(id);
            IotDeviceFault oldFault = getFaultById(id);
            dataPermissionService.requireFaultManager(getDeviceRaw(oldFault.getDeviceId()));
        }
        return faultMapper.softDeleteBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IotDeviceFault updateFault(IotDeviceFault fault) {
        if (fault == null || fault.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备故障ID不能为空");
        }
        IotDeviceFault oldFault = faultMapper.selectById(fault.getId());
        if (oldFault == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备故障不存在");
        }
        dataPermissionService.requireFaultManager(getDeviceRaw(oldFault.getDeviceId()));
        IotDevice targetDevice = fault.getDeviceId() == null ? getDeviceRaw(oldFault.getDeviceId()) : getDeviceRaw(fault.getDeviceId());
        if (fault.getDeviceId() != null) {
            dataPermissionService.requireFaultManager(targetDevice);
        }
        validateUpdate(fault);
        normalizeAssignmentForUpdate(fault, oldFault);
        if (fault.getHandleResult() != null) {
            fault.setHandleResult(normalizeOptionalText(fault.getHandleResult()));
        }
        validateHandlerIfPresent(fault.getHandleUserId(), targetDevice.getUserId());
        validateLifecycleTransition(oldFault, fault.getStatus(),
                fault.getHandleUserId(), fault.getAssignStatus(), fault.getHandleResult());
        int rows = faultMapper.updateById(fault);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "设备故障修改失败");
        }
        if (!Integer.valueOf(STATUS_HANDLED).equals(oldFault.getStatus())
                && Integer.valueOf(STATUS_HANDLED).equals(fault.getStatus())) {
            restoreDeviceAfterFaultResolved(oldFault.getDeviceId());
        }
        return faultMapper.selectById(fault.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status, String handleResult) {
        requireId(id);
        IotDeviceFault oldFault = getFaultById(id);
        dataPermissionService.requireFaultSolver(oldFault, getDeviceRaw(oldFault.getDeviceId()));
        validateStatus(status);
        String normalizedResult = normalizeOptionalText(handleResult);
        validateLifecycleTransition(oldFault, status, null, null, normalizedResult);
        int rows = faultMapper.updateStatus(id, status, normalizedResult);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备故障不存在");
        }
        if (STATUS_HANDLED == status) {
            restoreDeviceAfterFaultResolved(oldFault.getDeviceId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignFault(Long id, Long handleUserId) {
        requireId(id);
        IotDeviceFault oldFault = getFaultById(id);
        IotDevice device = getDeviceRaw(oldFault.getDeviceId());
        dataPermissionService.requireFaultManager(device);
        validateHandler(handleUserId, device.getUserId());
        validateAssignable(oldFault);
        int rows = faultMapper.updateAssignee(id, handleUserId);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备故障不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptAssignment(Long id) {
        requireId(id);
        IotDeviceFault oldFault = getFaultById(id);
        requireCurrentAssignedTechnician(oldFault);
        validateAssignable(oldFault);
        if (Integer.valueOf(ASSIGN_STATUS_REJECTED).equals(oldFault.getAssignStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该故障已拒绝接单，请重新分配");
        }
        int rows = faultMapper.updateAssignmentStatus(id, ASSIGN_STATUS_ACCEPTED, STATUS_PROCESSING, null);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备故障不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectAssignment(Long id, String rejectReason) {
        requireId(id);
        IotDeviceFault oldFault = getFaultById(id);
        requireCurrentAssignedTechnician(oldFault);
        validateAssignable(oldFault);
        int rows = faultMapper.updateAssignmentStatus(
                id,
                ASSIGN_STATUS_REJECTED,
                STATUS_PENDING,
                normalizeOptionalText(rejectReason));
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备故障不存在");
        }
    }

    @Override
    public IotDeviceFault getFaultById(Long id) {
        requireId(id);
        IotDeviceFault fault = faultMapper.selectById(id);
        if (fault == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备故障不存在");
        }
        dataPermissionService.requireFaultSolver(fault, getDeviceRaw(fault.getDeviceId()));
        return fault;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageInfo<IotDeviceFault> listFaults(Long deviceId, String deviceName, String faultCode, String faultName,
                                               Integer faultType, Integer severity, Integer status, Long handleUserId,
                                               Integer pageNum, Integer pageSize) {
        validateFaultType(faultType);
        validateSeverity(severity);
        validateStatus(status);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = null;
        if (!dataPermissionService.isAdmin() && !dataPermissionService.isTechnician()) {
            IotDevice scopeDevice = new IotDevice();
            scopeDevice.setUserId(dataPermissionService.currentUser().getId());
            dataPermissionService.requireFaultManager(scopeDevice);
            scopedUserId = dataPermissionService.currentUser().getId();
        }
        Long scopedHandleUserId = dataPermissionService.restrictFaultHandlerId(handleUserId);
        syncMissingFaultsForUnhealthyDevices(scopedUserId);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(faultMapper.selectList(
                scopedUserId,
                deviceId,
                normalizeOptionalText(deviceName),
                normalizeOptionalText(faultCode),
                normalizeOptionalText(faultName),
                faultType,
                severity,
                status,
                scopedHandleUserId));
    }

    private void validateCreate(IotDeviceFault fault) {
        if (fault == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备故障信息不能为空");
        }
        if (fault.getDeviceId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备不能为空");
        }
        if (!StringUtils.hasText(fault.getFaultCode())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "故障编码不能为空");
        }
        if (!StringUtils.hasText(fault.getFaultName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "故障名称不能为空");
        }
        if (fault.getFaultType() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "故障类型不能为空");
        }
    }

    private void validateUpdate(IotDeviceFault fault) {
        validateFaultType(fault.getFaultType());
        validateSeverity(fault.getSeverity());
        validateStatus(fault.getStatus());
    }

    private void normalizeDefaults(IotDeviceFault fault) {
        fault.setFaultCode(normalizeRequiredText(fault.getFaultCode(), "故障编码不能为空"));
        fault.setFaultName(normalizeRequiredText(fault.getFaultName(), "故障名称不能为空"));
        fault.setFaultDesc(normalizeOptionalText(fault.getFaultDesc()));
        fault.setHandleResult(normalizeOptionalText(fault.getHandleResult()));
        fault.setSeverity(fault.getSeverity() == null ? 1 : fault.getSeverity());
        fault.setStatus(fault.getStatus() == null ? STATUS_PENDING : fault.getStatus());
        fault.setAssignStatus(resolveCreateAssignStatus(fault));
        fault.setStartTime(fault.getStartTime() == null ? LocalDateTime.now() : fault.getStartTime());
        validateUpdate(fault);
        if (fault.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "新增故障只能处于待处理状态");
        }
    }

    private IotDevice getDeviceRaw(Long deviceId) {
        IotDevice device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "关联设备不存在");
        }
        return device;
    }

    private void markDeviceFault(Long deviceId) {
        IotDevice device = new IotDevice();
        device.setId(deviceId);
        device.setHealthStatus(HEALTH_STATUS_FAULT);
        device.setControlStatus(CONTROL_STATUS_OFF);
        device.setOnlineStatus(ONLINE_STATUS_OFFLINE);
        deviceMapper.updateById(device);
    }

    private void autoAssignHandlerIfBlank(IotDeviceFault fault, Long ownerId) {
        if (fault.getHandleUserId() != null) {
            return;
        }
        Long handlerId = userMapper.selectBoundTechnicianIdByOwnerId(ownerId);
        if (handlerId != null) {
            fault.setHandleUserId(handlerId);
            fault.setAssignStatus(ASSIGN_STATUS_ASSIGNED);
            fault.setStatus(STATUS_PENDING);
        }
    }

    /** 将健康状态为故障但未建单的设备补偿同步到故障列表。 */
    private void syncMissingFaultsForUnhealthyDevices(Long scopedUserId) {
        List<IotDevice> devices = deviceMapper.selectUnhealthyDevicesWithoutOpenFault(scopedUserId);
        for (IotDevice device : devices) {
            createAutoFaultIfAbsent(device);
        }
    }

    private void createAutoFaultIfAbsent(IotDevice device) {
        if (device == null || device.getId() == null || faultMapper.selectOpenByDeviceId(device.getId()) != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = device.getFaultTime() == null ? now : device.getFaultTime();
        Long handlerId = userMapper.selectBoundTechnicianIdByOwnerId(device.getUserId());

        IotDeviceFault fault = new IotDeviceFault();
        fault.setDeviceId(device.getId());
        fault.setFaultCode(buildAutoFaultCode(device.getId(), startTime));
        fault.setFaultName(defaultText(device.getName(), "未知设备") + "设备故障");
        fault.setFaultType(AUTO_FAULT_TYPE_OTHER);
        fault.setSeverity(AUTO_FAULT_SEVERITY_HIGH);
        fault.setFaultDesc(buildAutoFaultDesc(device));
        fault.setStartTime(startTime);
        fault.setHandleUserId(handlerId);
        fault.setAssignStatus(handlerId == null ? ASSIGN_STATUS_UNASSIGNED : ASSIGN_STATUS_ASSIGNED);
        fault.setStatus(STATUS_PENDING);
        faultMapper.insert(fault);
        maintenanceMessageService.publishForFault(fault.getId());
    }

    private String buildAutoFaultCode(Long deviceId, LocalDateTime startTime) {
        return "DF_" + deviceId + "_" + startTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String buildAutoFaultDesc(IotDevice device) {
        return "设备健康状态已标记为故障，系统自动生成故障记录";
    }

    private void validateHandlerIfPresent(Long userId, Long ownerId) {
        if (userId != null) {
            validateHandler(userId, ownerId);
        }
    }

    private void validateHandler(Long userId, Long ownerId) {
        if (userId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "处理人不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "处理人不存在");
        }
        if (!"technician".equals(user.getRoleCode())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "故障只能分配给技术人员");
        }
        if (userMapper.countActiveTechnicianBinding(ownerId, userId) == 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "处理人必须是该农场主绑定的技术人员");
        }
    }

    private int resolveCreateAssignStatus(IotDeviceFault fault) {
        if (fault.getHandleUserId() == null) {
            return ASSIGN_STATUS_UNASSIGNED;
        }
        if (fault.getAssignStatus() == null || fault.getAssignStatus() == ASSIGN_STATUS_UNASSIGNED) {
            return ASSIGN_STATUS_ASSIGNED;
        }
        validateAssignStatus(fault.getAssignStatus());
        return fault.getAssignStatus();
    }

    private void normalizeAssignmentForUpdate(IotDeviceFault fault, IotDeviceFault oldFault) {
        if (fault.getAssignStatus() != null) {
            validateAssignStatus(fault.getAssignStatus());
        }
        if (fault.getHandleUserId() == null || fault.getHandleUserId().equals(oldFault.getHandleUserId())) {
            return;
        }
        fault.setAssignStatus(ASSIGN_STATUS_ASSIGNED);
        if (oldFault.getStatus() == STATUS_PENDING || oldFault.getStatus() == STATUS_PROCESSING) {
            fault.setStatus(STATUS_PENDING);
        }
    }

    private void requireCurrentAssignedTechnician(IotDeviceFault fault) {
        if (!dataPermissionService.isTechnician()
                || fault.getHandleUserId() == null
                || !fault.getHandleUserId().equals(dataPermissionService.currentUser().getId())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "只有当前分配的技术人员可以接受或拒绝任务");
        }
    }

    private void validateAssignable(IotDeviceFault fault) {
        if (fault.getStatus() != null && (fault.getStatus() == STATUS_HANDLED || fault.getStatus() == STATUS_CLOSED)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "已处理或已关闭的故障不能调整分配");
        }
    }

    /**
     * 最后一条开放故障完成后恢复设备；存在其他开放故障时继续保持故障离线状态。
     */
    private void restoreDeviceAfterFaultResolved(Long deviceId) {
        int restoredRows = deviceMapper.restoreOnlineAfterFaultResolved(deviceId);
        if (restoredRows == 0 && faultMapper.selectOpenByDeviceId(deviceId) == null) {
            throw new BusinessException(ResponseCode.FAIL, "故障已完成，但设备在线状态恢复失败");
        }
    }

    /** 保证故障只能在处理人接单并填写结果后完成，且完成后才能关闭。 */
    private void validateLifecycleTransition(IotDeviceFault oldFault, Integer requestedStatus,
                                             Long requestedHandlerId, Integer requestedAssignStatus,
                                             String requestedResult) {
        int oldStatus = oldFault.getStatus() == null ? STATUS_PENDING : oldFault.getStatus();
        int targetStatus = requestedStatus == null ? oldStatus : requestedStatus;
        Long handlerId = requestedHandlerId == null ? oldFault.getHandleUserId() : requestedHandlerId;
        Integer assignStatus = requestedAssignStatus == null ? oldFault.getAssignStatus() : requestedAssignStatus;
        String result = StringUtils.hasText(requestedResult) ? requestedResult.trim() : oldFault.getHandleResult();

        if (oldStatus == STATUS_CLOSED && targetStatus != STATUS_CLOSED) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "已关闭的故障不能重新流转");
        }
        if (oldStatus == STATUS_HANDLED && targetStatus != STATUS_HANDLED && targetStatus != STATUS_CLOSED) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "已处理的故障只能继续关闭");
        }
        if (targetStatus == STATUS_PROCESSING
                && (handlerId == null || !Integer.valueOf(ASSIGN_STATUS_ACCEPTED).equals(assignStatus))) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "处理人接受任务后才能进入处理中");
        }
        if (targetStatus == STATUS_HANDLED
                && (handlerId == null || !Integer.valueOf(ASSIGN_STATUS_ACCEPTED).equals(assignStatus)
                || !StringUtils.hasText(result))) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "完成故障前必须由处理人接单并填写处理结果");
        }
        if (targetStatus == STATUS_CLOSED && oldStatus != STATUS_HANDLED && oldStatus != STATUS_CLOSED) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "故障处理完成后才能关闭");
        }
    }

    private void validateAssignStatus(Integer assignStatus) {
        if (assignStatus != null
                && assignStatus != ASSIGN_STATUS_UNASSIGNED
                && assignStatus != ASSIGN_STATUS_ASSIGNED
                && assignStatus != ASSIGN_STATUS_ACCEPTED
                && assignStatus != ASSIGN_STATUS_REJECTED) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "分配状态只能为0、1、2或3");
        }
    }

    private void validateFaultType(Integer faultType) {
        if (faultType != null && (faultType < 1 || faultType > 6)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "故障类型只能为1到6");
        }
    }

    private void validateSeverity(Integer severity) {
        if (severity != null && (severity < 1 || severity > 4)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "严重程度只能为1到4");
        }
    }

    private void validateStatus(Integer status) {
        if (status != null && status != STATUS_PENDING && status != STATUS_PROCESSING
                && status != STATUS_HANDLED && status != STATUS_CLOSED) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "故障状态只能为0、1、2或3");
        }
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "ID不能为空");
        }
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

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
