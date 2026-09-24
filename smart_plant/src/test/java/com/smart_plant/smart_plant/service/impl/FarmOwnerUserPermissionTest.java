package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.Role;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.mapper.RoleMapper;
import com.smart_plant.smart_plant.mapper.RolePermissionMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FarmOwnerUserPermissionTest {

    @Test
    void ownerGetsUserManagementWithoutGrantingItToOtherRoles() {
        // 现有部署可能没有用户管理菜单授权，农场主仍能进入受限的员工管理页。
        RoleMapper roleMapper = mock(RoleMapper.class);
        RolePermissionMapper rolePermissionMapper = mock(RolePermissionMapper.class);
        PermissionAuthorizationServiceImpl service = new PermissionAuthorizationServiceImpl(
                roleMapper, rolePermissionMapper);
        Role ownerRole = new Role();
        ownerRole.setId(1L);
        when(roleMapper.selectByRoleCode("farm_owner")).thenReturn(ownerRole);
        when(rolePermissionMapper.selectPermissionsByRoleId(1L)).thenReturn(List.of());

        User owner = new User();
        owner.setRoleCode("farm_owner");
        User technician = new User();
        technician.setRoleCode("technician");

        assertTrue(service.hasAnyPermission(owner, "user:manage"));
        assertFalse(service.hasAnyPermission(technician, "user:manage"));
    }
}
