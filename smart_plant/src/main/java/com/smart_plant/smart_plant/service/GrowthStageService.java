package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.GrowthStage;

import java.util.List;

/**
 * 作物生长期业务接口，定义作物生长期管理能力。
 */
public interface GrowthStageService {

    /** 新增作物生长期 */
    GrowthStage addGrowthStage(GrowthStage growthStage);

    /** 编辑作物生长期 */
    GrowthStage updateGrowthStage(GrowthStage growthStage);

    /** 删除作物生长期 */
    void deleteGrowthStage(Long id);

    /** 批量删除作物生长期 */
    int deleteGrowthStages(List<Long> ids);

    /** 启用或禁用作物生长期 */
    void updateStatus(Long id, Integer status);

    /** 根据ID查询作物生长期 */
    GrowthStage getGrowthStageById(Long id);

    /** 分页查询作物生长期列表 */
    PageInfo<GrowthStage> listGrowthStages(Long cropId, String stageName, Integer status,
                                           Integer pageNum, Integer pageSize);
}
