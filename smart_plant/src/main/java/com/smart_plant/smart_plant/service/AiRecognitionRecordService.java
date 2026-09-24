package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AiRecognitionRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AiRecognitionRecordService {

    void deleteAiRecognitionRecord(Long id);

    int deleteAiRecognitionRecords(List<Long> ids);

    AiRecognitionRecord getAiRecognitionRecordById(Long id);

    PageInfo<AiRecognitionRecord> listAiRecognitionRecords(String username, Integer sourceType, Integer status,
                                                           Integer resultStatus, String cameraName,
                                                           String plotName, Long recognitionType,
                                                           String resultName, LocalDate startDate,
                                                           LocalDate endDate, Integer pageNum,
                                                           Integer pageSize);

    Map<String, Object> statisticsAiRecognitionRecords(String username, Integer sourceType, Integer status,
                                                       Integer resultStatus, String cameraName,
                                                       String plotName, Long recognitionType,
                                                       String resultName, LocalDate startDate,
                                                       LocalDate endDate);
}
