package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.Notification;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.NotificationService;
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
@RequestMapping("/notification")
@RequiredArgsConstructor
@RequirePermission("notification:manage")
public class NotificationController {

    private final NotificationService notificationService;

    @DeleteMapping("/{id}")
    public R<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return R.success();
    }

    @DeleteMapping("/batch")
    public R<Integer> deleteNotifications(@RequestBody List<Long> ids) {
        return R.success(notificationService.deleteNotifications(ids));
    }

    @PutMapping("/{id}/read-status")
    public R<Notification> updateReadStatus(@PathVariable Long id, @RequestParam Integer isRead) {
        return R.success(notificationService.updateReadStatus(id, isRead));
    }

    @PutMapping("/mark-all-read")
    public R<Integer> markAllRead() {
        return R.success(notificationService.markAllRead());
    }

    @GetMapping("/{id}")
    public R<Notification> getNotificationById(@PathVariable Long id) {
        return R.success(notificationService.getNotificationById(id));
    }

    @GetMapping("/list")
    public R<PageInfo<Notification>> listNotifications(@RequestParam(required = false) String title,
                                                       @RequestParam(required = false) Integer noticeType,
                                                       @RequestParam(required = false) Integer level,
                                                       @RequestParam(required = false) Integer isRead,
                                                       @RequestParam(required = false) String refType,
                                                       @RequestParam(required = false)
                                                       @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                       LocalDate startDate,
                                                       @RequestParam(required = false)
                                                       @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                       LocalDate endDate,
                                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(notificationService.listNotifications(
                title, noticeType, level, isRead, refType,
                startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/statistics")
    public R<Map<String, Object>> statisticsNotifications() {
        return R.success(notificationService.statisticsNotifications());
    }
}
