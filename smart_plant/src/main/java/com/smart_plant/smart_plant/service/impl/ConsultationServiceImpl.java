package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ClientExpertChatDetailResponse;
import com.smart_plant.smart_plant.dto.ClientExpertChatMessageResponse;
import com.smart_plant.smart_plant.dto.ClientExpertChatRequest;
import com.smart_plant.smart_plant.dto.ClientExpertChatResponse;
import com.smart_plant.smart_plant.dto.ClientExpertChatSessionResponse;
import com.smart_plant.smart_plant.dto.ExpertChatDetailResponse;
import com.smart_plant.smart_plant.dto.ConsultWebSocketMessage;
import com.smart_plant.smart_plant.entity.ConsultMessage;
import com.smart_plant.smart_plant.entity.ConsultSession;
import com.smart_plant.smart_plant.entity.ExpertProfile;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.ConsultMessageMapper;
import com.smart_plant.smart_plant.mapper.ConsultSessionMapper;
import com.smart_plant.smart_plant.mapper.ExpertProfileMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.ConsultationService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.websocket.ConsultWebSocketPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 专家咨询业务实现。
 *
 * <p>咨询消息属于强业务数据，必须落库；farm 本地只适合做输入草稿或弱网临时缓存。</p>
 */
@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private static final int MESSAGE_TYPE_TEXT = 1;
    private static final int MESSAGE_TYPE_IMAGE = 2;
    private static final int MESSAGE_TYPE_VOICE = 3;
    private static final int MESSAGE_TYPE_FILE = 4;
    private static final int SESSION_STATUS_ACTIVE = 1;
    private static final int MESSAGE_STATUS_NORMAL = 1;
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int MAX_MEDIA_URL_LENGTH = 500;
    private static final int MAX_SUMMARY_LENGTH = 255;
    private static final DateTimeFormatter SESSION_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final ConsultSessionMapper consultSessionMapper;
    private final ConsultMessageMapper consultMessageMapper;
    private final ExpertProfileMapper expertProfileMapper;
    private final DataPermissionService dataPermissionService;
    private final ConsultWebSocketPushService consultWebSocketPushService;
    private final ConsultAttachmentService attachmentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientExpertChatResponse sendClientMessage(ClientExpertChatRequest request) {
        User currentUser = dataPermissionService.currentUser();
        // 专家只能在本人会话中回复，不能使用咨询发起人接口创建用户侧会话。
        if ("expert".equalsIgnoreCase(currentUser.getRoleCode())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "专家请使用回复接口");
        }
        Integer messageType = normalizeMessageType(request == null ? null : request.getMessageType());
        String content = normalizeOptionalText(request == null ? null : request.getContent());
        String mediaUrl = normalizeOptionalText(request == null ? null : request.getMediaUrl());
        validateMessageBody(messageType, content, mediaUrl);
        if (messageType == MESSAGE_TYPE_IMAGE || messageType == MESSAGE_TYPE_FILE) {
            // 数据库只接受当前登录用户已上传到聊天目录的附件地址。
            attachmentService.requireOwned(mediaUrl, messageType);
        }

        ExpertProfile expert = resolveAvailableExpert(request == null ? null : request.getExpertId());
        if (currentUser.getId().equals(expert.getUserId())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "不能向自己发起咨询");
        }

        ConsultSession session = resolveSession(request == null ? null : request.getSessionId(), currentUser.getId(), expert, content, mediaUrl);
        ConsultMessage message = buildMessage(session, currentUser.getId(), expert.getUserId(), messageType, content, mediaUrl);
        consultMessageMapper.insert(message);
        consultSessionMapper.updateLastMessage(session.getId(), buildMessageSummary(content, mediaUrl), true);
        pushSavedClientMessage(session, message, expert, "user");

        return new ClientExpertChatResponse(
                session.getId(),
                message.getId(),
                expert.getId(),
                expert.getRealName(),
                content,
                mediaUrl,
                message.getCreateTime());
    }

    @Override
    public List<ClientExpertChatSessionResponse> listClientSessions() {
        User currentUser = dataPermissionService.currentUser();
        return consultSessionMapper.selectClientSessionList(currentUser.getId())
                .stream()
                .map(this::toClientSessionResponse)
                .toList();
    }

    @Override
    public ExpertProfile getCurrentExpertProfile() {
        User currentUser = dataPermissionService.currentUser();
        if (!"expert".equalsIgnoreCase(currentUser.getRoleCode())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "仅专家可访问该功能");
        }
        ExpertProfile profile = expertProfileMapper.selectByUserId(currentUser.getId());
        if (profile == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "专家认证档案不存在");
        }
        return profile;
    }

    @Override
    public List<ConsultSession> listExpertSessions() {
        // 身份与会话归属均由服务端确定，不接受客户端传入 expertId。
        ExpertProfile expert = getCurrentExpertProfile();
        return Integer.valueOf(1).equals(expert.getStatus())
                ? consultSessionMapper.selectExpertSessionList(expert.getUserId()) : List.of();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpertChatDetailResponse getExpertChatDetail(Long sessionId, Long beforeId, Long afterId,
                                                        Integer pageSize) {
        validateMessageCursor(beforeId, afterId);
        int limit = pageSize == null ? 30 : Math.max(1, Math.min(pageSize, 50));
        ExpertProfile expert = getCurrentExpertProfile();
        ConsultSession session = requireExpertSession(sessionId, expert);
        List<ConsultMessage> rows = consultMessageMapper
                .selectExpertMessagesBySessionId(sessionId, expert.getUserId(), beforeId, afterId, limit + 1);
        boolean hasMore = rows.size() > limit;
        if (hasMore) rows = rows.subList(0, limit);
        if (afterId == null) rows = new java.util.ArrayList<>(rows.reversed());
        List<ClientExpertChatMessageResponse> messages = rows.stream()
                .map(message -> toClientMessageResponse(message, expert.getUserId()))
                .toList();
        // 先取历史再标记已读，确保详情仍返回刚收到的消息。
        consultMessageMapper.markExpertMessagesRead(sessionId, expert.getUserId());
        consultSessionMapper.resetExpertUnreadCount(sessionId, expert.getUserId());
        String userName = StringUtils.hasText(session.getNickname())
                ? session.getNickname() : session.getUsername();
        return new ExpertChatDetailResponse(sessionId, userName, session.getLastMessageTime(), messages,
                afterId == null && hasMore, afterId != null && hasMore);
    }

    /** 游标必须为正数且不能同时向前、向后查询。 */
    private void validateMessageCursor(Long beforeId, Long afterId) {
        if (beforeId != null && afterId != null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "历史游标和增量游标不能同时使用");
        }
        if ((beforeId != null && beforeId <= 0) || (afterId != null && afterId <= 0)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "消息游标不合法");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientExpertChatResponse replyAsExpert(Long sessionId, String rawContent) {
        return replyAsExpert(sessionId, rawContent, MESSAGE_TYPE_TEXT, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientExpertChatResponse replyAsExpert(Long sessionId, String rawContent,
                                                  Integer rawMessageType, String rawMediaUrl) {
        ExpertProfile expert = getCurrentExpertProfile();
        ConsultSession session = requireExpertSession(sessionId, expert);
        Integer messageType = normalizeMessageType(rawMessageType);
        String content = normalizeOptionalText(rawContent);
        String mediaUrl = normalizeOptionalText(rawMediaUrl);
        validateMessageBody(messageType, content, mediaUrl);
        if (messageType == MESSAGE_TYPE_IMAGE || messageType == MESSAGE_TYPE_FILE) {
            attachmentService.requireOwned(mediaUrl, messageType);
        }

        // 图片与文件也沿用咨询消息表、会话摘要和实时推送，接收者始终是会话发起人。
        ConsultMessage message = buildMessage(session, expert.getUserId(), session.getUserId(),
                messageType, content, mediaUrl);
        consultMessageMapper.insert(message);
        consultSessionMapper.updateLastMessage(sessionId, buildMessageSummary(content, mediaUrl), false);
        pushSavedClientMessage(session, message, expert, "expert");
        return new ClientExpertChatResponse(sessionId, message.getId(), expert.getId(),
                expert.getRealName(), content, mediaUrl, message.getCreateTime());
    }

    /** 会话必须属于当前专家档案；此校验覆盖读取、已读和回复三种操作。 */
    private ConsultSession requireExpertSession(Long sessionId, ExpertProfile expert) {
        if (!Integer.valueOf(1).equals(expert.getStatus())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "专家档案尚未启用");
        }
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择咨询会话");
        }
        ConsultSession session = consultSessionMapper.selectById(sessionId);
        if (session == null || Integer.valueOf(0).equals(session.getStatus())) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "咨询会话不存在");
        }
        if (!expert.getId().equals(session.getExpertId())
                || !expert.getUserId().equals(session.getExpertUserId())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "无权访问该咨询会话");
        }
        return session;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientExpertChatDetailResponse getClientChatDetail(Long sessionId, Long expertId) {
        User currentUser = dataPermissionService.currentUser();
        ConsultSession session = resolveClientSessionForDetail(sessionId, expertId, currentUser.getId());

        if (session == null) {
            ExpertProfile expert = resolveAvailableExpert(expertId);
            return new ClientExpertChatDetailResponse(
                    null,
                    expert.getId(),
                    expert.getAvatar(),
                    expert.getRealName(),
                    null,
                    List.of());
        }

        List<ClientExpertChatMessageResponse> messages = consultMessageMapper
                .selectClientMessagesBySessionId(session.getId(), currentUser.getId())
                .stream()
                .map(message -> toClientMessageResponse(message, currentUser.getId()))
                .toList();

        // 读取聊天详情视为用户已查看专家回复，避免消息页继续显示已读消息的未读数。
        consultMessageMapper.markClientMessagesRead(session.getId(), currentUser.getId());
        consultSessionMapper.resetUserUnreadCount(session.getId(), currentUser.getId());

        return new ClientExpertChatDetailResponse(
                session.getId(),
                session.getExpertId(),
                session.getExpertAvatar(),
                session.getExpertName(),
                session.getLastMessageTime(),
                messages);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long id) {
        ConsultMessage message = getMessageById(id);
        int rows = consultMessageMapper.deleteById(message.getId());
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "咨询消息删除失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteMessages(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择需要删除的咨询消息");
        }
        for (Long id : ids) {
            getMessageById(id);
        }
        return consultMessageMapper.deleteBatchByIds(ids);
    }

    @Override
    public ConsultMessage getMessageById(Long id) {
        requireId(id);
        ConsultMessage message = consultMessageMapper.selectById(id);
        if (message == null || Integer.valueOf(0).equals(message.getStatus())) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "咨询消息不存在");
        }
        requireMessageAccess(message);
        return message;
    }

    @Override
    public PageInfo<ConsultMessage> listMessages(String username, String expertName, String sessionNo,
                                                 Integer messageType, LocalDate startDate, LocalDate endDate,
                                                 Integer pageNum, Integer pageSize) {
        QueryParams params = normalizeQueryParams(username, expertName, sessionNo, messageType, startDate, endDate);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(consultMessageMapper.selectList(
                scopedUserId,
                params.username(),
                params.expertName(),
                params.sessionNo(),
                params.messageType(),
                params.startTime(),
                params.endTime()));
    }

    @Override
    public Map<String, Object> statisticsMessages(String username, String expertName, String sessionNo,
                                                  Integer messageType, LocalDate startDate, LocalDate endDate) {
        QueryParams params = normalizeQueryParams(username, expertName, sessionNo, messageType, startDate, endDate);
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        Map<String, Object> statistics = consultMessageMapper.selectStatistics(
                scopedUserId,
                params.username(),
                params.expertName(),
                params.sessionNo(),
                params.messageType(),
                params.startTime(),
                params.endTime());
        return statistics == null ? new HashMap<>() : statistics;
    }

    /** 创建或复用当前用户与专家之间的进行中会话。 */
    private ConsultSession resolveSession(Long sessionId, Long userId, ExpertProfile expert, String content, String mediaUrl) {
        if (sessionId != null) {
            ConsultSession session = consultSessionMapper.selectById(sessionId);
            if (session == null || Integer.valueOf(0).equals(session.getStatus())) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "咨询会话不存在");
            }
            if (!userId.equals(session.getUserId())) {
                throw new BusinessException(ResponseCode.FORBIDDEN, "无权访问该咨询会话");
            }
            if (!expert.getId().equals(session.getExpertId())) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "专家与咨询会话不匹配");
            }
            return session;
        }

        // 同一用户与同一专家只复用最新一条非删除会话，保证专家列表入口和消息入口看到同一批消息。
        ConsultSession oldSession = consultSessionMapper.selectLatestByUserAndExpert(userId, expert.getId());
        if (oldSession != null) {
            return oldSession;
        }

        ConsultSession session = new ConsultSession();
        session.setUserId(userId);
        session.setExpertId(expert.getId());
        session.setSessionNo(buildSessionNo(userId, expert.getId()));
        session.setSessionTitle(buildSessionTitle(content, mediaUrl));
        session.setStatus(SESSION_STATUS_ACTIVE);
        session.setUserUnreadCount(0);
        session.setExpertUnreadCount(0);
        consultSessionMapper.insert(session);
        consultSessionMapper.increaseExpertConsultationCount(expert.getId());
        return session;
    }

    /** 根据 sessionId 或 expertId 解析当前用户聊天详情对应的会话。 */
    private ConsultSession resolveClientSessionForDetail(Long sessionId, Long expertId, Long userId) {
        if (sessionId != null) {
            ConsultSession session = consultSessionMapper.selectById(sessionId);
            if (session == null || Integer.valueOf(0).equals(session.getStatus())) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "咨询会话不存在");
            }
            if (!userId.equals(session.getUserId())) {
                throw new BusinessException(ResponseCode.FORBIDDEN, "无权访问该咨询会话");
            }
            if (expertId != null && !expertId.equals(session.getExpertId())) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "专家与咨询会话不匹配");
            }
            return session;
        }

        if (expertId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择需要咨询的专家");
        }
        resolveAvailableExpert(expertId);
        return consultSessionMapper.selectLatestByUserAndExpert(userId, expertId);
    }

    /** 根据指定专家或默认专家获取可咨询专家，并做状态校验。 */
    private ExpertProfile resolveAvailableExpert(Long expertId) {
        Long resolvedExpertId = expertId;
        if (resolvedExpertId == null) {
            ConsultSession defaultExpert = consultSessionMapper.selectDefaultExpert();
            resolvedExpertId = defaultExpert == null ? null : defaultExpert.getExpertId();
        }
        if (resolvedExpertId == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "暂无可咨询专家");
        }

        ExpertProfile expert = expertProfileMapper.selectById(resolvedExpertId);
        if (expert == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "专家不存在");
        }
        if (!Integer.valueOf(1).equals(expert.getServiceStatus())
                || !Integer.valueOf(1).equals(expert.getConsultationStatus())
                || !Integer.valueOf(1).equals(expert.getStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家当前不可咨询");
        }
        return expert;
    }

    private ConsultMessage buildMessage(ConsultSession session, Long senderId, Long receiverId,
                                        Integer messageType, String content, String mediaUrl) {
        ConsultMessage message = new ConsultMessage();
        message.setSessionId(session.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setMessageType(messageType);
        message.setContent(content);
        message.setMediaUrl(mediaUrl);
        message.setIsRead(0);
        message.setStatus(MESSAGE_STATUS_NORMAL);
        message.setCreateTime(LocalDateTime.now());
        return message;
    }

    /** HTTP 消息发送成功后推送给发送方和接收方，推送失败不影响已落库结果。 */
    private void pushSavedClientMessage(ConsultSession session, ConsultMessage message,
                                        ExpertProfile expert, String senderRole) {
        ConsultWebSocketMessage pushMessage = new ConsultWebSocketMessage(
                ConsultWebSocketPushService.TYPE_CONSULT_MESSAGE,
                session.getId(),
                message.getId(),
                expert.getId(),
                expert.getRealName(),
                message.getSenderId(),
                message.getReceiverId(),
                senderRole,
                message.getMessageType(),
                message.getContent(),
                message.getMediaUrl(),
                message.getCreateTime());
        consultWebSocketPushService.pushToUser(message.getSenderId(), pushMessage);
        consultWebSocketPushService.pushToUser(message.getReceiverId(), pushMessage);
    }

    /** 将会话聚合实体转换成 farm 消息页需要的列表字段。 */
    private ClientExpertChatSessionResponse toClientSessionResponse(ConsultSession session) {
        return new ClientExpertChatSessionResponse(
                session.getId(),
                session.getExpertId(),
                session.getExpertAvatar(),
                session.getExpertName(),
                session.getLastMessageContent(),
                session.getLastMessageTime(),
                session.getUserUnreadCount() == null ? 0 : session.getUserUnreadCount());
    }

    /** 将消息实体转换成聊天页直接可渲染的用户端消息对象。 */
    private ClientExpertChatMessageResponse toClientMessageResponse(ConsultMessage message, Long currentUserId) {
        String role = currentUserId.equals(message.getSenderId()) ? "user" : "expert";
        return new ClientExpertChatMessageResponse(
                message.getId(),
                role,
                message.getMessageType(),
                message.getContent(),
                message.getMediaUrl(),
                message.getCreateTime());
    }

    private QueryParams normalizeQueryParams(String username, String expertName, String sessionNo,
                                             Integer messageType, LocalDate startDate, LocalDate endDate) {
        if (messageType != null) {
            validateMessageType(messageType);
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }
        return new QueryParams(
                normalizeOptionalText(username),
                normalizeOptionalText(expertName),
                normalizeOptionalText(sessionNo),
                messageType,
                startDate == null ? null : startDate.atStartOfDay(),
                endDate == null ? null : endDate.atTime(LocalTime.MAX));
    }

    private Integer normalizeMessageType(Integer messageType) {
        Integer normalized = messageType == null ? MESSAGE_TYPE_TEXT : messageType;
        validateMessageType(normalized);
        return normalized;
    }

    private void validateMessageType(Integer messageType) {
        if (messageType != MESSAGE_TYPE_TEXT
                && messageType != MESSAGE_TYPE_IMAGE
                && messageType != MESSAGE_TYPE_VOICE
                && messageType != MESSAGE_TYPE_FILE) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "消息类型不合法");
        }
    }

    private void validateMessageBody(Integer messageType, String content, String mediaUrl) {
        validateLength(content, MAX_CONTENT_LENGTH, "消息内容不能超过2000个字符");
        validateLength(mediaUrl, MAX_MEDIA_URL_LENGTH, "媒体地址不能超过500个字符");
        if (messageType == MESSAGE_TYPE_TEXT && !StringUtils.hasText(content)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "消息内容不能为空");
        }
        if (messageType != MESSAGE_TYPE_TEXT && !StringUtils.hasText(mediaUrl)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "媒体文件地址不能为空");
        }
    }

    /** 后台非管理员只能访问自己参与的消息，管理员不受该限制。 */
    private void requireMessageAccess(ConsultMessage message) {
        if (dataPermissionService.isAdmin()) {
            return;
        }
        Long currentUserId = dataPermissionService.currentUser().getId();
        if (!currentUserId.equals(message.getSenderId()) && !currentUserId.equals(message.getReceiverId())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "无权访问该咨询消息");
        }
    }

    private String buildSessionNo(Long userId, Long expertId) {
        return "CS" + LocalDateTime.now().format(SESSION_NO_TIME_FORMATTER) + userId + expertId;
    }

    private String buildSessionTitle(String content, String mediaUrl) {
        return buildMessageSummary(content, mediaUrl);
    }

    private String buildMessageSummary(String content, String mediaUrl) {
        String summary = StringUtils.hasText(content) ? content.trim() : "附件消息：" + mediaUrl;
        return summary.length() <= MAX_SUMMARY_LENGTH ? summary : summary.substring(0, MAX_SUMMARY_LENGTH);
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "咨询消息ID不能为空");
        }
    }

    private record QueryParams(String username,
                               String expertName,
                               String sessionNo,
                               Integer messageType,
                               LocalDateTime startTime,
                               LocalDateTime endTime) {
    }
}
