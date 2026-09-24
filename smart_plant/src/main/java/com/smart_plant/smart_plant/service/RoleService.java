package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.RoleDetailResponse;
import com.smart_plant.smart_plant.dto.MenuPermissionTreeNode;
import com.smart_plant.smart_plant.entity.Permission;
import com.smart_plant.smart_plant.entity.Role;

import java.util.List;

public interface RoleService {

    Role addRole(Role role);

    Role updateRole(Role role);

    void deleteRole(Long id);

    int deleteRoles(List<Long> ids);

    Role getRoleById(Long id);

    PageInfo<Role> listRoles(String roleName, String roleCode, Integer pageNum, Integer pageSize);

    PageInfo<Role> listRoles(String roleName, String roleCode, Integer status, Integer pageNum, Integer pageSize);

    RoleDetailResponse getRoleDetail(Long id);

    void assignPermissions(Long id, List<Long> permissionIds);

    List<Permission> listRolePermissions(Long id);

    List<MenuPermissionTreeNode> listAssignableMenuTree();
}
