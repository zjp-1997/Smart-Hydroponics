package com.smart_plant.smart_plant.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.response.ResponseCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Actuator 使用专用 HandlerMapping，在 Spring Boot 4 中不能依赖 MVC Interceptor 完成保护。
 * 该 Filter 在 Servlet 层强制校验除健康探针以外的所有运维端点。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class OperationalEndpointSecurityFilter extends OncePerRequestFilter {

    private final JwtAuthenticationInterceptor authenticationInterceptor;

    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = applicationPath(request);
        return !path.startsWith("/actuator/") || isHealthPath(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            if (!authenticationInterceptor.preHandle(request, response, this)) {
                return;
            }
            try {
                filterChain.doFilter(request, response);
            } finally {
                authenticationInterceptor.afterCompletion(request, response, this, null);
            }
        } catch (BusinessException exception) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(
                    R.fail(ResponseCode.UNAUTHORIZED, exception.getMessage())
            ));
        }
    }

    private String applicationPath(HttpServletRequest request) {
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String requestUri = request.getRequestURI();
        return requestUri.startsWith(contextPath) ? requestUri.substring(contextPath.length()) : requestUri;
    }

    private boolean isHealthPath(String path) {
        return "/actuator/health".equals(path) || path.startsWith("/actuator/health/");
    }
}
