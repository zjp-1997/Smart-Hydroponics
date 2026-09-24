package com.smart_plant.smart_plant.security;

import com.smart_plant.smart_plant.dto.JwtPayload;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.RoleMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.JwtService;
import com.smart_plant.smart_plant.service.LoginSessionService;
import com.smart_plant.smart_plant.service.TokenBlacklistService;
import com.smart_plant.smart_plant.utils.ClientRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    private final TokenBlacklistService tokenBlacklistService;

    private final LoginSessionService loginSessionService;

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = isUploadResourceGet(request)
                ? ClientRequestUtils.getUploadResourceToken(request)
                : ClientRequestUtils.getBearerToken(request);
        if (token == null || token.isBlank()) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "Missing authentication token");
        }
        JwtPayload payload = jwtService.parseToken(token);
        if (tokenBlacklistService.isBlacklisted(payload.getJti())) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "Token has been logged out");
        }
        if (!"access".equals(payload.getTokenType()) || !loginSessionService.isAccessTokenActive(payload.getAdminId(), payload.getJti())) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "Login session has expired");
        }
        User user = userMapper.selectById(payload.getAdminId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "User is disabled or not found");
        }
        var role = roleMapper.selectById(user.getRoleId());
        if (role == null || !Integer.valueOf(1).equals(role.getStatus())) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "Role is disabled or not found");
        }
        CurrentUserContext.set(user);
        return true;
    }

    private boolean isUploadResourceGet(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        return request.getRequestURI().startsWith(contextPath + "/uploads/");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
    }
}
