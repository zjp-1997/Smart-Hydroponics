package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 首页顶部栏摘要。
 *
 * <p>当前只返回登录用户的未读通知数量，后续如需增加待办数量可继续扩展该对象，
 * 避免前端为每一个顶部指标分别发起请求。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeHeaderSummaryResponse {

    /** 当前登录用户尚未阅读的有效通知数量。 */
    private Long unreadNoticeCount;
}
