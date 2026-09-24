package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/** 已落库的成员消息 WebSocket 事件，客户端使用 messageId 去重。 */
@Data
@AllArgsConstructor
public class FarmChatWebSocketMessage {
    private String type;
    private Long sessionId;
    private Long messageId;
    private Long peerUserId;
    private Long senderId;
    private Long receiverId;
    private String role;
    private Integer messageType;
    private String content;
    private String mediaUrl;
    private LocalDateTime createTime;
}
