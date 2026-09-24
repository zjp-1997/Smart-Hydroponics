package com.smart_plant.smart_plant.websocket;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.smart_plant.smart_plant.mapper.UserMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * farm 聊天 WebSocket 在线连接管理器。
 *
 * <p>按用户ID维护多个连接，兼容同一账号在 H5、App 或多窗口同时在线的场景。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsultWebSocketSessionManager {

    private final UserMapper userMapper;

    /** 用户ID -> 该用户当前所有在线 WebSocket 连接。 */
    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    /** 注册用户连接。 */
    public void addSession(Long userId, WebSocketSession session) {
        if (userId == null || session == null) {
            return;
        }
        sessions.computeIfAbsent(userId, key -> new CopyOnWriteArraySet<>()).add(session);
    }

    /** 移除用户连接，集合为空时同步清理用户键，避免内存长期增长。 */
    public void removeSession(Long userId, WebSocketSession session) {
        if (userId == null || session == null) {
            return;
        }
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        userSessions.remove(session);
        if (userSessions.isEmpty()) {
            sessions.remove(userId);
        }
    }

    /** 向指定用户的所有在线端推送专家咨询或成员聊天消息。 */
    public void sendToUser(Long userId, String payload) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }
        // 每次推送检查当前状态，撤销后各节点的既有连接均不能继续接收数据。
        var user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            for (WebSocketSession session : userSessions) {
                try {
                    session.close(CloseStatus.POLICY_VIOLATION);
                } catch (IOException exception) {
                    log.debug("Close disabled user websocket failed, userId={}", userId, exception);
                } finally {
                    removeSession(userId, session);
                }
            }
            return;
        }
        for (WebSocketSession session : userSessions) {
            sendSafely(userId, session, payload);
        }
    }

    private void sendSafely(Long userId, WebSocketSession session, String payload) {
        if (session == null || !session.isOpen()) {
            removeSession(userId, session);
            return;
        }
        try {
            session.sendMessage(new TextMessage(payload));
        } catch (IOException exception) {
            log.warn("Push consult websocket message failed, userId={}", userId, exception);
            removeSession(userId, session);
        }
    }
}
