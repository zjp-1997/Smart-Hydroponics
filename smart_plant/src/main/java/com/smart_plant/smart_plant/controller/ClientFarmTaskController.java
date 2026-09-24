package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientFarmTaskDateSummaryResponse;
import com.smart_plant.smart_plant.dto.ClientFarmTaskActionRequest;
import com.smart_plant.smart_plant.dto.ClientFarmTaskResponse;
import com.smart_plant.smart_plant.dto.ClientFarmTaskStatisticsResponse;
import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.entity.FarmTaskRecord;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.ClientFarmTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * farm 用户端农事任务接口。
 *
 * <p>该接口供移动端首页“农事管理”使用，用户身份由 token 解析，
 * 不从请求参数接收 userId，保证数据访问边界清晰。</p>
 */
@RestController
@RequestMapping({"/client/farm-tasks", "/api/client/farm-tasks"})
@RequiredArgsConstructor
public class ClientFarmTaskController {

    private final ClientFarmTaskService clientFarmTaskService;

    @GetMapping("/dates")
    public R<List<ClientFarmTaskDateSummaryResponse>> listDateSummaries(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate) {
        // 返回连续 7 天任务数量，前端日期条无需自行聚合列表数据。
        return R.success(clientFarmTaskService.listDateSummaries(startDate));
    }

    @GetMapping("/list")
    public R<List<ClientFarmTaskResponse>> listTasks(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate taskDate,
            @RequestParam(required = false) Integer taskType) {
        // 根据日期和类型筛选任务；taskType 为空时表示“全部”。
        return R.success(clientFarmTaskService.listTasks(taskDate, taskType));
    }

    @GetMapping("/mine")
    public R<List<ClientFarmTaskResponse>> listMyTasks(
            @RequestParam(required = false) Integer status) {
        // 我的农事不限制日期；状态为空时返回当前用户全部未取消任务。
        return R.success(clientFarmTaskService.listMyTasks(status));
    }

    @GetMapping("/plot/{plotId}")
    public R<List<ClientFarmTaskResponse>> listTasksByPlot(@PathVariable Long plotId) {
        // 地块农事时间序列一次读取完整任务视图，避免移动端逐条请求任务详情。
        return R.success(clientFarmTaskService.listTasksByPlot(plotId));
    }

    @GetMapping("/plot/{plotId}/timeline")
    public R<List<FarmTaskRecord>> listPlotTimeline(@PathVariable Long plotId) {
        // 返回地块下各任务、各操作人的完整事件流，供 farm 端统一按时间展示。
        return R.success(clientFarmTaskService.listPlotTimeline(plotId));
    }

    @GetMapping("/{id}")
    public R<ClientFarmTaskResponse> getTaskDetail(@PathVariable Long id) {
        // 详情页所需任务、地块和作物信息一次返回，并由服务层校验当前用户的数据权限。
        return R.success(clientFarmTaskService.getTaskDetail(id));
    }

    @PostMapping("/{id}/completion-image")
    public R<CropImageUploadResult> uploadCompletionImage(@PathVariable Long id,
                                                           @RequestParam("image") MultipartFile image) {
        // 图片先按任务归属进行鉴权，再存入独立的完成凭证目录。
        return R.success(clientFarmTaskService.uploadCompletionImage(id, image));
    }

    @GetMapping("/statistics")
    public R<ClientFarmTaskStatisticsResponse> statistics() {
        // 首页只读取当前用户的轻量聚合结果，不加载任务明细。
        return R.success(clientFarmTaskService.statistics());
    }

    @PutMapping("/{id}/execute")
    public R<ClientFarmTaskResponse> executeTask(@PathVariable Long id,
                                                  @RequestBody(required = false) ClientFarmTaskActionRequest request) {
        // 保留旧路径兼容已发布客户端，但内部已使用统一的事务化执行时间线。
        return R.success(clientFarmTaskService.executeTask(id, request));
    }

    @PostMapping("/{id}/actions/start")
    public R<ClientFarmTaskResponse> startTask(@PathVariable Long id,
                                                @RequestBody(required = false) ClientFarmTaskActionRequest request) {
        // 新版客户端使用语义明确的开始命令，避免直接提交任意 status 值。
        return R.success(clientFarmTaskService.executeTask(id, request));
    }

    @PostMapping("/{id}/actions/progress")
    public R<ClientFarmTaskResponse> submitProgress(@PathVariable Long id,
                                                     @RequestBody ClientFarmTaskActionRequest request) {
        // 过程反馈只追加不可变事件，不改变任务的进行中状态。
        return R.success(clientFarmTaskService.submitProgress(id, request));
    }

    @PostMapping("/{id}/actions/complete")
    public R<ClientFarmTaskResponse> completeTask(@PathVariable Long id,
                                                   @RequestBody ClientFarmTaskActionRequest request) {
        // 完成命令会在同一事务中更新任务快照并追加完成事件。
        return R.success(clientFarmTaskService.completeTask(id, request));
    }

    @GetMapping("/{id}/timeline")
    public R<List<FarmTaskRecord>> listTimeline(@PathVariable Long id) {
        // 时间线按业务发生时间顺序返回，供 farm 端还原完整执行过程。
        return R.success(clientFarmTaskService.listTimeline(id));
    }
}
