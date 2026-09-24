package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * farm 首页农事任务状态统计。
 *
 * <p>统计口径与移动端农事任务列表一致：仅统计当前登录用户的
 * 灌溉、施肥、喷药和采收任务；已取消任务不计入首页统计。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientFarmTaskStatisticsResponse {

    /** 状态为 1 的未开始任务数。 */
    private Long pendingCount;

    /** 状态为 2 的进行中任务数。 */
    private Long runningCount;

    /** 状态为 3 的已完成任务数。 */
    private Long completedCount;

    /** 状态为 4 的已逾期任务数。 */
    private Long overdueCount;
}
