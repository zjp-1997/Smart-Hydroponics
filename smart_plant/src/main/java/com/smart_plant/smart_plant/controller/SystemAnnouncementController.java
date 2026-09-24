package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.SystemAnnouncementRequest;
import com.smart_plant.smart_plant.entity.Notification;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 系统公告管理控制器。
 *
 * <p>该控制器面向 smart_farm 后台“系统公告”二级菜单，只暴露公告级别的发布、编辑、查询和删除接口。</p>
 */
@RestController
@RequestMapping("/sys-msg")
@RequiredArgsConstructor
@RequirePermission("sys_msg:manage")
public class SystemAnnouncementController {

    /** 复用通知领域服务，系统公告是 notice_type=1 的通知聚合视图。 */
    private final NotificationService notificationService;

    /** 发布公告：后端根据启用用户生成接收明细，前端不提交接收人列表。 */
    @PostMapping("/add")
    public R<Notification> publishAnnouncement(@RequestBody SystemAnnouncementRequest request) {
        return R.success(notificationService.publishSystemAnnouncement(request));
    }

    /** 编辑公告：按公告组更新所有接收明细，保证用户侧看到的公告内容一致。 */
    @PutMapping
    public R<Notification> updateAnnouncement(@RequestParam Long id,
                                              @RequestBody SystemAnnouncementRequest request) {
        return R.success(notificationService.updateSystemAnnouncement(id, request));
    }

    /** 删除公告：执行逻辑删除，保留历史数据可审计。 */
    @DeleteMapping("/{id}")
    public R<Void> deleteAnnouncement(@PathVariable Long id) {
        notificationService.deleteSystemAnnouncement(id);
        return R.success();
    }

    /** 批量删除公告：入参为公告列表ID，服务层会转换成公告组ID。 */
    @DeleteMapping("/batch")
    public R<Integer> deleteAnnouncements(@RequestBody List<Long> ids) {
        return R.success(notificationService.deleteSystemAnnouncements(ids));
    }

    /** 查询公告详情：返回公告标题、内容、发布人、接收人数和已读数量等展示字段。 */
    @GetMapping("/{id}")
    public R<Notification> getAnnouncementById(@PathVariable Long id) {
        return R.success(notificationService.getSystemAnnouncementById(id));
    }

    /** 分页查询公告列表：支持公告标题、公告内容和发布时间范围查询。 */
    @GetMapping("/list")
    public R<PageInfo<Notification>> listAnnouncements(@RequestParam(required = false) String title,
                                                       @RequestParam(required = false) String content,
                                                       @RequestParam(required = false)
                                                       @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                       LocalDate startDate,
                                                       @RequestParam(required = false)
                                                       @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                       LocalDate endDate,
                                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(notificationService.listSystemAnnouncements(
                title, content, startDate, endDate, pageNum, pageSize));
    }

    /** 查询系统公告统计数据，用于页面顶部统计卡片。 */
    @GetMapping("/statistics")
    public R<Map<String, Object>> statisticsAnnouncements() {
        return R.success(notificationService.statisticsSystemAnnouncements());
    }
}
