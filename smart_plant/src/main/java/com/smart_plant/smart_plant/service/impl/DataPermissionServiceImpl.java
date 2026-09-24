package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.IotDevice;
import com.smart_plant.smart_plant.entity.IotDeviceFault;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import com.smart_plant.smart_plant.service.DataPermissionService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DataPermissionServiceImpl implements DataPermissionService {

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_FARM_OWNER = "farm_owner";
    private static final String ROLE_TECHNICIAN = "technician";
    private static final String ROLE_USER = "user";

    /** 普通用户所属农场主只能从注册绑定表读取，不能由客户端参数指定。 */
    private final UserMapper userMapper;

    @Override
    public User currentUser() {
        User user = CurrentUserContext.get();
        if (user == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "Not logged in");
        }
        return user;
    }

    @Override
    public Long currentClientOwnerId() {
        User viewer = currentUser();
        if (hasRole(ROLE_FARM_OWNER)) {
            return viewer.getId();
        }
        if (hasRole(ROLE_USER)) {
            Long ownerId = userMapper.selectBoundOwnerIdByUserId(viewer.getId());
            if (ownerId != null && ownerId > 0) {
                return ownerId;
            }
        }
        throw forbidden();
    }

    @Override
    public void requireClientFarmReader(Long ownerId) {
        if (ownerId == null || !ownerId.equals(currentClientOwnerId())) {
            throw forbidden();
        }
    }

    @Override
    public boolean isAdmin() {
        return hasRole(ROLE_ADMIN);
    }

    @Override
    public boolean isTechnician() {
        return hasRole(ROLE_TECHNICIAN);
    }

    @Override
    public Long restrictUserId(Long requestedUserId) {
        if (isAdmin()) {
            return requestedUserId;
        }
        Long currentUserId = currentUser().getId();
        if (requestedUserId != null && !currentUserId.equals(requestedUserId)) {
            throw forbidden();
        }
        return currentUserId;
    }

    @Override
    public Long restrictFaultHandlerId(Long requestedHandleUserId) {
        if (isAdmin() || hasRole(ROLE_FARM_OWNER)) {
            return requestedHandleUserId;
        }
        if (hasRole(ROLE_TECHNICIAN)) {
            Long currentUserId = currentUser().getId();
            if (requestedHandleUserId != null && !currentUserId.equals(requestedHandleUserId)) {
                throw forbidden();
            }
            return currentUserId;
        }
        return requestedHandleUserId;
    }

    @Override
    public void requireOwnedResource(Long resourceUserId) {
        if (isAdmin()) {
            return;
        }
        if (resourceUserId == null || !currentUser().getId().equals(resourceUserId)) {
            throw forbidden();
        }
    }

    @Override
    public void requireFarmManager(Long resourceUserId) {
        if (isAdmin()) {
            return;
        }
        if (hasRole(ROLE_FARM_OWNER) && currentUser().getId().equals(resourceUserId)) {
            return;
        }
        throw forbidden();
    }

    @Override
    public void requireAgriculturalOperator(Long resourceUserId) {
        if (isAdmin()) {
            return;
        }
        if ((hasRole(ROLE_FARM_OWNER) || hasRole(ROLE_USER)) && currentUser().getId().equals(resourceUserId)) {
            return;
        }
        throw forbidden();
    }

    @Override
    public void requireDeviceManager(IotDevice device) {
        requireFarmManager(device == null ? null : device.getUserId());
    }

    @Override
    public void requireSensorDataReader(IotDevice device) {
        requireOwnedResource(device == null ? null : device.getUserId());
    }

    @Override
    public void requireSensorDataManager(IotDevice device) {
        requireFarmManager(device == null ? null : device.getUserId());
    }

    @Override
    public void requireFaultManager(IotDevice device) {
        requireFarmManager(device == null ? null : device.getUserId());
    }

    @Override
    public void requireFaultSolver(IotDeviceFault fault, IotDevice device) {
        if (isAdmin()) {
            return;
        }
        if (hasRole(ROLE_FARM_OWNER) && device != null && currentUser().getId().equals(device.getUserId())) {
            return;
        }
        if (hasRole(ROLE_TECHNICIAN) && fault != null && currentUser().getId().equals(fault.getHandleUserId())) {
            return;
        }
        throw forbidden();
    }

    private boolean hasRole(String roleCode) {
        return roleCode.equals(normalizeRole(currentUser().getRoleCode()));
    }

    private String normalizeRole(String roleCode) {
        return roleCode == null ? "" : roleCode.trim().toLowerCase(Locale.ROOT);
    }

    private BusinessException forbidden() {
        return new BusinessException(ResponseCode.FORBIDDEN, "No permission to access this resource");
    }
}
