package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.Permission;

public interface PermissionService {

    Permission addPermission(Permission permission);

    Permission updatePermission(Permission permission);

    void deletePermission(Long id);

    Permission getPermissionById(Long id);

    PageInfo<Permission> listPermissions(String permissionName, String permissionCode, Integer type,
                                         Integer status, Integer pageNum, Integer pageSize);
}
