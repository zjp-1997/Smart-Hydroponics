package com.smart_plant.smart_plant.dto;

import lombok.Data;

/**
 * farm 用户端向专家发送消息的请求对象。
 */
@Data
public class ClientExpertChatRequest {

    /** 专家档案ID；为空时后端自动选择第一位可咨询专家。 */
    private Long expertId;

    /** 已有会话ID；为空时后端创建或复用当前用户与专家的进行中会话。 */
    private Long sessionId;

    /** 消息类型：1文本，2图片，3语音，4文件。 */
    private Integer messageType;

    /** 文本消息内容。 */
    private String content;

    /** 图片、语音或文件地址。 */
    private String mediaUrl;
}
