package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * farm 用户端发送专家咨询消息后的响应对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientExpertChatResponse {

    /** 咨询会话ID，后续继续发送消息时可复用。 */
    private Long sessionId;

    /** 本次发送的消息ID。 */
    private Long messageId;

    /** 专家档案ID。 */
    private Long expertId;

    /** 专家姓名。 */
    private String expertName;

    /** 用户发送内容。 */
    private String content;

    /** 媒体文件地址。 */
    private String mediaUrl;

    /** 消息发送时间。 */
    private LocalDateTime createTime;
}
