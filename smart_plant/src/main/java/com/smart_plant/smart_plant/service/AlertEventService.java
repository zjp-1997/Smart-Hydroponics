package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.entity.AlertEvent;

import com.github.pagehelper.PageInfo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AlertEventService {

    AlertEvent createIfAbsent(AlertEvent alertEvent);

    AlertEvent getBySource(String sourceType, Long sourceId, Integer alertType);

    AlertEvent getAlertEventById(Long id);

    PageInfo<AlertEvent> listAlertEvents(String plotName, Integer alertType, Integer alertLevel,
                                         Integer processStatus, String sourceType, LocalDate startDate,
                                         LocalDate endDate, Integer pageNum, Integer pageSize);

    Map<String, Object> statisticsAlertEvents();

    AlertEvent updateProcessStatus(Long id, Integer processStatus, String handleResult);

    void deleteAlertEvent(Long id);

    int deleteAlertEvents(List<Long> ids);
}
