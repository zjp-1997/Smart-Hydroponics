package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AiRecognitionType;

import java.util.List;
import java.util.Map;

/**
 * AI识别类型业务接口。
 *
 * <p>该接口定义AI识别类型管理功能的业务能力，控制器通过它调用具体实现。</p>
 */
public interface AiRecognitionTypeService {

    /** 新增AI识别类型。 */
    AiRecognitionType addAiRecognitionType(AiRecognitionType aiRecognitionType);

    /** 修改AI识别类型基础信息。 */
    AiRecognitionType updateAiRecognitionType(AiRecognitionType aiRecognitionType);

    /** 删除单个AI识别类型。 */
    void deleteAiRecognitionType(Long id);

    /** 批量删除AI识别类型。 */
    int deleteAiRecognitionTypes(List<Long> ids);

    /** 修改AI识别类型启用或禁用状态。 */
    void updateStatus(Long id, Integer status);

    /** 根据ID查询AI识别类型详情。 */
    AiRecognitionType getAiRecognitionTypeById(Long id);

    /** 分页查询AI识别类型列表。 */
    PageInfo<AiRecognitionType> listAiRecognitionTypes(String typeCode, String typeName, Integer status,
                                                       Integer pageNum, Integer pageSize);

    /** 统计AI识别类型总数、启用数、禁用数和识别结果引用数。 */
    Map<String, Object> statisticsAiRecognitionTypes(String typeCode, String typeName, Integer status);
}
