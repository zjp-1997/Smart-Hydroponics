package com.smart_plant.smart_plant.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 消息首页使用的成员联系人及会话摘要。 */
@Data
public class FarmChatSessionResponse {
    /** 尚未发送消息的联系人没有会话ID，首次发送时由服务端创建。 */
    private Long sessionId;
    private Long peerUserId;
    private String peerName;
    private String peerAvatar;
    private String peerRoleCode;
    private String lastMessageContent;
    private LocalDateTime chatTime;
    private Integer unreadCount;
}
