package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.Notification;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** farm 端系统公告接口；用户身份只从登录态读取，不允许客户端指定接收人。 */
@RestController
@RequestMapping("/client/system-announcements")
@RequiredArgsConstructor
public class ClientSystemAnnouncementController {

    private final NotificationService notificationService;

    /** 分页返回本人收到的公告及每条公告的已读状态。 */
    @GetMapping("/list")
    public R<PageInfo<Notification>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.success(notificationService.listClientAnnouncements(pageNum, pageSize));
    }

    /** 标题与消息入口共用此未读总数，不受当前页限制。 */
    @GetMapping("/statistics")
    public R<Map<String, Long>> statistics() {
        return R.success(Map.of("unreadDeliveryCount", notificationService.countClientUnreadAnnouncements()));
    }

    /** 仅允许当前接收人阅读自己的公告。 */
    @PutMapping("/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        notificationService.markClientAnnouncementRead(id);
        return R.success();
    }

    /** 一键已读只作用于当前用户的系统公告。 */
    @PutMapping("/mark-all-read")
    public R<Integer> markAllRead() {
        return R.success(notificationService.markAllClientAnnouncementsRead());
    }
}
