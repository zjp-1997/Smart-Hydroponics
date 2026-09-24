package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.dto.FarmChatSessionResponse;
import com.smart_plant.smart_plant.entity.FarmChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 农场成员聊天会话持久层。 */
@Mapper
public interface FarmChatSessionMapper {
    int insert(FarmChatSession session);

    FarmChatSession selectById(Long id);

    /** 按参与双方查询唯一会话，首次发送时用于避免重复创建。 */
    FarmChatSession selectByParticipants(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId);

    /** 农场主看到全部有效绑定成员，即使双方还没有发送过消息。 */
    List<FarmChatSessionResponse> selectOwnerSessionList(@Param("ownerId") Long ownerId);

    /** 普通用户只看到注册时绑定的农场主。 */
    List<FarmChatSessionResponse> selectUserSessionList(@Param("userId") Long userId);

    /** 技术人员看到全部处于有效绑定状态的农场主。 */
    List<FarmChatSessionResponse> selectTechnicianSessionList(@Param("technicianId") Long technicianId);

    /** 保存最后消息摘要，同时只增加接收方未读数。 */
    int updateLastMessage(@Param("id") Long id,
                          @Param("lastMessageContent") String lastMessageContent,
                          @Param("senderIsOwner") boolean senderIsOwner);

    /** 当前用户进入详情后清空自己一侧的未读数。 */
    int resetUnreadCount(@Param("id") Long id, @Param("currentUserId") Long currentUserId);
}
