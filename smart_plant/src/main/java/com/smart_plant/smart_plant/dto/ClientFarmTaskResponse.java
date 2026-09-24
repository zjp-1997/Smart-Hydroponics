package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * farm 用户端农事任务列表项。
 *
 * <p>该 DTO 面向移动端任务列表 UI，聚合任务、地块和作物名称，
 * 减少前端二次请求并隔离后台管理字段。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientFarmTaskResponse {

    /** 农事任务主键。 */
    private Long id;

    /** 任务所属地块 ID。 */
    private Long plotId;

    /** 任务所属地块名称。 */
    private String plotName;

    /** 当前种植批次对应的作物名称。 */
    private String cropName;

    /** 当前种植批次或作物档案中的图片地址，供任务详情卡片展示。 */
    private String cropImage;

    /** 任务标题，例如：浇水、施肥。 */
    private String taskTitle;

    /** 任务执行要求或注意事项。 */
    private String taskContent;

    /** 任务类型：1灌溉/浇水、2施肥、3喷药、4采收。 */
    private Integer taskType;

    /** 任务类型中文名，供前端兜底展示。 */
    private String taskTypeName;

    /** 任务状态：1未开始、2进行中、3已完成、4已逾期、5已取消。 */
    private Integer status;

    /** 任务状态中文名。 */
    private String statusName;

    /** 任务截至时间，移动端日期列表按该时间归类。 */
    private LocalDateTime deadlineTime;

    /** 任务创建时间，用于补全移动端农事记录的“未开始”状态节点。 */
    private LocalDateTime createTime;

    /** 实际开始时间，由开始执行命令自动记录。 */
    private LocalDateTime actualStartTime;

    /** 实际完成时间，由完成任务命令自动记录。 */
    private LocalDateTime actualEndTime;

    /** 当前执行人ID。 */
    private Long executorId;

    /** 当前执行人展示名称。 */
    private String executorName;

    /** 完成任务时填写的结果说明。 */
    private String completeRemark;

    /** 是否超过截至时间；该字段与任务业务状态相互独立。 */
    private Boolean overdue;

    /** 是否允许用户点击“执行任务”。 */
    private Boolean canExecute;

    /** 是否允许用户提交过程进度。 */
    private Boolean canSubmitProgress;

    /** 是否允许用户完成任务。 */
    private Boolean canComplete;
}
