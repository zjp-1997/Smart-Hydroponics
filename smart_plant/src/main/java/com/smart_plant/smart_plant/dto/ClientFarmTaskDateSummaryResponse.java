package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * farm 用户端农事任务日期摘要。
 *
 * <p>移动端日期横向列表只需要展示日期、星期和当天任务数量，
 * 单独封装 DTO 可以避免把数据库表字段直接暴露给前端页面。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientFarmTaskDateSummaryResponse {

    /** 当前日期，前端点击日期时用它再次查询任务列表。 */
    private LocalDate taskDate;

    /** 中文星期文案，例如：周一。 */
    private String weekday;

    /** 日期中的日，例如：9。 */
    private String dayText;

    /** 当前日期下的农事任务总数。 */
    private Integer taskCount;
}
