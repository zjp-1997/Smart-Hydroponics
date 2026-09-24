package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** 农场成员聊天详情，统一供农场主、普通用户和技术人员使用。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FarmChatDetailResponse {
    private Long sessionId;
    private Long peerUserId;
    private String peerName;
    private String peerAvatar;
    private String peerRoleCode;
    private LocalDateTime chatTime;
    private List<FarmChatMessageResponse> messages;
    /** beforeId 查询是否还有更早消息。 */
    private boolean hasMoreBefore;
    /** afterId 增量查询是否还有尚未返回的新消息。 */
    private boolean hasMoreAfter;
}
