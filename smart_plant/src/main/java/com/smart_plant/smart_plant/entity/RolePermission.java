package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {

    private Long id;

    private Long roleId;

    private Long permissionId;

    private LocalDateTime createTime;
}
