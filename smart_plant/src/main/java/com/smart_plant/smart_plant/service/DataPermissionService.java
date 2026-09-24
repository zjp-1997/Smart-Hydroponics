package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.entity.IotDevice;
import com.smart_plant.smart_plant.entity.IotDeviceFault;
import com.smart_plant.smart_plant.entity.User;

public interface DataPermissionService {

    User currentUser();

    /** 农场主返回本人ID，普通用户返回绑定农场主ID；其他角色无权使用农场工作台。 */
    Long currentClientOwnerId();

    /** 校验农场只读资源是否属于当前移动端账号可见的农场主。 */
    void requireClientFarmReader(Long ownerId);

    boolean isAdmin();

    boolean isTechnician();

    Long restrictUserId(Long requestedUserId);

    Long restrictFaultHandlerId(Long requestedHandleUserId);

    void requireOwnedResource(Long resourceUserId);

    void requireFarmManager(Long resourceUserId);

    void requireAgriculturalOperator(Long resourceUserId);

    void requireDeviceManager(IotDevice device);

    void requireSensorDataReader(IotDevice device);

    void requireSensorDataManager(IotDevice device);

    void requireFaultManager(IotDevice device);

    void requireFaultSolver(IotDeviceFault fault, IotDevice device);
}
