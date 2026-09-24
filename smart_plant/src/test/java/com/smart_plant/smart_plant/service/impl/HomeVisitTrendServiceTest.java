package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.HomeVisitTrendPoint;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.mapper.HomeDashboardMapper;
import com.smart_plant.smart_plant.mapper.PageVisitLogMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 首页访问趋势必须按登录用户隔离，管理员同样只统计本人访问。 */
class HomeVisitTrendServiceTest {

    @Test
    void queriesOnlyCurrentUsersVisits() {
        PageVisitLogMapper mapper = mock(PageVisitLogMapper.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);
        User admin = new User();
        admin.setId(8L);
        admin.setRoleCode("admin");
        when(permissionService.currentUser()).thenReturn(admin);
        when(mapper.selectDailyVisitTrend(eq("home_workbench"), eq(8L),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new HomeVisitTrendPoint(java.time.LocalDate.now().toString(), "今天", 2L)));
        HomeDashboardServiceImpl service = new HomeDashboardServiceImpl(
                mock(HomeDashboardMapper.class), mapper, permissionService);

        assertEquals(2L, service.getVisitTrend(7).getTotal());
        verify(mapper).selectDailyVisitTrend(eq("home_workbench"), eq(8L),
                any(LocalDateTime.class), any(LocalDateTime.class));
    }
}
