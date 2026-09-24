package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/** 消息保存成功后的最小响应，供前端更新会话ID并去重。 */
@Data
@AllArgsConstructor
public class FarmChatSendResponse {
    private Long sessionId;
    private Long messageId;
    private Long peerUserId;
    private String content;
    private String mediaUrl;
    private LocalDateTime createTime;
}
