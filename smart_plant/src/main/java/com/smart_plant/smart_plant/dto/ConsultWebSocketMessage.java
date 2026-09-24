package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 专家咨询 WebSocket 推送消息。
 *
 * <p>第一阶段仍由 HTTP 接口负责发送和落库，WebSocket 只把已落库的消息实时推送给在线用户。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultWebSocketMessage {

    /** 消息事件类型，前端据此区分咨询消息、心跳或后续扩展事件。 */
    private String type;

    /** 咨询会话ID。 */
    private Long sessionId;

    /** 消息ID，用于前端做去重处理。 */
    private Long messageId;

    /** 专家档案ID。 */
    private Long expertId;

    /** 专家姓名。 */
    private String expertName;

    /** 发送人用户ID。 */
    private Long senderId;

    /** 接收人用户ID。 */
    private Long receiverId;

    /** 当前接收端视角的角色：user 或 expert。 */
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
