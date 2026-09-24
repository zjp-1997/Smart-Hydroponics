package com.smart_plant.smart_plant.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 专家工作台聊天详情；姓名是咨询发起人的昵称，消息角色以当前专家为视角。 */
public record ExpertChatDetailResponse(Long sessionId, String userName, LocalDateTime chatTime,
                                       List<ClientExpertChatMessageResponse> messages,
                                       boolean hasMoreBefore, boolean hasMoreAfter) {
}
