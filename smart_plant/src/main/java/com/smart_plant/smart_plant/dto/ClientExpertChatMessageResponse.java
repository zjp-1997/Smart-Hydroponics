package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * farm 用户端专家聊天消息响应对象。
 *
 * <p>该 DTO 只暴露聊天页渲染需要的消息内容和发送角色，避免把发送人账号等后台字段直接返回给用户端。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientExpertChatMessageResponse {

    /** 消息ID，用作前端列表渲染的稳定 key。 */
    private Long messageId;

    /** 消息角色：user 表示当前登录用户发送，expert 表示专家发送。 */
    private String role;

    /** 消息类型：1文本，2图片，3语音，4文件。 */
    private Integer messageType;

    /** 文本消息内容。 */
    private String content;

    /** 图片、语音或文件地址。 */
    private String mediaUrl;

    /** 消息发送时间。 */
    private LocalDateTime createTime;
}
