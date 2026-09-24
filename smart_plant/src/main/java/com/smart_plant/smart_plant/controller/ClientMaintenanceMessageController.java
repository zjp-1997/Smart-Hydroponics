package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.MaintenanceMessage;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.MaintenanceMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** farm移动端个人维护通知，只查询本人接收明细。 */
@RestController
@RequestMapping("/client/maintenance-msg")
@RequiredArgsConstructor

public class ClientMaintenanceMessageController {
    private final MaintenanceMessageService service;

    /** 按消息标题、内容、地块查询，返回标准PageInfo分页结构。 */
    @GetMapping("/list")
    public R<PageInfo<MaintenanceMessage>> list(@RequestParam(required=false) String title,
            @RequestParam(required=false) String content, @RequestParam(required=false) String plotName,
            @RequestParam(defaultValue="1") int pageNum, @RequestParam(defaultValue="10") int pageSize) {
        return R.success(service.list(title, content, plotName, pageNum, pageSize, true));
    }

    /** 消息总数、今日发布、未读送达、已读送达四项统计。 */
    @GetMapping("/statistics")
    public R<Map<String,Object>> statistics() { return R.success(service.statistics(true)); }

    /** 查看消息快照，不修改故障本体。 */
    @GetMapping("/{id}")
    public R<MaintenanceMessage> detail(@PathVariable Long id) { return R.success(service.detail(id, true)); }

    /** 标记当前接收人已读；重复调用保留首次阅读时间。 */
    @PutMapping("/{id}/read")
    public R<Void> read(@PathVariable Long id) { service.markRead(id); return R.success(); }
}
