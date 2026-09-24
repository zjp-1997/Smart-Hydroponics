package com.smart_plant.smart_plant.dto;

/** 专家回复可携带文字或上传的附件地址；会话归属由服务端校验。 */
public record ExpertReplyRequest(String content, Integer messageType, String mediaUrl) {
}
