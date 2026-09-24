package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.CropType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 作物分类持久层接口，负责 crop_type 表的增删改查。
 */
@Mapper
public interface CropTypeMapper {

    /** 新增作物分类 */
    int insert(CropType cropType);

    /** 根据ID删除作物分类 */
    int deleteById(Long id);

    /** 根据ID集合批量删除作物分类 */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 根据ID动态更新作物分类 */
    int updateById(CropType cropType);

    /** 修改作物分类启用/禁用状态 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 根据ID查询作物分类 */
    CropType selectById(Long id);

    /** 分页条件查询作物分类列表 */
    List<CropType> selectList(@Param("typeName") String typeName,
                              @Param("parentId") Long parentId,
                              @Param("status") Integer status);

    /** 统计同一父分类下是否存在同名分类，用于唯一性校验 */
    int countByTypeNameAndParentId(@Param("typeName") String typeName,
                                   @Param("parentId") Long parentId,
                                   @Param("excludeId") Long excludeId);

    /** 统计指定分类是否存在子分类 */
    int countChildrenByParentId(Long parentId);
}
