package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.CameraCapturePlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 摄像头采集计划数据访问层。 */
@Mapper
public interface CameraCapturePlanMapper {
    /** 新增计划并回填主键。 */ int insert(CameraCapturePlan plan);
    /** 按版本号更新计划，返回0表示发生并发修改。 */ int update(CameraCapturePlan plan);
    /** 根据主键删除计划。 */ int deleteById(@Param("id") Long id);
    /** 查询单条计划及设备展示信息。 */ CameraCapturePlan selectById(@Param("id") Long id);
    /** 按数据范围和条件查询计划。 */
    List<CameraCapturePlan> selectList(@Param("userId") Long userId,
                                       @Param("plotId") Long plotId,
                                       @Param("keyword") String keyword,
                                       @Param("enabled") Integer enabled);
}
