package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.GrowthStage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 作物生长期持久层接口，负责 growth_stage 表的增删改查。
 */
@Mapper
public interface GrowthStageMapper {

    /** 新增作物生长期 */
    int insert(GrowthStage growthStage);

    /** 根据ID删除作物生长期 */
    int deleteById(Long id);

    /** 根据ID集合批量删除作物生长期 */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 根据ID动态更新作物生长期 */
    int updateById(GrowthStage growthStage);

    /** 修改作物生长期启用/禁用状态 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 根据ID查询作物生长期 */
    GrowthStage selectById(Long id);

    /** 分页条件查询作物生长期列表 */
    List<GrowthStage> selectList(@Param("cropId") Long cropId,
                                 @Param("stageName") String stageName,
                                 @Param("status") Integer status);

    /** 统计同一作物下阶段顺序是否已存在，用于唯一性校验 */
    int countByCropIdAndStageOrder(@Param("cropId") Long cropId,
                                   @Param("stageOrder") Integer stageOrder,
                                   @Param("excludeId") Long excludeId);
}
