package com.smart_plant.smart_plant.websocket;

import com.smart_plant.smart_plant.dto.JwtPayload;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.service.JwtService;
import com.smart_plant.smart_plant.service.LoginSessionService;
import com.smart_plant.smart_plant.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 专家咨询 WebSocket 握手鉴权拦截器。
 *
 * <p>uni-app WebSocket 对自定义 Header 兼容性不完全一致，因此这里支持通过 query token 完成 JWT 鉴权。</p>
 */
@Component
@RequiredArgsConstructor
public class ConsultWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final LoginSessionService loginSessionService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            String token = resolveToken(request);
            JwtPayload payload = jwtService.parseToken(token);
            if (tokenBlacklistService.isBlacklisted(payload.getJti())
                    || !"access".equals(payload.getTokenType())
                    || !loginSessionService.isAccessTokenActive(payload.getAdminId(), payload.getJti())) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            var user = userMapper.selectById(payload.getAdminId());
            if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put(ConsultWebSocketHandler.ATTR_USER_ID, payload.getAdminId());
            return true;
        } catch (BusinessException exception) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后无需额外处理，连接生命周期由 ConsultWebSocketHandler 维护。
    }

    private String resolveToken(ServerHttpRequest request) {
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        String token = params.getFirst("token");
        return token == null ? "" : token;
    }
}
