package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.ClientExpertListResponse;

import java.util.List;

/**
 * farm 用户端专家咨询业务接口。
 *
 * <p>该接口面向移动端/小程序端，返回全部专家数据及咨询状态。</p>
 */
public interface ClientExpertService {

    /**
     * 查询用户端可展示的专家列表。
     *
     * @param keyword 专家姓名关键字，可为空；为空时返回全部专家
     * @return 专家列表展示数据
     */
    List<ClientExpertListResponse> listExperts(String keyword);
}
