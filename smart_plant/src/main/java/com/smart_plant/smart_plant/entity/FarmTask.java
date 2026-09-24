package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FarmTask {

    /** 农事任务主键ID，对应 farm_task.id。 */
    private Long id;

    /** 农事归属用户ID，后台列表中同时作为发布人ID使用。 */
    private Long userId;

    /** 发布人登录账号，来自 user.username。 */
    private String username;

    /** 发布人昵称，优先用于页面展示。 */
    private String nickname;

    /** 农事所属地块ID，对应 farm_task.plot_id。 */
    private Long plotId;

    /** 地块名称，列表查询时由 plot 表关联得到。 */
    private String plotName;

    /** 地块编号，便于运营人员区分同名地块。 */
    private String plotCode;

    /** 种植批次ID，用于把农事和当前作物批次关联。 */
    private Long batchId;

    /** 批次编号，详情展示时由 planting_batch 表关联得到。 */
    private String batchNo;

    /** 农事标题，是管理端列表和条件查询的核心字段。 */
    private String taskTitle;

    /** 农事类型：1浇水 2施肥 3打药 4采收 5巡检 6除草 7补光 8其他。 */
    private Integer taskType;

    /** 农事内容，支持管理端按内容关键词检索。 */
    private String taskContent;

    /** 优先级：1低 2普通 3高 4紧急。 */
    private Integer priority;

    /** 任务截至时间，替代容易产生歧义的计划开始/计划结束区间。 */
    private LocalDateTime deadlineTime;

    /** 实际开始时间，由状态流转到进行中时自动记录。 */
    private LocalDateTime actualStartTime;

    /** 实际完成时间，由状态流转到已完成时自动记录。 */
    private LocalDateTime actualEndTime;

    /** 农事状态：1未开始 2进行中 3已完成 4已逾期 5已取消。 */
    private Integer status;

    /** 执行人用户ID。 */
    private Long executorId;

    /** 执行人名称，列表查询时由 user 表关联得到。 */
    private String executorName;

    /** 完成备注，用于记录执行闭环说明。 */
    private String completeRemark;

    /** 后台备注，不直接参与业务状态流转。 */
    private String remark;

    /** 来源类型，例如 AI_SOLUTION 表示由 AI 处置方案生成。 */
    private String sourceType;

    /** 来源业务ID，与 sourceType 共同定位来源记录。 */
    private Long sourceId;

    /** AI 处置方案ID，便于从识别结果追溯到农事任务。 */
    private Long aiSolutionId;

    /** 创建时间，管理端作为农事发布时间展示。 */
    private LocalDateTime createTime;

    /** 发布时间别名，便于前端按“农事消息”语义展示。 */
    private LocalDateTime publishTime;

    /** 发布人名称别名，优先取昵称，兜底取账号。 */
    private String publisherName;

    /** 更新时间，记录最后一次编辑或状态流转时间。 */
    private LocalDateTime updateTime;

    /** 是否已经超过截至时间，由查询层动态计算，不作为互斥业务状态持久化。 */
    private Boolean overdue;
}
