package com.smart_plant.smart_plant.config;

import com.smart_plant.smart_plant.websocket.ConsultWebSocketHandler;
import com.smart_plant.smart_plant.websocket.ConsultWebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置。
 *
 * <p>注册专家咨询与成员聊天实时推送通道，地址分别为 /ws/consult 和 /ws/chat。</p>
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ConsultWebSocketHandler consultWebSocketHandler;
    private final ConsultWebSocketHandshakeInterceptor consultWebSocketHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 两个地址复用同一鉴权连接池；保留旧地址以兼容已上线的专家聊天页面。
        registry.addHandler(consultWebSocketHandler, "/ws/consult", "/ws/chat")
                .addInterceptors(consultWebSocketHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
