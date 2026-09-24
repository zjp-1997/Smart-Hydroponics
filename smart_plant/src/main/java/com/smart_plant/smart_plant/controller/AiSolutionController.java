package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AiSolution;
import com.smart_plant.smart_plant.entity.FarmTask;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.AiSolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/ai-solution")
@RequiredArgsConstructor
@RequirePermission("ai_solution:manage")
public class AiSolutionController {

    private final AiSolutionService aiSolutionService;

    @PostMapping("/generate/{recordId}")
    public R<AiSolution> generateSolution(@PathVariable Long recordId,
                                          @RequestParam(required = false) Boolean generateTask) {
        return R.success(aiSolutionService.generateSolution(recordId, generateTask));
    }

    @PostMapping("/{id}/generate-task")
    public R<FarmTask> generateFarmTask(@PathVariable Long id, @RequestBody(required = false) FarmTask request) {
        return R.success(aiSolutionService.generateFarmTask(id, request));
    }

    @GetMapping("/{id}")
    public R<AiSolution> getAiSolutionById(@PathVariable Long id) {
        return R.success(aiSolutionService.getAiSolutionById(id));
    }

    @GetMapping("/record/{recordId}")
    public R<AiSolution> getLatestByRecordId(@PathVariable Long recordId) {
        return R.success(aiSolutionService.getLatestByRecordId(recordId));
    }

    @GetMapping("/list")
    public R<PageInfo<AiSolution>> listAiSolutions(@RequestParam(required = false) String plotName,
                                                   @RequestParam(required = false) String cameraName,
                                                   @RequestParam(required = false) String solutionTitle,
                                                   @RequestParam(required = false) Integer priorityLevel,
                                                   @RequestParam(required = false) Integer taskGenerated,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                   LocalDate startDate,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                   LocalDate endDate,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(aiSolutionService.listAiSolutions(
                plotName, cameraName, solutionTitle, priorityLevel, taskGenerated,
                status, startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/statistics")
    public R<Map<String, Object>> statisticsAiSolutions() {
        return R.success(aiSolutionService.statisticsAiSolutions());
    }
}
