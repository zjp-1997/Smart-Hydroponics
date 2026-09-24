package com.smart_plant.smart_plant.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 农场主与普通用户或技术人员之间的一对一聊天会话。 */
@Data
public class FarmChatSession {
    private Long id;
    private String sessionNo;
    private Long farmOwnerId;
    private Long memberUserId;
    private String memberRole;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private Integer ownerUnreadCount;
    private Integer memberUnreadCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
