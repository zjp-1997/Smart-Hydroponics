package com.smart_plant.smart_plant.dto;

import lombok.Data;

/** 发送成员聊天消息的请求；发送人始终从登录令牌读取。 */
@Data
public class FarmChatSendRequest {
    private Long sessionId;
    private Long peerUserId;
    private Integer messageType;
    private String content;
    private String mediaUrl;
}
