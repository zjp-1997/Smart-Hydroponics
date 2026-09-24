package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.FarmTask;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.FarmTaskService;
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

@RestController
@RequestMapping("/farm-task")
@RequiredArgsConstructor
@RequirePermission("farm_task:manage")
public class FarmTaskController {

    /** 农事任务领域服务，统一承接任务计划、执行状态和反馈闭环。 */
    private final FarmTaskService farmTaskService;

    /** 新增农事任务：创建一条可被移动端执行和反馈的任务本体。 */
    @PostMapping("/add")
    public R<FarmTask> addFarmTask(@RequestBody FarmTask farmTask) {
        return R.success(farmTaskService.addFarmTask(farmTask));
    }

    /** 编辑农事任务：按 ID 修改标题、内容、类型、地块和状态等任务字段。 */
    @PutMapping
    public R<FarmTask> updateFarmTask(@RequestBody FarmTask farmTask) {
        return R.success(farmTaskService.updateFarmTask(farmTask));
    }

    /** 删除农事任务：同步清理执行反馈和 AI 方案关联。 */
    @DeleteMapping("/{id}")
    public R<Void> deleteFarmTask(@PathVariable Long id) {
        farmTaskService.deleteFarmTask(id);
        return R.success();
    }

    /** 批量删除农事任务：入参为表格勾选的任务 ID 列表。 */
    @DeleteMapping("/batch")
    public R<Integer> deleteFarmTasks(@RequestBody List<Long> ids) {
        return R.success(farmTaskService.deleteFarmTasks(ids));
    }

    /** 查询农事任务详情：返回标题、地块、计划时间和执行反馈所需字段。 */
    @GetMapping("/{id}")
    public R<FarmTask> getFarmTaskById(@PathVariable Long id) {
        return R.success(farmTaskService.getFarmTaskById(id));
    }

    /** 更新农事状态：用于详情抽屉中的开始执行、完成任务等操作。 */
    @PutMapping("/{id}/status")
    public R<FarmTask> updateTaskStatus(@PathVariable Long id,
                                        @RequestParam Integer status,
                                        @RequestParam(required = false) String completeRemark) {
        return R.success(farmTaskService.updateTaskStatus(id, status, completeRemark));
    }

    /** 分页查询农事任务：支持任务标题、任务内容、地块、状态和计划时间等条件。 */
    @GetMapping("/list")
    public R<PageInfo<FarmTask>> listFarmTasks(@RequestParam(required = false) String plotName,
                                               @RequestParam(required = false) String taskTitle,
                                               @RequestParam(required = false) String taskContent,
                                               @RequestParam(required = false) Integer taskType,
                                               @RequestParam(required = false) Integer status,
                                               @RequestParam(required = false) String sourceType,
                                               @RequestParam(required = false)
                                               @DateTimeFormat(pattern = "yyyy-MM-dd")
                                               LocalDate startDate,
                                               @RequestParam(required = false)
                                               @DateTimeFormat(pattern = "yyyy-MM-dd")
                                               LocalDate endDate,
                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(farmTaskService.listFarmTasks(
                plotName, taskTitle, taskContent, taskType, status, sourceType,
                startDate, endDate, pageNum, pageSize));
    }

    /** 查询农事任务统计：返回任务总数、待处理数、进行中数和已完成数等聚合结果。 */
    @GetMapping("/statistics")
    public R<Map<String, Object>> statisticsFarmTasks() {
        return R.success(farmTaskService.statisticsFarmTasks());
    }
}
