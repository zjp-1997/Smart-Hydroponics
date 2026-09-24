package com.smart_plant.smart_plant.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.response.ResponseCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationalEndpointSecurityFilterTest {

    @Test
    void protectsPrometheusBeforeActuatorHandlerMapping() throws Exception {
        JwtAuthenticationInterceptor interceptor = mock(JwtAuthenticationInterceptor.class);
        OperationalEndpointSecurityFilter filter = new OperationalEndpointSecurityFilter(interceptor, mock(ObjectMapper.class));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/smart_plant/actuator/prometheus");
        request.setContextPath("/smart_plant");
        when(interceptor.preHandle(any(), any(), any())).thenReturn(true);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(interceptor).preHandle(any(), any(), any());
        verify(interceptor).afterCompletion(any(), any(), any(), any());
    }

    @Test
    void leavesHealthProbeAnonymous() throws Exception {
        JwtAuthenticationInterceptor interceptor = mock(JwtAuthenticationInterceptor.class);
        OperationalEndpointSecurityFilter filter = new OperationalEndpointSecurityFilter(interceptor, mock(ObjectMapper.class));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/smart_plant/actuator/health");
        request.setContextPath("/smart_plant");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(interceptor, never()).preHandle(any(), any(), any());
    }

    @Test
    void returnsUnauthorizedInsteadOfLeakingFilterException() throws Exception {
        JwtAuthenticationInterceptor interceptor = mock(JwtAuthenticationInterceptor.class);
        ObjectMapper objectMapper = new ObjectMapper();
        OperationalEndpointSecurityFilter filter = new OperationalEndpointSecurityFilter(interceptor, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/smart_plant/actuator/prometheus");
        request.setContextPath("/smart_plant");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(interceptor.preHandle(any(), any(), any()))
                .thenThrow(new BusinessException(ResponseCode.UNAUTHORIZED, "Missing authentication token"));

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }
}
