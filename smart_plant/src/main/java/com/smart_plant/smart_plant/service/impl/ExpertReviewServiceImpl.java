package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.ExpertProfile;
import com.smart_plant.smart_plant.entity.ExpertReview;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.ExpertProfileMapper;
import com.smart_plant.smart_plant.mapper.ExpertReviewMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.ExpertReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 专家服务评价业务实现类，处理评价新增、删除和专家评分汇总。
 */
@Service
@RequiredArgsConstructor
public class ExpertReviewServiceImpl implements ExpertReviewService {

    private final ExpertReviewMapper expertReviewMapper;

    private final ExpertProfileMapper expertProfileMapper;

    private final UserMapper userMapper;

    /**
     * 新增专家评价，并重新计算专家平均评分和评价数量。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpertReview addExpertReview(ExpertReview expertReview) {
        validateCreateExpertReview(expertReview);
        ExpertProfile expertProfile = expertProfileMapper.selectById(expertReview.getExpertId());
        if (expertProfile == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "专家信息不存在");
        }
        if (expertProfile.getAuditStatus() == null || expertProfile.getAuditStatus() != 2) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "只能评价已通过入驻审核的专家");
        }
        if (expertReview.getUserId() != null && userMapper.selectById(expertReview.getUserId()) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "评价用户不存在");
        }
        expertReviewMapper.insert(expertReview);
        expertProfileMapper.updateRatingSummary(expertReview.getExpertId());
        return expertReviewMapper.selectById(expertReview.getId());
    }

    /**
     * 编辑专家评价，并重新计算专家平均评分和评价数量。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpertReview updateExpertReview(ExpertReview expertReview) {
        validateUpdateExpertReview(expertReview);
        ExpertReview oldReview = expertReviewMapper.selectById(expertReview.getId());
        if (oldReview == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "评价不存在");
        }
        int rows = expertReviewMapper.updateById(expertReview);
        if (rows <= 0) {
            throw new BusinessException(ResponseCode.FAIL, "评价修改失败");
        }
        expertProfileMapper.updateRatingSummary(oldReview.getExpertId());
        return expertReviewMapper.selectById(expertReview.getId());
    }

    /**
     * 删除专家评价，并重新计算专家平均评分和评价数量。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteExpertReview(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "评价ID不能为空");
        }
        ExpertReview oldReview = expertReviewMapper.selectById(id);
        if (oldReview == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "评价不存在");
        }
        expertReviewMapper.deleteById(id);
        expertProfileMapper.updateRatingSummary(oldReview.getExpertId());
    }

    /**
     * 批量删除专家评价，并重新计算被影响专家的评分汇总。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteExpertReviews(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的评价");
        }
        for (Long id : ids) {
            ExpertReview oldReview = expertReviewMapper.selectById(id);
            if (oldReview == null) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "评价不存在");
            }
            int rows = expertReviewMapper.deleteById(id);
            if (rows <= 0) {
                throw new BusinessException(ResponseCode.FAIL, "评价删除失败");
            }
            expertProfileMapper.updateRatingSummary(oldReview.getExpertId());
        }
        return ids.size();
    }

    /**
     * 分页查询专家服务评价列表，列表不返回专家审核状态和服务状态。
     */
    @Override
    public PageInfo<ExpertReview> listExpertReviews(Long expertId, String expertName, Long userId,
                                                    String userName, String startTime, String endTime,
                                                    Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(expertReviewMapper.selectList(expertId, expertName, userId, userName, startTime, endTime));
    }

    /**
     * 查询专家评价统计数据，统计范围与评价管理列表保持一致。
     */
    @Override
    public Map<String, Object> statisticsExpertReviews() {
        return expertReviewMapper.selectStatistics();
    }

    /** 新增评价参数校验。 */
    private void validateCreateExpertReview(ExpertReview expertReview) {
        if (expertReview == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "评价信息不能为空");
        }
        if (expertReview.getExpertId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家ID不能为空");
        }
        if (expertReview.getRating() == null
                || expertReview.getRating().compareTo(BigDecimal.ZERO) < 0
                || expertReview.getRating().compareTo(new BigDecimal("5.0")) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "评价评分必须在0到5之间");
        }
        if (!StringUtils.hasText(expertReview.getContent())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "评价内容不能为空");
        }
    }

    /** 编辑评价参数校验。 */
    private void validateUpdateExpertReview(ExpertReview expertReview) {
        if (expertReview == null || expertReview.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "评价ID不能为空");
        }
        if (expertReview.getRating() == null
                || expertReview.getRating().compareTo(BigDecimal.ZERO) < 0
                || expertReview.getRating().compareTo(new BigDecimal("5.0")) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "评价评分必须在0到5之间");
        }
        if (!StringUtils.hasText(expertReview.getContent())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "评价内容不能为空");
        }
    }
}
