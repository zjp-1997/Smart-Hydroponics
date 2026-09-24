package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AlertEvent;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.AlertEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/alert-event")
@RequiredArgsConstructor
@RequirePermission("alert_event:manage")
public class AlertEventController {

    private final AlertEventService alertEventService;

    @DeleteMapping("/{id}")
    public R<Void> deleteAlertEvent(@PathVariable Long id) {
        alertEventService.deleteAlertEvent(id);
        return R.success();
    }

    @DeleteMapping("/batch")
    public R<Integer> deleteAlertEvents(@RequestBody List<Long> ids) {
        return R.success(alertEventService.deleteAlertEvents(ids));
    }

    @PutMapping("/{id}/process-status")
    public R<AlertEvent> updateProcessStatus(@PathVariable Long id,
                                             @RequestParam Integer processStatus,
                                             @RequestParam(required = false) String handleResult) {
        return R.success(alertEventService.updateProcessStatus(id, processStatus, handleResult));
    }

    @GetMapping("/{id}")
    public R<AlertEvent> getAlertEventById(@PathVariable Long id) {
        return R.success(alertEventService.getAlertEventById(id));
    }

    @GetMapping("/list")
    public R<PageInfo<AlertEvent>> listAlertEvents(@RequestParam(required = false) String plotName,
                                                   @RequestParam(required = false) Integer alertType,
                                                   @RequestParam(required = false) Integer alertLevel,
                                                   @RequestParam(required = false) Integer processStatus,
                                                   @RequestParam(required = false) String sourceType,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                   LocalDate startDate,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                   LocalDate endDate,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(alertEventService.listAlertEvents(
                plotName, alertType, alertLevel, processStatus, sourceType,
                startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/statistics")
    public R<Map<String, Object>> statisticsAlertEvents() {
        return R.success(alertEventService.statisticsAlertEvents());
    }
}
