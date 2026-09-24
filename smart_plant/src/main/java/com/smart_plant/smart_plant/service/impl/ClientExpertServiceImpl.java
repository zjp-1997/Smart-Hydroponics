package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.ClientExpertListResponse;
import com.smart_plant.smart_plant.entity.ExpertProfile;
import com.smart_plant.smart_plant.mapper.ExpertProfileMapper;
import com.smart_plant.smart_plant.service.ClientExpertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * farm 用户端专家咨询业务实现。
 *
 * <p>这里复用后台专家档案 Mapper，向用户端返回全部专家及其咨询状态。</p>
 */
@Service
@RequiredArgsConstructor
public class ClientExpertServiceImpl implements ClientExpertService {

    /** 专家资料持久层，负责聚合 expert_profile、institution、expert_detail 等表数据。 */
    private final ExpertProfileMapper expertProfileMapper;

    /**
     * 查询用户端专家列表，并转换为移动端专用 DTO。
     */
    @Override
    public List<ClientExpertListResponse> listExperts(String keyword) {
        String realName = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return expertProfileMapper
                .selectClientList(realName)
                .stream()
                .map(this::toClientExpertListResponse)
                .toList();
    }

    /**
     * 将数据库聚合实体转换成用户端响应对象。
     *
     * <p>默认值在服务端兜底，前端即使遇到部分旧数据为空，也能保持页面稳定渲染。</p>
     */
    private ClientExpertListResponse toClientExpertListResponse(ExpertProfile expertProfile) {
        ClientExpertListResponse response = new ClientExpertListResponse();
        response.setId(expertProfile.getId());
        response.setName(defaultText(expertProfile.getRealName(), "未知专家"));
        response.setAvatar(expertProfile.getAvatar());
        response.setUnit(defaultText(expertProfile.getOrganization(), "未知机构"));
        response.setJobTitle(defaultText(expertProfile.getJobTitle(), "农学专家"));
        response.setSpecialty(defaultText(expertProfile.getSpecialty(), "农学专家"));
        response.setIntroduction(defaultText(expertProfile.getIntroduction(), "暂无专家简介"));
        response.setRating(expertProfile.getRating() == null ? BigDecimal.ZERO : expertProfile.getRating());
        response.setConsultable(Boolean.TRUE.equals(expertProfile.getConsultable()));
        return response;
    }

    /**
     * 文本字段统一兜底，减少前端对历史空数据的兼容判断。
     */
    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
