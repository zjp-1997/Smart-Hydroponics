package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 专家咨询会话实体，对应 consult_session 表。
 *
 * <p>会话表只保存一段咨询关系的摘要信息，具体聊天内容全部存放在 consult_message 表。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultSession {

    /** 咨询会话ID。 */
    private Long id;

    /** 咨询用户ID，对应 user.id。 */
    private Long userId;

    /** 专家档案ID，对应 expert_profile.id。 */
    private Long expertId;

    /** 会话编号，用于前端和后台展示稳定业务编号。 */
    private String sessionNo;

    /** 会话标题，默认取用户首次咨询内容摘要。 */
    private String sessionTitle;

    /** 会话状态：1进行中，2已结束，0已删除。 */
    private Integer status;

    /** 最后一条消息摘要，用于列表快速展示。 */
    private String lastMessageContent;

    /** 最后一条消息发送时间，用于会话排序。 */
    private LocalDateTime lastMessageTime;

    /** 用户未读消息数量。 */
    private Integer userUnreadCount;

    /** 专家未读消息数量。 */
    private Integer expertUnreadCount;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /** 关联查询得到的用户账号，不在 consult_session 表冗余存储。 */
    private String username;

    /** 关联查询得到的用户昵称，不在 consult_session 表冗余存储。 */
    private String nickname;

    /** 专家消息列表展示咨询用户头像，仅通过关联 user 表获取。 */
    private String userAvatar;

    /** 关联查询得到的专家姓名，不在 consult_session 表冗余存储。 */
    private String expertName;

    /** 关联查询得到的专家头像，不在 consult_session 表冗余存储。 */
    private String expertAvatar;

    /** 关联查询得到的专家账号用户ID，用于消息接收人落库。 */
    private Long expertUserId;
}
