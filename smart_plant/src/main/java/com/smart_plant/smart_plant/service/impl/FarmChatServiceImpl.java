package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.FarmChatDetailResponse;
import com.smart_plant.smart_plant.dto.FarmChatMessageResponse;
import com.smart_plant.smart_plant.dto.FarmChatSendRequest;
import com.smart_plant.smart_plant.dto.FarmChatSendResponse;
import com.smart_plant.smart_plant.dto.FarmChatSessionResponse;
import com.smart_plant.smart_plant.dto.FarmChatWebSocketMessage;
import com.smart_plant.smart_plant.entity.FarmChatMessage;
import com.smart_plant.smart_plant.entity.FarmChatSession;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.FarmChatMessageMapper;
import com.smart_plant.smart_plant.mapper.FarmChatSessionMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.FarmChatService;
import com.smart_plant.smart_plant.websocket.ConsultWebSocketPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 农场成员聊天实现。
 *
 * <p>所有参与人均从登录态和绑定表推导，客户端无法伪造发送人或跨农场创建会话。</p>
 */
@Service
@RequiredArgsConstructor
public class FarmChatServiceImpl implements FarmChatService {
    private static final Set<String> CHAT_ROLES = Set.of("farm_owner", "user", "technician");
    private static final int TEXT = 1;
    private static final int IMAGE = 2;
    private static final int FILE = 4;
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int MAX_MEDIA_URL_LENGTH = 500;
    private static final int MAX_SUMMARY_LENGTH = 255;

    private final FarmChatSessionMapper sessionMapper;
    private final FarmChatMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final DataPermissionService dataPermissionService;
    private final ConsultAttachmentService attachmentService;
    private final ConsultWebSocketPushService webSocketPushService;

    @Override
    public void requireCurrentChatRole() {
        currentChatUser();
    }

