package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.MenuPermissionTreeNode;
import com.smart_plant.smart_plant.entity.Permission;
import com.smart_plant.smart_plant.mapper.PermissionMapper;
import com.smart_plant.smart_plant.mapper.RoleMapper;
import com.smart_plant.smart_plant.mapper.RolePermissionMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.service.OperationLogService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleServiceImplTest {

    private final PermissionMapper permissionMapper = mock(PermissionMapper.class);

    private final RoleServiceImpl roleService = new RoleServiceImpl(
            mock(RoleMapper.class),
            mock(RolePermissionMapper.class),
            permissionMapper,
            mock(UserMapper.class),
            mock(OperationLogService.class)
    );

    @Test
    void buildsTwoLevelAssignableMenuTree() {
        Permission group = permission(100L, 0L, "设备管理");
        Permission child = permission(101L, 100L, "设备信息管理");
        Permission directMenu = permission(102L, 0L, "监控管理");
        when(permissionMapper.selectEnabledMenus()).thenReturn(List.of(group, child, directMenu));

        List<MenuPermissionTreeNode> tree = roleService.listAssignableMenuTree();

        assertEquals(List.of("设备管理", "监控管理"),
                tree.stream().map(MenuPermissionTreeNode::getPermissionName).toList());
        assertEquals(List.of("设备信息管理"),
                tree.getFirst().getChildren().stream().map(MenuPermissionTreeNode::getPermissionName).toList());
    }

    private Permission permission(Long id, Long parentId, String name) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setParentId(parentId);
        permission.setPermissionName(name);
        permission.setPermissionCode("menu:" + id);
        return permission;
    }
}
