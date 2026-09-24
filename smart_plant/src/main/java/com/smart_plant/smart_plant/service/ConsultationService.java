package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ClientExpertChatDetailResponse;
import com.smart_plant.smart_plant.dto.ClientExpertChatRequest;
import com.smart_plant.smart_plant.dto.ClientExpertChatResponse;
import com.smart_plant.smart_plant.dto.ClientExpertChatSessionResponse;
import com.smart_plant.smart_plant.dto.ExpertChatDetailResponse;
import com.smart_plant.smart_plant.entity.ConsultSession;
import com.smart_plant.smart_plant.entity.ExpertProfile;
import com.smart_plant.smart_plant.entity.ConsultMessage;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 专家咨询业务服务。
 */
public interface ConsultationService {

    /** farm 用户端向专家发送消息，并保存到 consult_message 表。 */
    ClientExpertChatResponse sendClientMessage(ClientExpertChatRequest request);

    /** farm 用户端查询当前用户与专家的聊天会话列表。 */
    List<ClientExpertChatSessionResponse> listClientSessions();

    /** farm 用户端查询某位专家的聊天详情和历史消息。 */
    ClientExpertChatDetailResponse getClientChatDetail(Long sessionId, Long expertId);

    /** 专家身份认证信息与本人收到的咨询，均从登录态关联 expert_profile.user_id。 */
    ExpertProfile getCurrentExpertProfile();

    List<ConsultSession> listExpertSessions();

    ExpertChatDetailResponse getExpertChatDetail(Long sessionId, Long beforeId, Long afterId, Integer pageSize);

    /** 回复本人会话并保存消息、更新未读数和推送给咨询用户。 */
    ClientExpertChatResponse replyAsExpert(Long sessionId, String content);

    /** 专家发送图片或文件时复用本人会话的权限校验和消息存储。 */
    ClientExpertChatResponse replyAsExpert(Long sessionId, String content, Integer messageType, String mediaUrl);

    /** 删除单条聊天内容。 */
    void deleteMessage(Long id);

    /** 批量删除聊天内容。 */
    int deleteMessages(List<Long> ids);

    /** 根据ID查询聊天内容详情。 */
    ConsultMessage getMessageById(Long id);

    /** 后台分页查询聊天内容列表。 */
    PageInfo<ConsultMessage> listMessages(String username,
                                          String expertName,
                                          String sessionNo,
                                          Integer messageType,
                                          LocalDate startDate,
                                          LocalDate endDate,
                                          Integer pageNum,
                                          Integer pageSize);

    /** 后台查询聊天内容统计数据。 */
    Map<String, Object> statisticsMessages(String username,
                                           String expertName,
                                           String sessionNo,
                                           Integer messageType,
                                           LocalDate startDate,
                                           LocalDate endDate);
}