    @Override
    public List<FarmChatSessionResponse> listSessions() {
        User current = currentChatUser();
        String role = normalizeRole(current.getRoleCode());
        if ("farm_owner".equals(role)) {
            return sessionMapper.selectOwnerSessionList(current.getId());
        }
        if ("technician".equals(role)) {
            return sessionMapper.selectTechnicianSessionList(current.getId());
        }
        return sessionMapper.selectUserSessionList(current.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmChatDetailResponse getDetail(Long sessionId, Long peerUserId, Long beforeId, Long afterId,
                                            Integer pageSize) {
        if (beforeId != null && afterId != null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "历史游标和增量游标不能同时使用");
        }
        if ((beforeId != null && beforeId <= 0) || (afterId != null && afterId <= 0)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "消息游标不合法");
        }
        int limit = pageSize == null ? 30 : Math.max(1, Math.min(pageSize, 50));
        User current = currentChatUser();
        Participants participants = resolveParticipants(current, sessionId, peerUserId, false);
        FarmChatSession session = participants.session();
        List<FarmChatMessage> rows = session == null ? List.of()
                : messageMapper.selectCursor(session.getId(), beforeId, afterId, limit + 1);
        boolean hasMore = rows.size() > limit;
        if (hasMore) rows = rows.subList(0, limit);
        // 初始页和历史页由SQL倒序读取后恢复为聊天正序；增量页本身已是正序。
        if (afterId == null) rows = new java.util.ArrayList<>(rows.reversed());
        List<FarmChatMessageResponse> messages = rows.stream()
                .map(message -> toMessageResponse(message, current.getId())).toList();
        if (session != null) {
            // 读取历史后再更新已读，保证本次响应仍包含刚收到的消息。
            messageMapper.markReceivedMessagesRead(session.getId(), current.getId());
            sessionMapper.resetUnreadCount(session.getId(), current.getId());
        }
        User peer = participants.peer();
        return new FarmChatDetailResponse(session == null ? null : session.getId(), peer.getId(),
                displayName(peer), peer.getAvatar(), normalizeRole(peer.getRoleCode()),
                session == null ? null : session.getLastMessageTime(), messages,
                afterId == null && hasMore, afterId != null && hasMore);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmChatSendResponse send(FarmChatSendRequest request) {
        User current = currentChatUser();
        Integer messageType = normalizeMessageType(request == null ? null : request.getMessageType());
        String content = normalizeText(request == null ? null : request.getContent());
        String mediaUrl = normalizeText(request == null ? null : request.getMediaUrl());
        validateMessage(messageType, content, mediaUrl);
        if (messageType == IMAGE || messageType == FILE) {
            attachmentService.requireOwned(mediaUrl, messageType);
        }

        // 发送消息始终要求当前仍存在有效绑定；历史会话在解绑后只能读取。
        Participants participants = resolveParticipants(current,
                request == null ? null : request.getSessionId(),
                request == null ? null : request.getPeerUserId(), true);
        FarmChatSession session = participants.session();
        if (session == null) {
            session = createSession(participants.ownerId(), participants.memberId(), participants.memberRole());
        }

        User peer = participants.peer();
        LocalDateTime createTime = LocalDateTime.now();
        FarmChatMessage message = new FarmChatMessage();
        message.setSessionId(session.getId());
        message.setSenderId(current.getId());
        message.setReceiverId(peer.getId());
        message.setMessageType(messageType);
        message.setContent(content);
        message.setMediaUrl(mediaUrl);
        message.setIsRead(0);
        message.setStatus(1);
        message.setCreateTime(createTime);
        messageMapper.insert(message);

        boolean senderIsOwner = current.getId().equals(session.getFarmOwnerId());
        sessionMapper.updateLastMessage(session.getId(), summary(content, mediaUrl, messageType), senderIsOwner);
        pushMessage(session, message, current, peer);
        return new FarmChatSendResponse(session.getId(), message.getId(), peer.getId(), content, mediaUrl, createTime);
    }

    /** 唯一索引兜住双端并发首次发送；冲突时直接复用另一请求已经创建的会话。 */
    private FarmChatSession createSession(Long ownerId, Long memberId, String memberRole) {
        FarmChatSession session = new FarmChatSession();
        session.setSessionNo("FC" + UUID.randomUUID().toString().replace("-", ""));
        session.setFarmOwnerId(ownerId);
        session.setMemberUserId(memberId);
        session.setMemberRole(memberRole);
        session.setOwnerUnreadCount(0);
        session.setMemberUnreadCount(0);
        session.setStatus(1);
        try {
            sessionMapper.insert(session);
            return session;
        } catch (DuplicateKeyException ignored) {
            FarmChatSession existing = sessionMapper.selectByParticipants(ownerId, memberId);
            if (existing != null) {
                return existing;
            }
            throw ignored;
        }
    }

    /**
     * 根据当前角色确定农场主和成员身份。
     * requireActiveBinding=true 用于发送并强制校验有效绑定；false 时已有会话允许查看历史。
     */
    private Participants resolveParticipants(User current, Long sessionId, Long peerUserId, boolean requireActiveBinding) {
        FarmChatSession session = null;
        User peer;
        Long ownerId;
        Long memberId;
        String memberRole;

        if (sessionId != null) {
            session = sessionMapper.selectById(sessionId);
            if (session == null || (!current.getId().equals(session.getFarmOwnerId())
                    && !current.getId().equals(session.getMemberUserId()))) {
                throw new BusinessException(ResponseCode.FORBIDDEN, "无权访问该聊天会话");
            }
            Long resolvedPeerId = current.getId().equals(session.getFarmOwnerId())
                    ? session.getMemberUserId() : session.getFarmOwnerId();
            if (peerUserId != null && !peerUserId.equals(resolvedPeerId)) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "联系人与会话不匹配");
            }
            peer = requireActiveUser(resolvedPeerId);
            ownerId = session.getFarmOwnerId();
            memberId = session.getMemberUserId();
            memberRole = session.getMemberRole();
        } else {
            if (peerUserId == null) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "联系人不能为空");
            }
            peer = requireActiveUser(peerUserId);
            String currentRole = normalizeRole(current.getRoleCode());
            if ("farm_owner".equals(currentRole)) {
                ownerId = current.getId();
                memberId = peer.getId();
                memberRole = normalizeRole(peer.getRoleCode());
            } else {
                ownerId = peer.getId();
                memberId = current.getId();
                memberRole = currentRole;
            }
            requireActiveBinding(ownerId, memberId, memberRole);
            session = sessionMapper.selectByParticipants(ownerId, memberId);
        }

        if (requireActiveBinding) {
            requireActiveBinding(ownerId, memberId, memberRole);
        }
        return new Participants(ownerId, memberId, memberRole, session, peer);
    }

    /** 使用既有绑定关系作为唯一聊天授权来源。 */
    private void requireActiveBinding(Long ownerId, Long memberId, String memberRole) {
        boolean bound = "user".equals(memberRole)
                ? userMapper.countBoundUserByOwnerId(ownerId, memberId) > 0
                : "technician".equals(memberRole)
                && userMapper.countActiveTechnicianBinding(ownerId, memberId) > 0;
        if (!bound) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "当前账号之间不存在有效农场绑定关系");
        }
    }

    private User currentChatUser() {
        User current = dataPermissionService.currentUser();
        if (!CHAT_ROLES.contains(normalizeRole(current.getRoleCode()))) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "当前角色不能使用农场成员聊天");
        }
        return current;
    }

    private User requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "聊天联系人不存在或已停用");
        }
        return user;
    }

    private FarmChatMessageResponse toMessageResponse(FarmChatMessage message, Long currentUserId) {
        return new FarmChatMessageResponse(message.getId(),
                currentUserId.equals(message.getSenderId()) ? "self" : "peer",
                message.getMessageType(), message.getContent(), message.getMediaUrl(), message.getCreateTime());
    }

    /** 推送给接收方和发送方其他设备，前端按 messageId 去重。 */
    private void pushMessage(FarmChatSession session, FarmChatMessage message, User sender, User peer) {
        FarmChatWebSocketMessage receiverEvent = new FarmChatWebSocketMessage("FARM_CHAT_MESSAGE",
                session.getId(), message.getId(), sender.getId(), sender.getId(), peer.getId(), "peer",
                message.getMessageType(), message.getContent(), message.getMediaUrl(), message.getCreateTime());
        FarmChatWebSocketMessage senderEvent = new FarmChatWebSocketMessage("FARM_CHAT_MESSAGE",
                session.getId(), message.getId(), peer.getId(), sender.getId(), peer.getId(), "self",
                message.getMessageType(), message.getContent(), message.getMediaUrl(), message.getCreateTime());
        // 仅在数据库事务提交后推送，防止接收端立刻回查却读不到尚未提交的消息。
        Runnable push = () -> {
            webSocketPushService.pushToUser(peer.getId(), receiverEvent);
            webSocketPushService.pushToUser(sender.getId(), senderEvent);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push.run();
                }
            });
        } else {
            push.run();
        }
    }

    private Integer normalizeMessageType(Integer type) {
        int normalized = type == null ? TEXT : type;
        // 语音采集尚未接入，当前只接受页面实际支持的文本、图片和文件。
        if (normalized != TEXT && normalized != IMAGE && normalized != FILE) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "消息类型不合法");
        }
        return normalized;
    }

    private void validateMessage(Integer type, String content, String mediaUrl) {
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "消息内容不能超过2000个字符");
        }
        if (mediaUrl != null && mediaUrl.length() > MAX_MEDIA_URL_LENGTH) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "媒体地址不能超过500个字符");
        }
        if (type == TEXT && !StringUtils.hasText(content)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "消息内容不能为空");
        }
        if (type != TEXT && !StringUtils.hasText(mediaUrl)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "媒体文件地址不能为空");
        }
    }

    private String summary(String content, String mediaUrl, int type) {
        String value = StringUtils.hasText(content) ? content : type == IMAGE ? "[图片]" : "[文件]";
        if (!StringUtils.hasText(value)) {
            value = mediaUrl;
        }
        return value.length() <= MAX_SUMMARY_LENGTH ? value : value.substring(0, MAX_SUMMARY_LENGTH);
    }

    private String displayName(User user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toLowerCase();
    }

    /** 已解析且通过权限校验的会话双方。 */
    private record Participants(Long ownerId, Long memberId, String memberRole,
                                FarmChatSession session, User peer) {
    }
}
