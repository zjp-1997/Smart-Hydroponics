package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 聊天详情中的单条消息；role 仅表示当前账号视角的 self 或 peer。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FarmChatMessageResponse {
    private Long messageId;
    private String role;
    private Integer messageType;
    private String content;
    private String mediaUrl;
    private LocalDateTime createTime;
}
