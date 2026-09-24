package com.smart_plant.smart_plant.security;

import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.response.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** 专家登录后只可使用个人账号、本人咨询和本人系统公告接口，防止访问农场主业务接口。 */
@Component
public class ExpertClientAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        User user = CurrentUserContext.get();
        if (user == null || !"expert".equalsIgnoreCase(user.getRoleCode())) {
            return true;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        // 公告接口按登录用户查询和更新接收记录，允许专家阅读本人收到的公告。
        if (path.startsWith("/client/auth/") || path.startsWith("/api/client/auth/")
                || path.startsWith("/client/expert-workspace/")
                || path.startsWith("/api/client/expert-workspace/")
                // 私有聊天附件由下载服务逐条核验会话参与人，专家可进入该专用入口。
                || path.startsWith("/uploads/consult-chat/")
                || path.startsWith("/client/system-announcements/")
                || path.startsWith("/api/client/system-announcements/")) {
            return true;
        }
        throw new BusinessException(ResponseCode.FORBIDDEN, "专家账号不能访问该功能");
    }
}
