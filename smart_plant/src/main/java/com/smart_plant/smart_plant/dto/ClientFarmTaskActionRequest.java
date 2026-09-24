package com.smart_plant.smart_plant.dto;

import lombok.Data;

/**
 * farm 用户端农事执行命令参数。
 *
 * <p>开始、进度和完成共用该结构，服务层会根据动作类型校验必填字段；
 * requestId 用于抵御移动网络重试和按钮连点产生的重复事件。</p>
 */
@Data
public class ClientFarmTaskActionRequest {

    /** 客户端生成的幂等请求号，最长 64 个字符。 */
    private String requestId;

    /** 本次执行动作的简要说明。 */
    private String actionContent;

    /** 当前任务进度百分比，仅提交过程进度时使用。 */
    private Integer progressPercent;

    /** 执行结果：1有效、2部分有效、3无效，仅完成动作使用。 */
    private Integer resultStatus;

    /** 执行反馈或完成说明。 */
    private String feedbackDetail;

    /** 附件地址 JSON 数组；完成任务时必须包含一张本系统上传的现场图片。 */
    private String attachments;
}
