package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ClientAiChatRequest;
import com.smart_plant.smart_plant.dto.ClientAiChatResponse;
import com.smart_plant.smart_plant.entity.AiChat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * AI对话业务接口。
 *
 * <p>该接口同时服务后台对话管理和 farm 用户端 AI 咨询。</p>
 */
public interface AiChatService {

    /** 删除单条AI对话记录。 */
    void deleteChat(Long id);

    /** 批量删除AI对话记录。 */
    int deleteChats(List<Long> ids);

    /** 根据ID查询AI对话详情。 */
    AiChat getChatById(Long id);

    /** 分页查询AI对话记录列表。 */
    PageInfo<AiChat> listChats(String username, String modelName, Integer status,
                               LocalDate startDate, LocalDate endDate,
                               Integer pageNum, Integer pageSize);

    /** 统计AI对话数量、成功数量、失败数量和图片咨询数量。 */
    Map<String, Object> statisticsChats(String username, String modelName, Integer status,
                                        LocalDate startDate, LocalDate endDate);

    /** farm 用户端发送AI咨询消息，并保存用户输入和AI回复。 */
    ClientAiChatResponse sendClientMessage(ClientAiChatRequest request);

    /** 当前用户按游标获取自己的历史 AI 咨询，结果按聊天时间正序返回。 */
    List<ClientAiChatResponse> listClientHistory(Long beforeId, Integer pageSize);
}
