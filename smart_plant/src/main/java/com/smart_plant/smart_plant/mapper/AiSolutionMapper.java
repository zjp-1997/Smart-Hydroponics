package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.AiSolution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AiSolutionMapper {

    int insert(AiSolution aiSolution);

    int updateById(AiSolution aiSolution);

    int updateTaskLink(@Param("id") Long id,
                       @Param("farmTaskId") Long farmTaskId,
                       @Param("taskGenerated") Integer taskGenerated,
                       @Param("status") Integer status);

    int clearTaskLinkByFarmTaskId(Long farmTaskId);

    int clearTaskLinkByFarmTaskIds(@Param("ids") List<Long> ids);

    int deleteByRecordId(Long recordId);

    int deleteByRecordIds(@Param("ids") List<Long> ids);

    AiSolution selectById(Long id);

    AiSolution selectLatestByRecordId(Long recordId);

    List<AiSolution> selectList(@Param("userId") Long userId,
                                @Param("plotName") String plotName,
                                @Param("cameraName") String cameraName,
                                @Param("solutionTitle") String solutionTitle,
                                @Param("priorityLevel") Integer priorityLevel,
                                @Param("taskGenerated") Integer taskGenerated,
                                @Param("status") Integer status,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    Map<String, Object> selectStatistics(@Param("userId") Long userId);
}
