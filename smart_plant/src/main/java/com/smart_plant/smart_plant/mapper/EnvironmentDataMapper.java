package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.EnvironmentData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 环境监测数据持久层接口，负责 environment_data 表的增删改查。
 */
@Mapper
public interface EnvironmentDataMapper {

    /** 新增环境监测数据 */
    int insert(EnvironmentData environmentData);

    /** 根据ID删除环境监测数据 */
    int deleteById(@Param("id") Long id);

    /** 根据ID集合批量删除环境监测数据 */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 根据ID动态更新环境监测数据 */
    int updateById(EnvironmentData environmentData);

    /** 根据ID查询环境监测数据 */
    EnvironmentData selectById(@Param("id") Long id);

    /** 分页条件查询环境监测数据列表，支持设备ID、数据状态和采集时间范围筛选。 */
    List<EnvironmentData> selectList(@Param("userId") Long userId,
                                     @Param("plotId") Long plotId,
                                     @Param("deviceId") Long deviceId,
                                     @Param("deviceCode") String deviceCode,
                                     @Param("dataStatus") Integer dataStatus,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /** 按地块、设备、状态和时间范围统计环境监测数据。 */
    Map<String, Object> selectStatistics(@Param("userId") Long userId,
                                         @Param("plotId") Long plotId,
                                         @Param("deviceId") Long deviceId,
                                         @Param("deviceCode") String deviceCode,
                                         @Param("dataStatus") Integer dataStatus,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

}
