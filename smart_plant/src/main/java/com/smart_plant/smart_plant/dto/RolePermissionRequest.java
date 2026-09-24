package com.smart_plant.smart_plant.dto;

import lombok.Data;

import java.util.List;

@Data
public class RolePermissionRequest {

    private List<Long> permissionIds;
}
