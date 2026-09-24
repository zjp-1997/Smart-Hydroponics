package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.ExpertReview;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.ExpertReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 专家服务评价控制器，提供评价列表、新增和删除接口。
 */
@RestController
@RequestMapping("/expert-review")
@RequiredArgsConstructor
@RequirePermission("expert_review:manage")
public class ExpertReviewController {

    private final ExpertReviewService expertReviewService;

    /**
     * 新增专家服务评价。
     */
    @PostMapping("/add")
    public R<ExpertReview> addExpertReview(@RequestBody ExpertReview expertReview) {
        return R.success(expertReviewService.addExpertReview(expertReview));
    }

    /**
     * 编辑专家服务评价。
     */
    @PutMapping
    public R<ExpertReview> updateExpertReview(@RequestBody ExpertReview expertReview) {
        return R.success(expertReviewService.updateExpertReview(expertReview));
    }

    /**
     * 删除专家服务评价。
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteExpertReview(@PathVariable Long id) {
        expertReviewService.deleteExpertReview(id);
        return R.success();
    }

    /**
     * 批量删除专家服务评价。
     */
    @DeleteMapping("/batch")
    public R<Integer> deleteExpertReviews(@RequestBody List<Long> ids) {
        return R.success(expertReviewService.deleteExpertReviews(ids));
    }

    /**
     * 分页查询专家服务评价列表。
     */
    @GetMapping("/list")
    public R<PageInfo<ExpertReview>> listExpertReviews(@RequestParam(required = false) Long expertId,
                                                       @RequestParam(required = false) String expertName,
                                                       @RequestParam(required = false) Long userId,
                                                       @RequestParam(required = false) String userName,
                                                       @RequestParam(required = false) String startTime,
                                                       @RequestParam(required = false) String endTime,
                                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(expertReviewService.listExpertReviews(
                expertId,
                expertName,
                userId,
                userName,
                startTime,
                endTime,
                pageNum,
                pageSize
        ));
    }

    /**
     * 查询专家评价统计数据。
     */
    @GetMapping("/statistics")
    public R<Map<String, Object>> statisticsExpertReviews() {
        return R.success(expertReviewService.statisticsExpertReviews());
    }
}
