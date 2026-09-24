package com.smart_plant.smart_plant.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * farm 聊天 WebSocket 处理器。
 *
 * <p>当前阶段不通过 WebSocket 写消息，只维护在线连接并响应轻量心跳，消息写入仍走 HTTP 接口。</p>
 */
@Component
@RequiredArgsConstructor
public class ConsultWebSocketHandler extends TextWebSocketHandler {

    /** 握手阶段写入 attributes 的当前登录用户ID。 */
    public static final String ATTR_USER_ID = "userId";

    private final ConsultWebSocketSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        sessionManager.addSession(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 第一阶段只支持 ping/pong 保活，业务消息发送仍使用 HTTP 接口，降低上线风险。
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        sessionManager.removeSession(userId, session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        sessionManager.removeSession(userId, session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
}
