package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AiRecognitionRecord;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.AiRecognitionRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai-recognition-record")
@RequiredArgsConstructor
@RequirePermission("ai_recognition_record:manage")
public class AiRecognitionRecordController {

    private final AiRecognitionRecordService aiRecognitionRecordService;

    @DeleteMapping("/{id}")
    public R<Void> deleteAiRecognitionRecord(@PathVariable Long id) {
        aiRecognitionRecordService.deleteAiRecognitionRecord(id);
        return R.success();
    }

    @DeleteMapping("/batch")
    public R<Integer> deleteAiRecognitionRecords(@RequestBody List<Long> ids) {
        return R.success(aiRecognitionRecordService.deleteAiRecognitionRecords(ids));
    }

    @GetMapping("/{id}")
    public R<AiRecognitionRecord> getAiRecognitionRecordById(@PathVariable Long id) {
        return R.success(aiRecognitionRecordService.getAiRecognitionRecordById(id));
    }

    @GetMapping("/list")
    public R<PageInfo<AiRecognitionRecord>> listAiRecognitionRecords(@RequestParam(required = false) String username,
                                                                     @RequestParam(required = false) Integer sourceType,
                                                                     @RequestParam(required = false) Integer status,
                                                                     @RequestParam(required = false) Integer resultStatus,
                                                                     @RequestParam(required = false) String cameraName,
                                                                     @RequestParam(required = false) String plotName,
                                                                     @RequestParam(required = false) Long recognitionType,
                                                                     @RequestParam(required = false) String resultName,
                                                                     @RequestParam(required = false)
                                                                     @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                                     LocalDate startDate,
                                                                     @RequestParam(required = false)
                                                                     @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                                     LocalDate endDate,
                                                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(aiRecognitionRecordService.listAiRecognitionRecords(
                username, sourceType, status, resultStatus, cameraName, plotName,
                recognitionType, resultName, startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/statistics")
    public R<Map<String, Object>> statisticsAiRecognitionRecords(@RequestParam(required = false) String username,
                                                                 @RequestParam(required = false) Integer sourceType,
                                                                 @RequestParam(required = false) Integer status,
                                                                 @RequestParam(required = false) Integer resultStatus,
                                                                 @RequestParam(required = false) String cameraName,
                                                                 @RequestParam(required = false) String plotName,
                                                                 @RequestParam(required = false) Long recognitionType,
                                                                 @RequestParam(required = false) String resultName,
                                                                 @RequestParam(required = false)
                                                                 @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                                 LocalDate startDate,
                                                                 @RequestParam(required = false)
                                                                 @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                                 LocalDate endDate) {
        return R.success(aiRecognitionRecordService.statisticsAiRecognitionRecords(
                username, sourceType, status, resultStatus, cameraName, plotName,
                recognitionType, resultName, startDate, endDate));
    }
}
