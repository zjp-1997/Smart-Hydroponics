package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.AiModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * AI模型配置 Mapper。
 *
 * <p>该接口负责 model 表的增删改查和统计查询。</p>
 */
@Mapper
public interface AiModelMapper {

    /** 新增模型配置，并回填自增主键。 */
    int insert(AiModel model);

    /** 根据主键动态更新模型配置。 */
    int updateById(AiModel model);

    /** 根据主键删除单条模型配置。 */
    int deleteById(Long id);

    /** 根据主键集合批量删除模型配置。 */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 根据主键查询模型配置详情。 */
    AiModel selectById(Long id);

    /** 查询第一个启用模型，farm 用户端未指定模型时使用。 */
    AiModel selectFirstEnabled();

    /** AI 咨询只选启用的 DeepSeek 配置，具体文本或视觉模型由服务端决定。 */
    AiModel selectFirstEnabledDeepSeek();

    /** 按条件分页查询模型配置列表。 */
    List<AiModel> selectList(@Param("modelName") String modelName,
                             @Param("model") String model,
                             @Param("status") Integer status);

    /** 统计同名模型配置数量，用于新增和编辑时做唯一性校验。 */
    int countByModelName(@Param("modelName") String modelName, @Param("excludeId") Long excludeId);

    /** 统计指定模型下的对话数量，删除时用于风险提示和数据统计。 */
    int countChatsByModelId(Long modelId);

    /** 统计模型配置总数、启用数、禁用数和对话引用数。 */
    Map<String, Object> selectStatistics(@Param("modelName") String modelName,
                                         @Param("model") String model,
                                         @Param("status") Integer status);
}
