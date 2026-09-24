package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.ExpertReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 专家服务评价持久层接口，负责 expert_review 表的新增、删除和查询。
 */
@Mapper
public interface ExpertReviewMapper {

    /** 新增专家评价 */
    int insert(ExpertReview expertReview);

    /** 根据ID动态更新专家评价 */
    int updateById(ExpertReview expertReview);

    /** 根据ID物理删除专家评价 */
    int deleteById(Long id);

    /** 根据ID集合批量物理删除专家评价 */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 根据ID查询专家评价 */
    ExpertReview selectById(Long id);

    /** 分页条件查询专家评价列表 */
    List<ExpertReview> selectList(@Param("expertId") Long expertId,
                                  @Param("expertName") String expertName,
                                  @Param("userId") Long userId,
                                  @Param("userName") String userName,
                                  @Param("startTime") String startTime,
                                  @Param("endTime") String endTime);

    /** 查询专家评价统计数据 */
    Map<String, Object> selectStatistics();
}
