package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.AlertEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AlertEventMapper {

    int insert(AlertEvent alertEvent);

    AlertEvent selectById(Long id);

    AlertEvent selectBySource(@Param("sourceType") String sourceType,
                              @Param("sourceId") Long sourceId,
                              @Param("alertType") Integer alertType);

    AlertEvent selectByDedupKeyForUpdate(@Param("dedupKey") String dedupKey);

    List<AlertEvent> selectList(@Param("userId") Long userId,
                                @Param("plotName") String plotName,
                                @Param("alertType") Integer alertType,
                                @Param("alertLevel") Integer alertLevel,
                                @Param("processStatus") Integer processStatus,
                                @Param("sourceType") String sourceType,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    Map<String, Object> selectStatistics(@Param("userId") Long userId);

    int updateProcessStatus(@Param("id") Long id,
                            @Param("processStatus") Integer processStatus,
                            @Param("handlerId") Long handlerId,
                            @Param("handleResult") String handleResult);

    int deleteById(Long id);

    int deleteBatchByIds(@Param("ids") List<Long> ids);
}
