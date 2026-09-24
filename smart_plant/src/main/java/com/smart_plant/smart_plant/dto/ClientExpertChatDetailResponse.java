package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * farm 用户端专家聊天详情响应对象。
 *
 * <p>用于统一专家列表入口和消息列表入口的数据源，确保两个入口进入同一专家时展示同一段会话历史。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientExpertChatDetailResponse {

    /** 咨询会话ID；没有历史会话时为空，首次发送消息时由后端创建。 */
    private Long sessionId;

    /** 专家档案ID，用于后续继续向该专家发送消息。 */
    private Long expertId;

    /** 专家头像地址。 */
    private String expertAvatar;

    /** 专家姓名。 */
    private String expertName;

    /** 最近一次聊天时间，前端用于顶部时间展示。 */
    private LocalDateTime chatTime;

    /** 当前会话下按发送时间正序排列的消息列表。 */
    private List<ClientExpertChatMessageResponse> messages;
}
