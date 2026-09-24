package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.HomeOperationOverviewItem;
import com.smart_plant.smart_plant.dto.HomeOperationOverviewResponse;
import com.smart_plant.smart_plant.dto.HomeVisitTrendPoint;
import com.smart_plant.smart_plant.dto.HomeVisitTrendResponse;
import com.smart_plant.smart_plant.dto.HomeHeaderSummaryResponse;
import com.smart_plant.smart_plant.dto.HomeSearchResponse;
import com.smart_plant.smart_plant.dto.HomeStatsResponse;
import com.smart_plant.smart_plant.dto.HomeWarehouseValueCategory;
import com.smart_plant.smart_plant.entity.PageVisitLog;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.HomeDashboardMapper;
import com.smart_plant.smart_plant.mapper.PageVisitLogMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.HomeDashboardService;
import com.smart_plant.smart_plant.utils.ClientRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HomeDashboardServiceImpl implements HomeDashboardService {

    private static final String HOME_PAGE_CODE = "home_workbench";
    private static final String HOME_PAGE_NAME = "首页工作台";
    private static final int DEFAULT_TREND_DAYS = 7;
    private static final int MAX_TREND_DAYS = 365;
    private static final int DEFAULT_SEARCH_LIMIT = 12;
    private static final int MAX_SEARCH_LIMIT = 30;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    private final HomeDashboardMapper homeDashboardMapper;
    private final PageVisitLogMapper pageVisitLogMapper;
    private final DataPermissionService dataPermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordHomeVisit(HttpServletRequest request) {
        User user = dataPermissionService.currentUser();
        PageVisitLog visitLog = new PageVisitLog();
        visitLog.setUserId(user.getId());
        visitLog.setUsername(user.getUsername());
        visitLog.setPageCode(HOME_PAGE_CODE);
        visitLog.setPageName(HOME_PAGE_NAME);
        visitLog.setIp(request == null ? "" : ClientRequestUtils.getClientIp(request));
        visitLog.setUserAgent(limitLength(request == null ? "" : request.getHeader("User-Agent"), 512));
        visitLog.setVisitTime(LocalDateTime.now());
        pageVisitLogMapper.insert(visitLog);
    }

    @Override
    public HomeStatsResponse getStats() {
        return homeDashboardMapper.selectHomeStats(dataPermissionService.restrictUserId(null));
    }

    @Override
    public HomeOperationOverviewResponse getOperationOverview() {
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        Map<String, HomeOperationOverviewItem> overviewMap = new LinkedHashMap<>();
        putDefaultOverviewItem(overviewMap, "sensorOnline", "传感器在线");
        putDefaultOverviewItem(overviewMap, "realtimeMonitoring", "实时监测");
        putDefaultOverviewItem(overviewMap, "pumpRunning", "水泵运行");
        putDefaultOverviewItem(overviewMap, "lightRunning", "补光灯照明");
        putDefaultOverviewItem(overviewMap, "fanRunning", "风机运行");

        HomeOperationOverviewItem cameraOverview = homeDashboardMapper.selectCameraOperationOverview(scopedUserId);
        mergeOverviewItem(overviewMap, cameraOverview);
        for (HomeOperationOverviewItem item : homeDashboardMapper.selectIotDeviceOperationOverview(scopedUserId)) {
            mergeOverviewItem(overviewMap, item);
        }

        return new HomeOperationOverviewResponse(LocalDateTime.now(), new ArrayList<>(overviewMap.values()));
    }

    @Override
    public List<HomeWarehouseValueCategory> getWarehouseValueAnalysis() {
        return homeDashboardMapper.selectWarehouseValueAnalysis(dataPermissionService.restrictUserId(null));
    }

    @Override
    public HomeVisitTrendResponse getVisitTrend(Integer days) {
        int normalizedDays = normalizeDays(days);
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(normalizedDays - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1).atStartOfDay();

        List<HomeVisitTrendPoint> existingPoints = pageVisitLogMapper.selectDailyVisitTrend(
                HOME_PAGE_CODE,
                // 访问趋势始终按实际登录用户统计，管理员也只查看本人访问量。
                dataPermissionService.currentUser().getId(),
                startTime,
                endTime);
        Map<String, Long> countMap = new HashMap<>();
        for (HomeVisitTrendPoint point : existingPoints) {
            countMap.put(point.getDate(), point.getVisitCount() == null ? 0L : point.getVisitCount());
        }

        long total = 0L;
        List<HomeVisitTrendPoint> points = new ArrayList<>(normalizedDays);
        for (int index = 0; index < normalizedDays; index++) {
            LocalDate date = startDate.plusDays(index);
            String dateText = DATE_FORMATTER.format(date);
            long visitCount = countMap.getOrDefault(dateText, 0L);
            total += visitCount;
            points.add(new HomeVisitTrendPoint(dateText, LABEL_FORMATTER.format(date), visitCount));
        }

        return new HomeVisitTrendResponse(normalizedDays, total, points);
    }

    @Override
    public HomeHeaderSummaryResponse getHeaderSummary() {
        // 管理员按全局公告组统计，普通用户仍只统计本人的未读投递。
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        return new HomeHeaderSummaryResponse(nullToZero(homeDashboardMapper.selectUnreadNoticeCount(scopedUserId)));
    }

    @Override
    public HomeSearchResponse search(String keyword, Integer limit) {
        if (!StringUtils.hasText(keyword)) {
            return new HomeSearchResponse("", List.of());
        }
        String normalizedKeyword = keyword.trim();
        if (normalizedKeyword.length() > 50) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "搜索关键字不能超过50个字符");
        }
        int normalizedLimit = limit == null ? DEFAULT_SEARCH_LIMIT : limit;
        if (normalizedLimit < 1 || normalizedLimit > MAX_SEARCH_LIMIT) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "搜索结果数量必须在1到30之间");
        }

        // 管理员返回全局数据，农场主等自有数据角色沿用工作台现有的数据范围。
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        return new HomeSearchResponse(
                normalizedKeyword,
                homeDashboardMapper.search(normalizedKeyword, scopedUserId, normalizedLimit));
    }

    private void putDefaultOverviewItem(Map<String, HomeOperationOverviewItem> overviewMap, String metricKey, String label) {
        overviewMap.put(metricKey, new HomeOperationOverviewItem(metricKey, label, 0L, 0L, 0L, BigDecimal.ZERO));
    }

    private void mergeOverviewItem(Map<String, HomeOperationOverviewItem> overviewMap, HomeOperationOverviewItem item) {
        if (item == null || item.getMetricKey() == null) {
            return;
        }
        HomeOperationOverviewItem normalizedItem = normalizeOverviewItem(item);
        overviewMap.put(normalizedItem.getMetricKey(), normalizedItem);
    }

    private HomeOperationOverviewItem normalizeOverviewItem(HomeOperationOverviewItem item) {
        item.setTotalCount(nullToZero(item.getTotalCount()));
        item.setOnlineCount(nullToZero(item.getOnlineCount()));
        item.setActiveCount(nullToZero(item.getActiveCount()));
        if (item.getOnlineRate() == null) {
            item.setOnlineRate(BigDecimal.ZERO);
        }
        return item;
    }

    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private int normalizeDays(Integer days) {
        if (days == null) {
            return DEFAULT_TREND_DAYS;
        }
        if (days < 1 || days > MAX_TREND_DAYS) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "访问趋势天数必须在1到365之间");
        }
        return days;
    }

    private String limitLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
