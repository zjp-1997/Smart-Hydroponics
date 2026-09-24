package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.ExpertReview;

import java.util.List;
import java.util.Map;

/**
 * 专家服务评价业务接口。
 */
public interface ExpertReviewService {

    /** 新增专家评价 */
    ExpertReview addExpertReview(ExpertReview expertReview);

    /** 编辑专家评价 */
    ExpertReview updateExpertReview(ExpertReview expertReview);

    /** 删除专家评价 */
    void deleteExpertReview(Long id);

    /** 批量删除专家评价 */
    int deleteExpertReviews(List<Long> ids);

    /** 分页查询专家服务评价列表 */
    PageInfo<ExpertReview> listExpertReviews(Long expertId, String expertName, Long userId,
                                             String userName, String startTime, String endTime,
                                             Integer pageNum, Integer pageSize);

    /** 查询专家评价统计数据 */
    Map<String, Object> statisticsExpertReviews();
}
