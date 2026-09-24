package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.ConsultSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专家咨询会话持久层接口。
 */
@Mapper
public interface ConsultSessionMapper {

    /** 新增咨询会话。 */
    int insert(ConsultSession session);

    /** 根据ID查询会话详情。 */
    ConsultSession selectById(Long id);

    /** 查询当前用户与专家之间进行中的会话，避免重复创建。 */
    ConsultSession selectActiveByUserAndExpert(@Param("userId") Long userId, @Param("expertId") Long expertId);

    /** 查询当前用户与专家之间最新的非删除会话，用于统一不同入口进入同一专家时的会话定位。 */
    ConsultSession selectLatestByUserAndExpert(@Param("userId") Long userId, @Param("expertId") Long expertId);

    /** 查询第一位可咨询专家，用于智能策略页未指定专家时的兜底发送。 */
    ConsultSession selectDefaultExpert();

    /** 查询当前用户与专家的聊天会话列表，供 farm 消息页展示。 */
    List<ConsultSession> selectClientSessionList(@Param("userId") Long userId);

    /** 专家工作台仅查询 expert_profile.user_id 对应的咨询会话。 */
    List<ConsultSession> selectExpertSessionList(@Param("expertUserId") Long expertUserId);

    /** 专家打开会话后清空自己的未读计数。 */
    int resetExpertUnreadCount(@Param("id") Long id, @Param("expertUserId") Long expertUserId);

    /** 更新会话最后一条消息和未读数量。 */
    int updateLastMessage(@Param("id") Long id,
                          @Param("lastMessageContent") String lastMessageContent,
                          @Param("receiverIsExpert") boolean receiverIsExpert);

    /** 当前用户进入聊天页后清空自己的未读计数。 */
    int resetUserUnreadCount(@Param("id") Long id, @Param("userId") Long userId);

    /** 递增专家咨询次数，便于专家管理页展示服务量。 */
    int increaseExpertConsultationCount(Long expertId);
}
