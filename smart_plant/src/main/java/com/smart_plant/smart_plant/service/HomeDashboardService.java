package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.HomeVisitTrendResponse;
import com.smart_plant.smart_plant.dto.HomeOperationOverviewResponse;
import com.smart_plant.smart_plant.dto.HomeHeaderSummaryResponse;
import com.smart_plant.smart_plant.dto.HomeSearchResponse;
import com.smart_plant.smart_plant.dto.HomeStatsResponse;
import com.smart_plant.smart_plant.dto.HomeWarehouseValueCategory;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface HomeDashboardService {

    void recordHomeVisit(HttpServletRequest request);

    HomeStatsResponse getStats();

    HomeOperationOverviewResponse getOperationOverview();

    List<HomeWarehouseValueCategory> getWarehouseValueAnalysis();

    HomeVisitTrendResponse getVisitTrend(Integer days);

    /** 查询首页顶部栏的动态摘要。 */
    HomeHeaderSummaryResponse getHeaderSummary();

    /** 全局搜索农事任务、设备和地块。 */
    HomeSearchResponse search(String keyword, Integer limit);
}
