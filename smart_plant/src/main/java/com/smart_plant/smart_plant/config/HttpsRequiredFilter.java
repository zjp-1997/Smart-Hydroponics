package com.smart_plant.smart_plant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.utils.ClientRequestUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class HttpsRequiredFilter extends OncePerRequestFilter {

    private final AdminAuthProperties properties;

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 认证接口会传输密码或令牌；生产环境开启 requireHttps 后，管理端和用户端认证路径都必须走 HTTPS。
        if (properties.isRequireHttps()
                && (request.getRequestURI().contains("/admin/auth/")
                || request.getRequestURI().contains("/api/auth/")
                || request.getRequestURI().contains("/client/auth/")
                || request.getRequestURI().contains("/api/client/auth/"))
                && !ClientRequestUtils.isHttps(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(R.fail(ResponseCode.FORBIDDEN, "请使用HTTPS访问")));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
