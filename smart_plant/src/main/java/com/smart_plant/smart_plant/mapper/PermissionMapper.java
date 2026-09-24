package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PermissionMapper {

    int insert(Permission permission);

    int updateById(Permission permission);

    int deleteById(Long id);

    Permission selectById(Long id);

    Permission selectByPermissionCode(String permissionCode);

    List<Permission> selectList(@Param("permissionName") String permissionName,
                                @Param("permissionCode") String permissionCode,
                                @Param("type") Integer type,
                                @Param("status") Integer status);

    List<Long> selectEnabledMenuIdsByIds(@Param("ids") List<Long> ids);

    List<Permission> selectEnabledMenus();

    int countByPermissionCode(@Param("permissionCode") String permissionCode,
                              @Param("excludeId") Long excludeId);
}
