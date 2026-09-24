package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.ConsultMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 专家咨询消息持久层接口。
 */
@Mapper
public interface ConsultMessageMapper {

    /** 新增咨询消息。 */
    int insert(ConsultMessage message);

    /** 逻辑删除单条咨询消息。 */
    int deleteById(Long id);

    /** 逻辑批量删除咨询消息。 */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 根据ID查询咨询消息详情。 */
    ConsultMessage selectById(Long id);

    int countMediaReferences(@Param("mediaUrl") String mediaUrl);

    /** 附件只允许管理员或该专家咨询会话的有效参与人读取。 */
    int countAccessibleMedia(@Param("mediaUrl") String mediaUrl,
                             @Param("userId") Long userId,
                             @Param("admin") boolean admin);

    /** 查询当前用户指定会话下的聊天历史，确保用户只能读取自己的会话消息。 */
    List<ConsultMessage> selectClientMessagesBySessionId(@Param("sessionId") Long sessionId,
                                                         @Param("userId") Long userId);

    /** 将当前用户接收的未读消息标记为已读，用于进入聊天页后的阅读回执。 */
    int markClientMessagesRead(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    /** 专家只读取本人档案关联会话的消息，并标记发给自己的消息已读。 */
    List<ConsultMessage> selectExpertMessagesBySessionId(@Param("sessionId") Long sessionId,
                                                          @Param("expertUserId") Long expertUserId,
                                                          @Param("beforeId") Long beforeId,
                                                          @Param("afterId") Long afterId,
                                                          @Param("limit") int limit);

    int markExpertMessagesRead(@Param("sessionId") Long sessionId,
                               @Param("expertUserId") Long expertUserId);

    /** 分页条件查询咨询消息列表。 */
    List<ConsultMessage> selectList(@Param("scopedUserId") Long scopedUserId,
                                    @Param("username") String username,
                                    @Param("expertName") String expertName,
                                    @Param("sessionNo") String sessionNo,
                                    @Param("messageType") Integer messageType,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    /** 查询咨询消息统计数据。 */
    Map<String, Object> selectStatistics(@Param("scopedUserId") Long scopedUserId,
                                         @Param("username") String username,
                                         @Param("expertName") String expertName,
                                         @Param("sessionNo") String sessionNo,
                                         @Param("messageType") Integer messageType,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
}
