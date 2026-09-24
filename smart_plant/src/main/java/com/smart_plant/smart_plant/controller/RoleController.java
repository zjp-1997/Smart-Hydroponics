package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.RoleDetailResponse;
import com.smart_plant.smart_plant.dto.RolePermissionRequest;
import com.smart_plant.smart_plant.dto.MenuPermissionTreeNode;
import com.smart_plant.smart_plant.entity.Permission;
import com.smart_plant.smart_plant.entity.Role;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@RequirePermission("role:manage")
public class RoleController {

    private final RoleService roleService;

    @PostMapping("/add")
    public R<Role> addRole(@RequestBody Role role) {
        return R.success(roleService.addRole(role));
    }

    @PutMapping
    public R<Role> updateRole(@RequestBody Role role) {
        return R.success(roleService.updateRole(role));
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return R.success();
    }

    @DeleteMapping("/batch")
    public R<Integer> deleteRoles(@RequestBody List<Long> ids) {
        return R.success(roleService.deleteRoles(ids));
    }

    @GetMapping("/{id}")
    public R<Role> getRoleById(@PathVariable Long id) {
        return R.success(roleService.getRoleById(id));
    }

    @GetMapping("/{id}/detail")
    public R<RoleDetailResponse> getRoleDetail(@PathVariable Long id) {
        return R.success(roleService.getRoleDetail(id));
    }

    @GetMapping("/{id}/permissions")
    public R<List<Permission>> listRolePermissions(@PathVariable Long id) {
        return R.success(roleService.listRolePermissions(id));
    }

    @GetMapping("/permissions/tree")
    public R<List<MenuPermissionTreeNode>> listAssignableMenuTree() {
        return R.success(roleService.listAssignableMenuTree());
    }

    @PutMapping("/{id}/permissions")
    public R<Void> assignPermissions(@PathVariable Long id, @RequestBody RolePermissionRequest request) {
        roleService.assignPermissions(id, request == null ? List.of() : request.getPermissionIds());
        return R.success();
    }

    @GetMapping("/list")
    public R<PageInfo<Role>> listRoles(@RequestParam(required = false) String roleName,
                                       @RequestParam(required = false) String roleCode,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(roleService.listRoles(roleName, roleCode, status, pageNum, pageSize));
    }
}
