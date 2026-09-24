package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RolePermissionMapper {

    int insertBatch(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);

    int deleteByRoleId(Long roleId);

    int deleteByPermissionId(Long permissionId);

    List<Long> selectPermissionIdsByRoleId(Long roleId);

    List<Permission> selectPermissionsByRoleId(Long roleId);
}
