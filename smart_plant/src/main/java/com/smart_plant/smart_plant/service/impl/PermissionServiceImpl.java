package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.Permission;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.PermissionMapper;
import com.smart_plant.smart_plant.mapper.RolePermissionMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import com.smart_plant.smart_plant.service.OperationLogService;
import com.smart_plant.smart_plant.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private static final int MENU_TYPE = 1;

    private final PermissionMapper permissionMapper;

    private final RolePermissionMapper rolePermissionMapper;

    private final OperationLogService operationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Permission addPermission(Permission permission) {
        requireAdmin();
        validatePermission(permission);
        permission.setType(MENU_TYPE);
        normalizePermission(permission);
        checkUnique(permission);
        if (permission.getParentId() == null) {
            permission.setParentId(0L);
        }
        if (permission.getStatus() == null) {
            permission.setStatus(1);
        }
        if (permission.getSort() == null) {
            permission.setSort(100);
        }
        permissionMapper.insert(permission);
        operationLogService.record("创建菜单：" + permission.getPermissionCode());
        return permissionMapper.selectById(permission.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Permission updatePermission(Permission permission) {
        requireAdmin();
        if (permission == null || permission.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "菜单ID不能为空");
        }
        Permission oldPermission = permissionMapper.selectById(permission.getId());
        if (oldPermission == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "菜单不存在");
        }
        if (!Integer.valueOf(MENU_TYPE).equals(oldPermission.getType())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "只能维护菜单类型权限");
        }
        permission.setType(MENU_TYPE);
        normalizePermission(permission);
        checkUnique(permission);
        int rows = permissionMapper.updateById(permission);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "菜单修改失败");
        }
        operationLogService.record("修改菜单：" + permission.getPermissionCode());
        return permissionMapper.selectById(permission.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(Long id) {
        requireAdmin();
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "菜单ID不能为空");
        }
        Permission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "菜单不存在");
        }
        if (!Integer.valueOf(MENU_TYPE).equals(permission.getType())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "只能删除菜单类型权限");
        }
        rolePermissionMapper.deleteByPermissionId(id);
        int rows = permissionMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "菜单不存在");
        }
        operationLogService.record("删除菜单：" + id);
    }

    @Override
    public Permission getPermissionById(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "菜单ID不能为空");
        }
        Permission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "菜单不存在");
        }
        if (!Integer.valueOf(MENU_TYPE).equals(permission.getType())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "只能查看菜单类型权限");
        }
        return permission;
    }

    @Override
    public PageInfo<Permission> listPermissions(String permissionName, String permissionCode, Integer type,
                                                Integer status, Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Integer menuType = type == null ? MENU_TYPE : type;
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(permissionMapper.selectList(permissionName, permissionCode, menuType, status));
    }

    private void validatePermission(Permission permission) {
        if (permission == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "菜单信息不能为空");
        }
        if (!StringUtils.hasText(permission.getPermissionName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "菜单名称不能为空");
        }
        if (!StringUtils.hasText(permission.getPermissionCode())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "菜单编码不能为空");
        }
    }

    private void normalizePermission(Permission permission) {
        if (StringUtils.hasText(permission.getPermissionName())) {
            permission.setPermissionName(permission.getPermissionName().trim());
        }
        if (StringUtils.hasText(permission.getPermissionCode())) {
            permission.setPermissionCode(permission.getPermissionCode().trim().toLowerCase(Locale.ROOT));
        }
        if (permission.getPath() != null) {
            permission.setPath(permission.getPath().trim());
        }
        if (permission.getComponent() != null) {
            permission.setComponent(permission.getComponent().trim());
        }
    }

    private void checkUnique(Permission permission) {
        if (StringUtils.hasText(permission.getPermissionCode())
                && permissionMapper.countByPermissionCode(permission.getPermissionCode(), permission.getId()) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "菜单编码已存在");
        }
    }

    private void requireAdmin() {
        User user = CurrentUserContext.get();
        if (user == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        String roleCode = user.getRoleCode() == null ? "" : user.getRoleCode().trim().toLowerCase(Locale.ROOT);
        if (!"admin".equals(roleCode)) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "无菜单管理权限");
        }
    }
}
