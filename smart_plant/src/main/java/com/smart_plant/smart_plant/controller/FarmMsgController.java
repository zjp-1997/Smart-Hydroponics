package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.FarmMessageRequest;
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
 * 农事消息管理控制器。
 *
 * <p>该入口面向 smart_farm 后台“消息通知管理 / 农事消息管理”页面，
 * 底层使用 notification 作为消息本体表，并通过 task_id 关联 farm_task 任务本体。</p>
 */
@RestController
@RequestMapping("/farm-msg")
@RequiredArgsConstructor
@RequirePermission("farm_msg:manage")
public class FarmMsgController {

    /** 复用通知领域服务，农事消息是 notice_type=2 且关联 task_id 的消息聚合视图。 */
    private final NotificationService notificationService;

    /** 发布农事消息：只新增消息接收明细，不创建或修改 farm_task 任务本体。 */
    @PostMapping("/add")
    public R<Notification> addFarmMsg(@RequestBody FarmMessageRequest request) {
        return R.success(notificationService.publishFarmMessage(request));
    }

    /** 编辑农事消息：只更新消息标题、内容、级别和 task_id 关联。 */
    @PutMapping
    public R<Notification> updateFarmMsg(@RequestParam Long id,
                                         @RequestBody FarmMessageRequest request) {
        return R.success(notificationService.updateFarmMessage(id, request));
    }

    /** 删除农事消息：按消息组逻辑删除 notification 明细，保留任务和反馈。 */
    @DeleteMapping("/{id}")
    public R<Void> deleteFarmMsg(@PathVariable Long id) {
        notificationService.deleteFarmMessage(id);
        return R.success();
    }

    /** 批量删除农事消息：入参为消息列表ID，服务层会转换成消息组ID。 */
    @DeleteMapping("/batch")
    public R<Integer> deleteFarmMsgs(@RequestBody List<Long> ids) {
        return R.success(notificationService.deleteFarmMessages(ids));
    }

    /** 查询农事消息详情：返回消息本体、关联任务、接收人数和已读数量等字段。 */
    @GetMapping("/{id}")
    public R<Notification> getFarmMsgById(@PathVariable Long id) {
        return R.success(notificationService.getFarmMessageById(id));
    }

    /** 分页查询农事消息：主查询条件为消息标题、消息内容和地块名称。 */
    @GetMapping("/list")
    public R<PageInfo<Notification>> listFarmMsgs(@RequestParam(required = false) String plotName,
                                                 @RequestParam(required = false) String title,
                                                 // 兼容旧版管理端参数，升级期间不影响已部署客户端。
                                                 @RequestParam(required = false) String taskTitle,
                                                 @RequestParam(required = false) String content,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                 LocalDate startDate,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                 LocalDate endDate,
                                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(notificationService.listFarmMessages(
                title != null && !title.isBlank() ? title : taskTitle,
                content, plotName, startDate, endDate, pageNum, pageSize));
    }

    /** 查询农事消息统计：返回消息组总数、今日发布和已读/未读送达数。 */
    @GetMapping("/statistics")
    public R<Map<String, Object>> statisticsFarmMsgs() {
        return R.success(notificationService.statisticsFarmMessages());
    }
}
