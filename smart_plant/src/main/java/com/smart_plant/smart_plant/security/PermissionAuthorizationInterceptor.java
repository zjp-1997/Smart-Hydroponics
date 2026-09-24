package com.smart_plant.smart_plant.security;

import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.PermissionAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class PermissionAuthorizationInterceptor implements HandlerInterceptor {

    private final PermissionAuthorizationService permissionAuthorizationService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePermission permission = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequirePermission.class);
        if (permission == null) {
            permission = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequirePermission.class);
        }
        if (permission == null) {
            return true;
        }

        User user = CurrentUserContext.get();
        if (user == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "Not logged in");
        }
        if (!permissionAuthorizationService.hasAnyPermission(user, permission.value())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "No permission to access this function");
        }
        return true;
    }
}
