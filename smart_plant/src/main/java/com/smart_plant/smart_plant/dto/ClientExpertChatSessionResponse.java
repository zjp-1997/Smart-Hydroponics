package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * farm 用户端专家聊天会话列表响应对象。
 *
 * <p>该 DTO 面向消息页展示，只暴露专家头像、专家姓名、未读数、最后消息和聊天时间。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientExpertChatSessionResponse {

    /** 咨询会话ID，点击消息项进入聊天页时继续复用该会话。 */
    private Long sessionId;

    /** 专家档案ID，用于继续向指定专家发送消息。 */
    private Long expertId;

    /** 专家头像地址。 */
    private String expertAvatar;

    /** 专家姓名。 */
    private String expertName;

    /** 最近一次聊天内容。 */
    private String lastMessageContent;

    /** 最近一次聊天时间。 */
    private LocalDateTime chatTime;

    /** 当前用户维度的未读消息数量。 */
    private Integer unreadCount;
}
