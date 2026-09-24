package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.dto.HomeSearchResultItem;
import com.smart_plant.smart_plant.dto.HomeStatsResponse;
import com.smart_plant.smart_plant.dto.HomeWarehouseValueCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证首页新增 SQL 能在本地 smart_plant 实际表结构上执行。 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class HomeDashboardMapperIntegrationTest {

    @Autowired
    private HomeDashboardMapper homeDashboardMapper;

    @Test
    void searchesDashboardResourcesAndCountsUnreadNotices() {
        // 使用不存在的关键字也会完整执行三个 UNION 分支，可验证字段名和 SQL 语法。
        List<HomeSearchResultItem> items = homeDashboardMapper.search("__dashboard_sql_check__", null, 12);
        Long unreadCount = homeDashboardMapper.selectUnreadNoticeCount(3L);
        Long adminUnreadCount = homeDashboardMapper.selectUnreadNoticeCount(null);
        HomeStatsResponse stats = homeDashboardMapper.selectHomeStats(null);
        List<HomeWarehouseValueCategory> warehouseValues = homeDashboardMapper.selectWarehouseValueAnalysis(null);

        assertNotNull(items);
        assertNotNull(unreadCount);
        assertNotNull(adminUnreadCount);
        assertNotNull(stats);
        assertNotNull(warehouseValues);
        assertTrue(warehouseValues.stream().allMatch(item -> item.getCategory() >= 1
                && item.getCategory() <= 6
                && item.getCategoryName() != null
                && item.getTotalValue().signum() > 0));
        assertTrue(warehouseValues.stream().map(HomeWarehouseValueCategory::getCategory).distinct().count()
                == warehouseValues.size());
        assertNotNull(stats.getFarms());
        assertTrue(unreadCount >= 0L);
        assertTrue(adminUnreadCount >= 0L);
    }
}
