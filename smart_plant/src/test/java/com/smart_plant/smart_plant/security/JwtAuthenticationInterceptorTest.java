package com.smart_plant.smart_plant.security;

import com.smart_plant.smart_plant.dto.JwtPayload;
import com.smart_plant.smart_plant.entity.Role;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.RoleMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.service.JwtService;
import com.smart_plant.smart_plant.service.LoginSessionService;
import com.smart_plant.smart_plant.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationInterceptorTest {

    @Test
    void disabledRoleInvalidatesExistingLoginSession() {
        JwtService jwt = mock(JwtService.class);
        TokenBlacklistService blacklist = mock(TokenBlacklistService.class);
        LoginSessionService sessions = mock(LoginSessionService.class);
        UserMapper users = mock(UserMapper.class);
        RoleMapper roles = mock(RoleMapper.class);
        JwtAuthenticationInterceptor interceptor = new JwtAuthenticationInterceptor(
                jwt, blacklist, sessions, users, roles);
        JwtPayload payload = new JwtPayload();
        payload.setAdminId(16L);
        payload.setJti("access-jti");
        payload.setTokenType("access");
        User technician = new User();
        technician.setId(16L);
        technician.setRoleId(3L);
        technician.setStatus(1);
        Role disabledRole = new Role();
        disabledRole.setId(3L);
        disabledRole.setStatus(0);
        when(jwt.parseToken("access-token")).thenReturn(payload);
        when(sessions.isAccessTokenActive(16L, "access-jti")).thenReturn(true);
        when(users.selectById(16L)).thenReturn(technician);
        when(roles.selectById(3L)).thenReturn(disabledRole);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/client/auth/me");
        request.addHeader("Authorization", "Bearer access-token");

        assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}
