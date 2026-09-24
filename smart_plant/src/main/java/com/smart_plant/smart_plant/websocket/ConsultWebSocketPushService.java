package com.smart_plant.smart_plant.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_plant.smart_plant.dto.ConsultWebSocketMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 专家咨询 WebSocket 推送服务。
 *
 * <p>业务服务只负责传入已落库消息，序列化和在线用户投递统一收敛在这里，便于后续替换为 MQ 广播。</p>
 */
@Service
@RequiredArgsConstructor
public class ConsultWebSocketPushService {

    /** 咨询消息事件类型。 */
    public static final String TYPE_CONSULT_MESSAGE = "CONSULT_MESSAGE";

    private final ObjectMapper objectMapper;
    private final ConsultWebSocketSessionManager sessionManager;

    /** 向指定用户推送咨询消息；用户不在线时直接跳过，数据库仍是最终消息源。 */
    public void pushToUser(Long userId, ConsultWebSocketMessage message) {
        pushToUser(userId, (Object) message);
    }

    /** 通用聊天事件也复用同一用户连接池，避免为每类消息重复建立 WebSocket。 */
    public void pushToUser(Long userId, Object message) {
        if (userId == null || message == null) {
            return;
        }
        try {
            sessionManager.sendToUser(userId, objectMapper.writeValueAsString(message));
        } catch (Exception exception) {
            // 推送失败不能影响 HTTP 发送接口提交结果，因此这里吞掉异常，历史消息仍可通过 HTTP 查询。
        }
    }
}
