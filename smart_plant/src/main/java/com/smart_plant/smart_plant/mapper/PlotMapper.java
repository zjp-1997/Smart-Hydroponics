package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.dto.PlotStatisticsResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlotMapper {

    int insert(Plot plot);

    int deleteById(Long id);

    int deleteBatchByIds(@Param("ids") List<Long> ids);

    int updateById(Plot plot);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    Plot selectById(Long id);

    List<Plot> selectList(@Param("plotName") String plotName,
                          @Param("plotCode") String plotCode,
                          @Param("farmId") Long farmId,
                          @Param("userId") Long userId,
                          @Param("type") Integer type,
                          @Param("status") Integer status);

    PlotStatisticsResponse selectStatistics(@Param("userId") Long userId);

    int countByUserIdAndPlotCode(@Param("userId") Long userId,
                                 @Param("plotCode") String plotCode,
                                 @Param("excludeId") Long excludeId);

    int countPlantingBatchByPlotId(@Param("plotId") Long plotId);
}
