package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AiSolution;
import com.smart_plant.smart_plant.entity.FarmTask;

import java.time.LocalDate;
import java.util.Map;

public interface AiSolutionService {

    AiSolution generateSolution(Long recordId, Boolean generateTask);

    FarmTask generateFarmTask(Long solutionId, FarmTask request);

    AiSolution getAiSolutionById(Long id);

    AiSolution getLatestByRecordId(Long recordId);

    PageInfo<AiSolution> listAiSolutions(String plotName, String cameraName, String solutionTitle,
                                         Integer priorityLevel, Integer taskGenerated, Integer status,
                                         LocalDate startDate, LocalDate endDate, Integer pageNum,
                                         Integer pageSize);

    Map<String, Object> statisticsAiSolutions();
}
