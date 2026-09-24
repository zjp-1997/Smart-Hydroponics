package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AiModel;

import java.util.List;
import java.util.Map;

/**
 * AI模型配置业务接口。
 *
 * <p>该接口定义后台模型管理所需的新增、修改、删除、查询和统计能力。</p>
 */
public interface AiModelService {

    /** 新增AI模型配置。 */
    AiModel addModel(AiModel model);

    /** 修改AI模型配置。 */
    AiModel updateModel(AiModel model);

    /** 删除单个AI模型配置。 */
    void deleteModel(Long id);

    /** 批量删除AI模型配置。 */
    int deleteModels(List<Long> ids);

    /** 根据ID查询AI模型配置详情。 */
    AiModel getModelById(Long id);

    /** 分页查询AI模型配置列表。 */
    PageInfo<AiModel> listModels(String modelName, String model, Integer status, Integer pageNum, Integer pageSize);

    /** 统计模型配置数量、启用数量、禁用数量和对话数量。 */
    Map<String, Object> statisticsModels(String modelName, String model, Integer status);
}
