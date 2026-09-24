package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.AiRecognitionType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * AI识别类型 Mapper 接口。
 *
 * <p>该接口声明 ai_recognition_type 表的增删改查、唯一性校验、引用统计和数据统计方法。</p>
 */
@Mapper
public interface AiRecognitionTypeMapper {

    /** 新增AI识别类型，并回填自增主键。 */
    int insert(AiRecognitionType aiRecognitionType);

    /** 根据主键删除单条AI识别类型。 */
    int deleteById(Long id);

    /** 根据主键集合批量删除AI识别类型。 */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 根据主键动态更新AI识别类型非空字段。 */
    int updateById(AiRecognitionType aiRecognitionType);

    /** 单独更新AI识别类型启用或禁用状态。 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 根据主键查询AI识别类型详情。 */
    AiRecognitionType selectById(Long id);

    /** 根据类型编码查询AI识别类型。 */
    AiRecognitionType selectByTypeCode(String typeCode);

    /** 分页条件查询AI识别类型列表。 */
    List<AiRecognitionType> selectList(@Param("typeCode") String typeCode,
                                       @Param("typeName") String typeName,
                                       @Param("status") Integer status);

    /** 统计同编码记录数量，用于新增和修改时校验唯一性。 */
    int countByTypeCode(@Param("typeCode") String typeCode, @Param("excludeId") Long excludeId);

    /** 统计AI识别结果表中引用指定类型的数量，用于删除保护。 */
    int countRecognitionResultsByTypeId(Long typeId);

    /** 统计AI识别类型总数、启用数、禁用数和识别结果引用数。 */
    Map<String, Object> selectStatistics(@Param("typeCode") String typeCode,
                                         @Param("typeName") String typeName,
                                         @Param("status") Integer status);
}
