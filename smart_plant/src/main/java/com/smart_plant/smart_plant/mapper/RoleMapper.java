package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper {

    int insert(Role role);

    int deleteById(Long id);

    int deleteBatchByIds(@Param("ids") List<Long> ids);

    int logicalDeleteById(Long id);

    int logicalDeleteBatchByIds(@Param("ids") List<Long> ids);

    int updateById(Role role);

    Role selectById(Long id);

    Role selectByRoleCode(String roleCode);

    List<Role> selectList(@Param("roleName") String roleName,
                          @Param("roleCode") String roleCode,
                          @Param("status") Integer status);

    int countByRoleName(@Param("roleName") String roleName,
                        @Param("excludeId") Long excludeId);

    int countByRoleCode(@Param("roleCode") String roleCode,
                        @Param("excludeId") Long excludeId);
}
