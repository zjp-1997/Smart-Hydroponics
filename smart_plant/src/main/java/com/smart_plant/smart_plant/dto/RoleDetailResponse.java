package com.smart_plant.smart_plant.dto;

import com.smart_plant.smart_plant.entity.Permission;
import com.smart_plant.smart_plant.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDetailResponse {

    private Role role;

    private List<Permission> permissions;

    private Integer userCount;
}
