package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 覆盖普通用户绑定农场的数据范围和跨农场访问拒绝逻辑。 */
class ClientWorkerScopeTest {
    private final UserMapper userMapper = mock(UserMapper.class);
    private final DataPermissionServiceImpl permissions = new DataPermissionServiceImpl(userMapper);

    @AfterEach
    void clearUser() {
        CurrentUserContext.clear();
    }

    @Test
    void workerUsesBoundOwnerAndCannotReadOtherFarm() {
        User worker = user(22L, "user");
        CurrentUserContext.set(worker);
        when(userMapper.selectBoundOwnerIdByUserId(22L)).thenReturn(8L);

        assertEquals(8L, permissions.currentClientOwnerId());
        permissions.requireClientFarmReader(8L);
        assertThrows(BusinessException.class, () -> permissions.requireClientFarmReader(9L));
    }

    @Test
    void missingBindingAndExpertRoleAreRejected() {
        CurrentUserContext.set(user(22L, "user"));
        assertThrows(BusinessException.class, permissions::currentClientOwnerId);
        CurrentUserContext.set(user(23L, "expert"));
        assertThrows(BusinessException.class, permissions::currentClientOwnerId);
    }

    /** 测试只构造权限判断需要的用户身份字段。 */
    private User user(Long id, String roleCode) {
        User user = new User();
        user.setId(id);
        user.setRoleCode(roleCode);
        return user;
    }
}
