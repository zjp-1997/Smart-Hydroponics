package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.FarmTaskRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FarmTaskRecordMapper {

    int insert(FarmTaskRecord record);

    int deleteByTaskId(Long taskId);

    int deleteByTaskIds(@Param("ids") List<Long> ids);

    FarmTaskRecord selectById(Long id);

    /** 按幂等请求号查询已落库事件，重复请求可直接返回已有结果。 */
    FarmTaskRecord selectByRequestId(String requestId);

    List<FarmTaskRecord> selectList(@Param("taskId") Long taskId, @Param("userId") Long userId);

    /** 查询单个任务的完整时间线，按事件发生时间正序返回。 */
    List<FarmTaskRecord> selectTimeline(@Param("taskId") Long taskId, @Param("userId") Long userId);

    /**
     * 查询当前用户指定地块下的农事执行记录，按事件发生时间正序返回。
     */
    List<FarmTaskRecord> selectListByPlotId(@Param("userId") Long userId, @Param("plotId") Long plotId);
}
