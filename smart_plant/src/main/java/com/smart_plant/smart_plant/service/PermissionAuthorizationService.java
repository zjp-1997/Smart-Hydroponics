package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.entity.User;

import java.util.List;

public interface PermissionAuthorizationService {

    String resolveDataScope(User user);

    List<String> resolvePermissions(User user);

    boolean hasAnyPermission(User user, String... permissionCodes);
}
