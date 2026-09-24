package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 页面访问日志，用于工作台访问量趋势统计。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVisitLog {

    private Long id;

    private Long userId;

    private String username;

    private String pageCode;

    private String pageName;

    private String ip;

    private String userAgent;

    private LocalDateTime visitTime;
}
