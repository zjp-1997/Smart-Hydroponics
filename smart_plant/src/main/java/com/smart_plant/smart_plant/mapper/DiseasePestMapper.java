package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.DiseasePest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 病虫害基础信息持久层接口。
 *
 * <p>该 Mapper 负责 disease_pest 表的增删改查，并在查询时关联作物类型名称。</p>
 */
@Mapper
public interface DiseasePestMapper {

    /** 新增一条病虫害基础信息。 */
    int insert(DiseasePest diseasePest);

    /** 根据ID删除一条病虫害基础信息。 */
    int deleteById(Long id);

    /** 根据ID集合批量删除病虫害基础信息。 */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 根据ID动态更新病虫害基础信息。 */
    int updateById(DiseasePest diseasePest);

    /** 单独修改病虫害基础信息启用/停用状态。 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 根据ID查询病虫害详情，并带出作物类型名称。 */
    DiseasePest selectById(Long id);

    /** 按条件查询病虫害基础信息列表。 */
    List<DiseasePest> selectList(@Param("name") String name,
                                 @Param("cropTypeId") Long cropTypeId,
                                 @Param("type") Integer type,
                                 @Param("status") Integer status);

    /** 统计同一作物类型下同名病虫害数量，用于新增和修改时的重复校验。 */
    int countByCropTypeIdAndName(@Param("cropTypeId") Long cropTypeId,
                                 @Param("name") String name,
                                 @Param("excludeId") Long excludeId);
}
