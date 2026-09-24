package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FarmTaskRecord {

    private Long id;

    private Long taskId;

    private String taskTitle;

    private Long userId;

    private Long plotId;

    private String plotName;

    private Long operatorId;

    /** 操作人名称快照，避免用户改名后历史时间线失真。 */
    private String operatorNameSnapshot;

    private String operatorName;

    private Integer actionType;

    private String actionContent;

    private Integer resultStatus;

    /** 本次过程反馈的完成百分比，取值范围为 0-100。 */
    private Integer progressPercent;

    private Integer feedbackScore;

    private String feedbackDetail;

    private String optimizeSuggestion;

    /** 执行凭证附件地址的 JSON 数组，为后续图片上传能力预留。 */
    private String attachments;

    /** 客户端幂等请求号，防止弱网重试或连续点击重复生成事件。 */
    private String requestId;

    /** 事件来源：FARM_APP、SMART_FARM 或 SYSTEM。 */
    private String sourceClient;

    private Integer beforeStatus;

    private Integer afterStatus;

    private LocalDateTime executeTime;

    private LocalDateTime createTime;
}
