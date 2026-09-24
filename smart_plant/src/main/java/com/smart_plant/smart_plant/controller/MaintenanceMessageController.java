package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.MaintenanceMessage;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.MaintenanceMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import com.smart_plant.smart_plant.dto.MaintenanceMessageRequest;
import com.smart_plant.smart_plant.entity.IotDevice;

/** smart_farm维护消息独立入口，菜单权限与数据权限分别校验。 */
@RestController
@RequestMapping("/maintenance-msg")
@RequiredArgsConstructor
@RequirePermission("maintenance_msg:manage")
public class MaintenanceMessageController {
    private final MaintenanceMessageService service;

    /** 按消息标题、内容、地块查询，返回标准PageInfo分页结构。 */
    @GetMapping("/list")
    public R<PageInfo<MaintenanceMessage>> list(@RequestParam(required=false) String title,
            @RequestParam(required=false) String content, @RequestParam(required=false) String plotName,
            @RequestParam(defaultValue="1") int pageNum, @RequestParam(defaultValue="10") int pageSize) {
        return R.success(service.list(title, content, plotName, pageNum, pageSize, false));
    }

    /** 消息总数、今日发布、未读送达、已读送达四项统计。 */
    @GetMapping("/statistics")
    public R<Map<String,Object>> statistics() { return R.success(service.statistics(false)); }

    /** 查看消息快照，不修改故障本体。 */
    @GetMapping("/{id}")
    public R<MaintenanceMessage> detail(@PathVariable Long id) { return R.success(service.detail(id, false)); }

    /** 维护消息专属设备候选，按登录用户归属/绑定关系过滤。 */
    @GetMapping("/devices")
    public R<List<IotDevice>> devices(@RequestParam(required=false) String keyword) { return R.success(service.devices(keyword)); }

    /** 人工发布不新增或修改故障记录。 */
    @PostMapping("/add")
    public R<MaintenanceMessage> publish(@RequestBody MaintenanceMessageRequest request) { return R.success(service.publish(request)); }

    /** 编辑消息本体并同步全部通知明细。 */
    @PutMapping("/{id}")
    public R<MaintenanceMessage> update(@PathVariable Long id, @RequestBody MaintenanceMessageRequest request) {
        return R.success(service.update(id, request));
    }

    /** 删除单条消息本体及整组接收明细。 */
    @DeleteMapping("/{id}")
    public R<Integer> delete(@PathVariable Long id) { return R.success(service.delete(List.of(id))); }

    /** 同一事务中批量删除，禁止部分成功。 */
    @DeleteMapping("/batch")
    public R<Integer> batchDelete(@RequestBody List<Long> ids) { return R.success(service.delete(ids)); }

    /** 标记当前接收人已读；重复调用保留首次阅读时间。 */
    @PutMapping("/{id}/read")
    public R<Void> read(@PathVariable Long id) { service.markRead(id); return R.success(); }
}
