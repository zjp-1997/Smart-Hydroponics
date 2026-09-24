package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 专家咨询消息实体，对应 consult_message 表。
 *
 * <p>每条消息只记录发送方、接收方、消息内容和阅读状态，避免在消息表冗余专家或用户资料。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultMessage {

    /** 咨询消息ID。 */
    private Long id;

    /** 所属咨询会话ID。 */
    private Long sessionId;

    /** 发送人用户ID。 */
    private Long senderId;

    /** 接收人用户ID。 */
    private Long receiverId;

    /** 消息类型：1文本，2图片，3语音，4文件。 */
    private Integer messageType;

    /** 文本消息内容。 */
    private String content;

    /** 图片、语音或文件地址。 */
    private String mediaUrl;

    /** 是否已读：0未读，1已读。 */
    private Integer isRead;

    /** 阅读时间。 */
    private LocalDateTime readTime;

    /** 状态：1正常，0删除。 */
    private Integer status;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /** 关联查询得到的会话编号。 */
    private String sessionNo;

    /** 关联查询得到的咨询用户账号。 */
    private String username;

    /** 关联查询得到的咨询用户昵称。 */
    private String nickname;

    /** 关联查询得到的专家姓名。 */
    private String expertName;

    /** 关联查询得到的发送人账号。 */
    private String senderName;

    /** 关联查询得到的接收人账号。 */
    private String receiverName;
}
