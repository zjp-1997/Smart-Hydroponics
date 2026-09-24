package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.dto.HomeVisitTrendPoint;
import com.smart_plant.smart_plant.entity.PageVisitLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PageVisitLogMapper {

    int insert(PageVisitLog pageVisitLog);

    List<HomeVisitTrendPoint> selectDailyVisitTrend(@Param("pageCode") String pageCode,
                                                    @Param("userId") Long userId,
                                                    @Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);
}
