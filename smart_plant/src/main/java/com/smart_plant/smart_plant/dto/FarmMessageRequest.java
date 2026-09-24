package com.smart_plant.smart_plant.dto;

import lombok.Data;

import java.util.List;

/**
 * 农事消息发布和编辑请求。
 *
 * <p>农事消息只保存消息本体字段，通过 taskId 关联 farm_task，
 * 不再创建、删除或修改农事任务本体。</p>
 */
@Data
public class FarmMessageRequest {

    /** 关联农事任务ID，对应 notification.task_id。 */
    private Long taskId;

    /** 消息标题，用于后台列表和用户端通知摘要展示。 */
    private String title;

    /** 消息正文内容，用于承载完整通知文本。 */
    private String content;

    /** 消息级别：1普通，2重要，3紧急。 */
    private Integer level;

    /** 指定接收人ID列表；为空时默认发送给任务负责人和任务归属用户。 */
    private List<Long> recipientIds;
}
