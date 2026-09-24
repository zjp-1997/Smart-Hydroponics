package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.dto.HomeOperationOverviewItem;
import com.smart_plant.smart_plant.dto.HomeSearchResultItem;
import com.smart_plant.smart_plant.dto.HomeStatsResponse;
import com.smart_plant.smart_plant.dto.HomeWarehouseValueCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HomeDashboardMapper {

    HomeStatsResponse selectHomeStats(@Param("userId") Long userId);

    List<HomeOperationOverviewItem> selectIotDeviceOperationOverview(@Param("userId") Long userId);

    HomeOperationOverviewItem selectCameraOperationOverview(@Param("userId") Long userId);

    List<HomeWarehouseValueCategory> selectWarehouseValueAnalysis(@Param("userId") Long userId);

    /** 普通用户统计本人未读投递；管理员传 null，按全局系统公告组统计。 */
    Long selectUnreadNoticeCount(@Param("userId") Long userId);

    /** 在当前数据权限范围内搜索任务、设备和地块。 */
    List<HomeSearchResultItem> search(@Param("keyword") String keyword,
                                      @Param("userId") Long userId,
                                      @Param("limit") Integer limit);
}
