package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.FarmTask;

import java.time.LocalDate;
import java.util.Map;

public interface FarmTaskService {

    /** 新增农事任务，后端会根据地块归属自动确定农事发布人。 */
    FarmTask addFarmTask(FarmTask farmTask);

    /** 编辑农事任务，保留未提交字段的原值并重新校验地块归属。 */
    FarmTask updateFarmTask(FarmTask farmTask);

    /** 删除单条农事任务，同时清理执行反馈和 AI 方案关联。 */
    void deleteFarmTask(Long id);

    /** 批量删除农事任务，返回实际删除数量。 */
    int deleteFarmTasks(java.util.List<Long> ids);

    /** 查询农事任务详情，用于详情抽屉和编辑表单回填。 */
    FarmTask getFarmTaskById(Long id);

    /** 更新农事状态，并在开始/完成时自动写入实际执行时间。 */
    FarmTask updateTaskStatus(Long id, Integer status, String completeRemark);

    /** 按地块名称、任务标题、任务内容等条件分页查询农事任务列表。 */
    PageInfo<FarmTask> listFarmTasks(String plotName, String taskTitle, String taskContent,
                                     Integer taskType, Integer status, String sourceType,
                                     LocalDate startDate, LocalDate endDate,
                                     Integer pageNum, Integer pageSize);

    /** 查询农事统计卡片数据：总数、待处理、进行中和已完成等。 */
    Map<String, Object> statisticsFarmTasks();
}
