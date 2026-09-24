package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.RoleDetailResponse;
import com.smart_plant.smart_plant.dto.MenuPermissionTreeNode;
import com.smart_plant.smart_plant.entity.Permission;
import com.smart_plant.smart_plant.entity.Role;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.RoleMapper;
import com.smart_plant.smart_plant.mapper.RolePermissionMapper;
import com.smart_plant.smart_plant.mapper.PermissionMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import com.smart_plant.smart_plant.service.RoleService;
import com.smart_plant.smart_plant.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private static final Set<String> SYSTEM_ROLE_CODES = Set.of(
            "admin", "farm_owner", "technician", "expert", "user"
    );

    private final RoleMapper roleMapper;

    private final RolePermissionMapper rolePermissionMapper;

    private final PermissionMapper permissionMapper;

    private final UserMapper userMapper;

    private final OperationLogService operationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Role addRole(Role role) {
        requireAdmin();
        validateCreateRole(role);
        normalizeRole(role);
        checkUnique(role);
        role.setCreateBy(currentUserId());
        role.setUpdateBy(currentUserId());
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        if (role.getSort() == null) {
            role.setSort(100);
        }
        roleMapper.insert(role);
        operationLogService.record("创建角色：" + role.getRoleCode());
        return roleMapper.selectById(role.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Role updateRole(Role role) {
        requireAdmin();
        if (role == null || role.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "角色ID不能为空");
        }
        Role oldRole = roleMapper.selectById(role.getId());
        if (oldRole == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "角色不存在");
        }
        if (isSystemRole(oldRole) && StringUtils.hasText(role.getRoleCode())
                && !oldRole.getRoleCode().equals(role.getRoleCode().trim())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "系统默认角色编码不允许修改");
        }
        normalizeRole(role);
        checkUnique(role);
        role.setUpdateBy(currentUserId());
        int rows = roleMapper.updateById(role);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "角色修改失败");
        }
        operationLogService.record("修改角色：" + oldRole.getRoleCode());
        return roleMapper.selectById(role.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        requireAdmin();
        requireId(id);
        Role role = getRoleById(id);
        if (isSystemRole(role)) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "系统默认角色不允许删除");
        }
        if (userMapper.countByRoleId(id) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "角色已关联用户，不能删除");
        }
        rolePermissionMapper.deleteByRoleId(id);
        int rows = roleMapper.logicalDeleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "角色不存在");
        }
        operationLogService.record("删除角色：" + role.getRoleCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoles(List<Long> ids) {
        requireAdmin();
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的角色");
        }
        for (Long id : ids) {
            Role role = roleMapper.selectById(id);
            if (role != null && isSystemRole(role)) {
                throw new BusinessException(ResponseCode.FORBIDDEN, "系统默认角色不允许删除");
            }
        }
        if (userMapper.countByRoleIds(ids) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "存在已关联用户的角色，不能删除");
        }
        ids.forEach(rolePermissionMapper::deleteByRoleId);
        int rows = roleMapper.logicalDeleteBatchByIds(ids);
        operationLogService.record("批量删除角色：" + ids);
        return rows;
    }

    @Override
    public Role getRoleById(Long id) {
        requireId(id);
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    @Override
    public PageInfo<Role> listRoles(String roleName, String roleCode, Integer pageNum, Integer pageSize) {
        return listRoles(roleName, roleCode, null, pageNum, pageSize);
    }

    @Override
    public PageInfo<Role> listRoles(String roleName, String roleCode, Integer status, Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(roleMapper.selectList(roleName, roleCode, status));
    }

    @Override
    public RoleDetailResponse getRoleDetail(Long id) {
        Role role = getRoleById(id);
        return new RoleDetailResponse(role, listAssignableMenuPermissions(id), userMapper.countByRoleId(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long id, List<Long> permissionIds) {
        requireAdmin();
        requireId(id);
        Role role = getRoleById(id);
        rolePermissionMapper.deleteByRoleId(id);
        List<Long> menuPermissionIds = normalizeMenuPermissionIds(permissionIds);
        if (!menuPermissionIds.isEmpty()) {
            rolePermissionMapper.insertBatch(id, menuPermissionIds);
        }
        operationLogService.record("分配角色权限：" + role.getRoleCode());
    }

    @Override
    public List<Permission> listRolePermissions(Long id) {
        requireId(id);
        return listAssignableMenuPermissions(id);
    }

    @Override
    public List<MenuPermissionTreeNode> listAssignableMenuTree() {
        List<Permission> permissions = permissionMapper.selectEnabledMenus();
        Map<Long, MenuPermissionTreeNode> nodes = new LinkedHashMap<>();
        permissions.forEach(permission -> nodes.put(permission.getId(), toTreeNode(permission)));

        List<MenuPermissionTreeNode> roots = new ArrayList<>();
        permissions.forEach(permission -> {
            MenuPermissionTreeNode node = nodes.get(permission.getId());
            MenuPermissionTreeNode parent = nodes.get(permission.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        });
        return roots;
    }

    private MenuPermissionTreeNode toTreeNode(Permission permission) {
        MenuPermissionTreeNode node = new MenuPermissionTreeNode();
        node.setId(permission.getId());
        node.setParentId(permission.getParentId());
        node.setPermissionName(permission.getPermissionName());
        node.setPermissionCode(permission.getPermissionCode());
        node.setPath(permission.getPath());
        node.setSort(permission.getSort());
        return node;
    }

    private List<Permission> listAssignableMenuPermissions(Long roleId) {
        return rolePermissionMapper.selectPermissionsByRoleId(roleId)
                .stream()
                .filter(permission -> Integer.valueOf(1).equals(permission.getType()))
                .toList();
    }

    private List<Long> normalizeMenuPermissionIds(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return List.of();
        }
        List<Long> normalizedIds = permissionIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (normalizedIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectEnabledMenuIdsByIds(normalizedIds);
    }

    private void validateCreateRole(Role role) {
        if (role == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "角色信息不能为空");
        }
        if (!StringUtils.hasText(role.getRoleName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "角色名称不能为空");
        }
        if (!StringUtils.hasText(role.getRoleCode())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "角色编码不能为空");
        }
    }

    private void normalizeRole(Role role) {
        if (StringUtils.hasText(role.getRoleName())) {
            role.setRoleName(role.getRoleName().trim());
        }
        if (StringUtils.hasText(role.getRoleCode())) {
            role.setRoleCode(normalizeCode(role.getRoleCode()));
        }
        if (role.getDescription() != null) {
            role.setDescription(role.getDescription().trim());
        }
        if (role.getRemark() != null) {
            role.setRemark(role.getRemark().trim());
        }
    }

    private void checkUnique(Role role) {
        Long excludeId = role.getId();
        if (StringUtils.hasText(role.getRoleName()) && roleMapper.countByRoleName(role.getRoleName(), excludeId) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "角色名称已存在");
        }
        if (StringUtils.hasText(role.getRoleCode()) && roleMapper.countByRoleCode(role.getRoleCode(), excludeId) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "角色编码已存在");
        }
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "角色ID不能为空");
        }
    }

    private boolean isSystemRole(Role role) {
        return role != null && SYSTEM_ROLE_CODES.contains(normalizeCode(role.getRoleCode()));
    }

    private String normalizeCode(String roleCode) {
        return roleCode == null ? "" : roleCode.trim().toLowerCase(Locale.ROOT);
    }

    private Long currentUserId() {
        User user = CurrentUserContext.get();
        return user == null ? null : user.getId();
    }

    private void requireAdmin() {
        User user = CurrentUserContext.get();
        if (user == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        if (!"admin".equals(normalizeCode(user.getRoleCode()))) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "无角色管理权限");
        }
    }
}
