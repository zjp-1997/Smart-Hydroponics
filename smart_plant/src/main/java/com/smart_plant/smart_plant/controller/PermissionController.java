package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.Permission;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.PermissionService;
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

@RestController
@RequestMapping({"/api/permissions", "/permission"})
@RequiredArgsConstructor
@RequirePermission("permission:manage")
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    public R<Permission> addPermission(@RequestBody Permission permission) {
        return R.success(permissionService.addPermission(permission));
    }

    @PostMapping("/add")
    public R<Permission> addPermissionLegacy(@RequestBody Permission permission) {
        return R.success(permissionService.addPermission(permission));
    }

    @PutMapping("/{id}")
    public R<Permission> updatePermission(@PathVariable Long id, @RequestBody Permission permission) {
        permission.setId(id);
        return R.success(permissionService.updatePermission(permission));
    }

    @PutMapping
    public R<Permission> updatePermissionLegacy(@RequestBody Permission permission) {
        return R.success(permissionService.updatePermission(permission));
    }

    @DeleteMapping("/{id}")
    public R<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return R.success();
    }

    @GetMapping("/{id}")
    public R<Permission> getPermissionById(@PathVariable Long id) {
        return R.success(permissionService.getPermissionById(id));
    }

    @GetMapping
    public R<PageInfo<Permission>> listPermissions(@RequestParam(required = false) String permissionName,
                                                   @RequestParam(required = false) String permissionCode,
                                                   @RequestParam(required = false) Integer type,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(permissionService.listPermissions(permissionName, permissionCode, type, status, pageNum, pageSize));
    }

    @GetMapping("/list")
    public R<PageInfo<Permission>> listPermissionsLegacy(@RequestParam(required = false) String permissionName,
                                                         @RequestParam(required = false) String permissionCode,
                                                         @RequestParam(required = false) Integer type,
                                                         @RequestParam(required = false) Integer status,
                                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(permissionService.listPermissions(permissionName, permissionCode, type, status, pageNum, pageSize));
    }
}
