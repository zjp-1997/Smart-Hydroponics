package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.ExpertDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专家详情持久层接口，负责 expert_detail 表的维护。
 */
@Mapper
public interface ExpertDetailMapper {

    /** 新增或更新专家详情，一个专家只对应一条详情记录。 */
    int upsertByExpertId(ExpertDetail expertDetail);

    /** 根据专家ID删除详情记录。 */
    int deleteByExpertId(Long expertId);

    /** 根据专家ID集合批量删除详情记录。 */
    int deleteBatchByExpertIds(@Param("expertIds") List<Long> expertIds);

    /** 根据专家ID查询详情记录。 */
    ExpertDetail selectByExpertId(Long expertId);
}
