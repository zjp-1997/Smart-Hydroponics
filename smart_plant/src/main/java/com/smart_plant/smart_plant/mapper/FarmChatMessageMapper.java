package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.FarmChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 农场成员聊天消息持久层。 */
@Mapper
public interface FarmChatMessageMapper {
    int insert(FarmChatMessage message);

    int countMediaReferences(@Param("mediaUrl") String mediaUrl);

    /** 成员解绑后立即失去附件读取权；农场主保留本农场会话的历史访问权。 */
    int countAccessibleMedia(@Param("mediaUrl") String mediaUrl,
                             @Param("userId") Long userId,
                             @Param("admin") boolean admin);

    /** 按消息ID游标查询，beforeId加载历史，afterId只补拉新消息。 */
    List<FarmChatMessage> selectCursor(@Param("sessionId") Long sessionId,
                                       @Param("beforeId") Long beforeId,
                                       @Param("afterId") Long afterId,
                                       @Param("limit") int limit);

    /** 将当前账号收到的消息标为已读。 */
    int markReceivedMessagesRead(@Param("sessionId") Long sessionId,
                                 @Param("receiverId") Long receiverId);
}
