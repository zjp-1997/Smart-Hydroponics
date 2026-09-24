package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.HomeOperationOverviewResponse;
import com.smart_plant.smart_plant.dto.HomeVisitTrendResponse;
import com.smart_plant.smart_plant.dto.HomeHeaderSummaryResponse;
import com.smart_plant.smart_plant.dto.HomeSearchResponse;
import com.smart_plant.smart_plant.dto.HomeStatsResponse;
import com.smart_plant.smart_plant.dto.HomeWarehouseValueCategory;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.HomeDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/home-dashboard")
@RequiredArgsConstructor
@RequirePermission("home:view")
public class HomeDashboardController {

    private final HomeDashboardService homeDashboardService;

    @PostMapping("/visit")
    public R<Void> recordVisit(HttpServletRequest request) {
        homeDashboardService.recordHomeVisit(request);
        return R.success();
    }

    @GetMapping("/stats")
    public R<HomeStatsResponse> getStats() {
        return R.success(homeDashboardService.getStats());
    }

    @GetMapping("/operation-overview")
    public R<HomeOperationOverviewResponse> getOperationOverview() {
        return R.success(homeDashboardService.getOperationOverview());
    }

    @GetMapping("/warehouse-value-analysis")
    public R<List<HomeWarehouseValueCategory>> getWarehouseValueAnalysis() {
        return R.success(homeDashboardService.getWarehouseValueAnalysis());
    }

    @GetMapping("/visit-trend")
    public R<HomeVisitTrendResponse> getVisitTrend(@RequestParam(defaultValue = "7") Integer days) {
        return R.success(homeDashboardService.getVisitTrend(days));
    }

    /** smart_farm 顶部栏动态摘要接口。 */
    @GetMapping("/header-summary")
    public R<HomeHeaderSummaryResponse> getHeaderSummary() {
        return R.success(homeDashboardService.getHeaderSummary());
    }

    /** smart_farm 顶部搜索框接口，返回任务、设备和地块三类结果。 */
    @GetMapping("/search")
    public R<HomeSearchResponse> search(@RequestParam String keyword,
                                        @RequestParam(defaultValue = "12") Integer limit) {
        return R.success(homeDashboardService.search(keyword, limit));
    }
}
