package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.Permission;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.mapper.RoleMapper;
import com.smart_plant.smart_plant.mapper.RolePermissionMapper;
import com.smart_plant.smart_plant.service.PermissionAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PermissionAuthorizationServiceImpl implements PermissionAuthorizationService {

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_FARM_OWNER = "farm_owner";
    private static final String ADMIN_WILDCARD = "*:*:*";

    private static final List<String> FARM_OWNER_FALLBACK_PERMISSIONS = List.of(
            "home:view",
            "user:manage",
            "farm:manage",
            "plot:manage",
            "map:view",
            "planting_batch:manage",
            "iot_device:manage",
            "iot_device_plan:manage",
            "iot_device_fault:manage",
            "camera_device:manage",
            "camera_image:manage",
            "sensor_data:manage",
            "crop_image:manage",
            "crop:manage",
            "warehouse:manage",
            "operation_log:view",
            "login_log:view",
            "ai_recognition_record:manage",
            "ai_solution:manage",
            "alert_event:manage",
            "message_notice:manage",
            "farm_msg:manage",
            // 数据库权限未配置时仍保留农场主的维护消息入口。
            "maintenance_msg:manage",
            "farm_task:manage"
    );

    private final RoleMapper roleMapper;

    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public String resolveDataScope(User user) {
        if (user != null && ROLE_ADMIN.equals(normalizeRoleCode(user.getRoleCode()))) {
            return "all";
        }
        return "self";
    }

    @Override
    public List<String> resolvePermissions(User user) {
        String roleCode = user == null ? "" : normalizeRoleCode(user.getRoleCode());
        if (ROLE_ADMIN.equals(roleCode)) {
            return List.of(ADMIN_WILDCARD);
        }
        List<String> configuredPermissions = resolveConfiguredPermissions(roleCode);
        if (configuredPermissions != null) {
            if (ROLE_FARM_OWNER.equals(roleCode) && !configuredPermissions.contains("user:manage")) {
                // 农场主始终可打开用户管理；UserService 会把可创建角色和数据范围限定为本农场员工。
                return java.util.stream.Stream.concat(configuredPermissions.stream(),
                        java.util.stream.Stream.of("user:manage")).toList();
            }
            return configuredPermissions;
        }
        if (ROLE_FARM_OWNER.equals(roleCode)) {
            return FARM_OWNER_FALLBACK_PERMISSIONS;
        }
        return List.of();
    }

    @Override
    public boolean hasAnyPermission(User user, String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return true;
        }
        List<String> permissions = resolvePermissions(user);
        if (permissions.contains(ADMIN_WILDCARD)) {
            return true;
        }
        for (String permissionCode : permissionCodes) {
            if (!StringUtils.hasText(permissionCode)) {
                continue;
            }
            String required = permissionCode.trim();
            if (permissions.stream().anyMatch(owned -> matches(owned, required))) {
                return true;
            }
        }
        return false;
    }

    private List<String> resolveConfiguredPermissions(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return null;
        }
        var role = roleMapper.selectByRoleCode(roleCode);
        if (role == null || role.getId() == null) {
            return null;
        }
        return rolePermissionMapper.selectPermissionsByRoleId(role.getId())
                .stream()
                .filter(permission -> permission.getStatus() == null || permission.getStatus() == 1)
                .map(Permission::getPermissionCode)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private boolean matches(String owned, String required) {
        if (!StringUtils.hasText(owned)) {
            return false;
        }
        String normalizedOwned = owned.trim();
        return ADMIN_WILDCARD.equals(normalizedOwned)
                || normalizedOwned.equals(required)
                || moduleManageMatches(normalizedOwned, required)
                || impliedMenuMatches(normalizedOwned, required)
                || wildcardMatches(normalizedOwned, required);
    }

    private boolean moduleManageMatches(String owned, String required) {
        if (!owned.endsWith(":manage") || !required.contains(":")) {
            return false;
        }
        String module = owned.substring(0, owned.length() - ":manage".length());
        return required.startsWith(module + ":");
    }

    private boolean impliedMenuMatches(String owned, String required) {
        return "farm_task:manage".equals(owned) && required.startsWith("farm_task_record:");
    }

    private boolean wildcardMatches(String owned, String required) {
        String[] ownedParts = owned.split(":");
        String[] requiredParts = required.split(":");
        if (ownedParts.length != requiredParts.length) {
            return false;
        }
        for (int index = 0; index < ownedParts.length; index++) {
            if (!"*".equals(ownedParts[index]) && !ownedParts[index].equals(requiredParts[index])) {
                return false;
            }
        }
        return true;
    }

    private String normalizeRoleCode(String roleCode) {
        return StringUtils.hasText(roleCode) ? roleCode.trim().toLowerCase(Locale.ROOT) : "";
    }
}
