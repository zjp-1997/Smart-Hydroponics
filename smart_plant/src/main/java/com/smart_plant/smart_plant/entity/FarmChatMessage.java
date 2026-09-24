package com.smart_plant.smart_plant.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 农场成员聊天消息，发送人和接收人均使用 user 表主键。 */
@Data
public class FarmChatMessage {
    private Long id;
    private Long sessionId;
    private Long senderId;
    private Long receiverId;
    private Integer messageType;
    private String content;
    private String mediaUrl;
    private Integer isRead;
    private LocalDateTime readTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
