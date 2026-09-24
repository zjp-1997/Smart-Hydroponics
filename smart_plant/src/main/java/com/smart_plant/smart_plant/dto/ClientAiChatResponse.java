package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * farm 用户端 AI 咨询响应对象。
 *
 * <p>字段贴近移动端展示需要，同时保留 chatId 便于后续查看历史或反馈。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientAiChatResponse {

    /** 对话记录ID。 */
    private Long chatId;

    /** 使用的模型配置ID。 */
    private Long modelId;

    /** 模型展示名称。 */
    private String modelName;

    /** 用户发送内容。 */
    private String userContent;

    /** 用户上传图片地址，可为空。 */
    private String imageUrl;

    /** 用户上传文档地址，可为空。 */
    private String fileUrl;

    /** 文件名称，供移动端消息气泡展示。 */
    private String fileName;

    /** AI回复内容。 */
    private String aiContent;
}
