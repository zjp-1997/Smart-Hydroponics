package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.IotDevice;
import com.smart_plant.smart_plant.entity.IotDeviceFault;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/** 验证技术人员读取和处理故障时只能使用服务端确认的本人指派范围。 */
class TechnicianFaultPermissionTest {

    private final DataPermissionServiceImpl permissions = new DataPermissionServiceImpl(mock(UserMapper.class));

    @AfterEach
    void clearUser() {
        CurrentUserContext.clear();
    }

    @Test
    void restrictsTechnicianListAndDetailToAssignedFaults() {
        User technician = new User();
        technician.setId(8L);
        technician.setRoleCode("technician");
        CurrentUserContext.set(technician);
        IotDevice otherOwnerDevice = new IotDevice();
        otherOwnerDevice.setUserId(7L);
        IotDeviceFault ownFault = new IotDeviceFault();
        ownFault.setHandleUserId(8L);
        IotDeviceFault otherFault = new IotDeviceFault();
        otherFault.setHandleUserId(9L);

        // 列表忽略客户端对其他处理人的请求，详情也不因设备所属农场而放宽。
        assertEquals(8L, permissions.restrictFaultHandlerId(null));
        assertThrows(BusinessException.class, () -> permissions.restrictFaultHandlerId(9L));
        permissions.requireFaultSolver(ownFault, otherOwnerDevice);
        assertThrows(BusinessException.class, () -> permissions.requireFaultSolver(otherFault, otherOwnerDevice));
    }
}
