package com.smart_plant.smart_plant.security;

import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 专家令牌可读本人公告，但不能访问普通咨询发送、农场主业务或后台接口。 */
class ExpertClientAccessInterceptorTest {
    @Test
    void expertCanUseOwnWorkspaceButNotOtherBusinessApis() {
        User expert = new User();
        expert.setRoleCode("expert");
        CurrentUserContext.set(expert);
        try {
            ExpertClientAccessInterceptor interceptor = new ExpertClientAccessInterceptor();
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            HandlerMethod handler = mock(HandlerMethod.class);
            when(request.getContextPath()).thenReturn("");
            when(request.getMethod()).thenReturn("GET");

            // 本人工作台、个人资料和按接收人隔离的公告可访问。
            when(request.getRequestURI()).thenReturn("/client/expert-workspace/sessions");
            assertTrue(interceptor.preHandle(request, response, handler));
            when(request.getRequestURI()).thenReturn("/client/expert-workspace/attachments");
            assertTrue(interceptor.preHandle(request, response, handler));
            when(request.getRequestURI()).thenReturn("/client/auth/me");
            assertTrue(interceptor.preHandle(request, response, handler));
            when(request.getRequestURI()).thenReturn("/client/system-announcements/statistics");
            assertTrue(interceptor.preHandle(request, response, handler));
            when(request.getRequestURI()).thenReturn("/client/system-announcements/1/read");
            assertTrue(interceptor.preHandle(request, response, handler));
            // 咨询发起人接口仍必须被拒绝。
            when(request.getRequestURI()).thenReturn("/client/expert-chat/sessions");
            assertThrows(BusinessException.class, () -> interceptor.preHandle(request, response, handler));
            when(request.getRequestURI()).thenReturn("/client/expert-chat/attachments");
            assertThrows(BusinessException.class, () -> interceptor.preHandle(request, response, handler));
        } finally {
            CurrentUserContext.clear();
        }
    }
}
