package com.smart_plant.smart_plant.utils;

import com.smart_plant.smart_plant.security.AuthCookieService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClientRequestUtilsTest {

    @Test
    void uploadResourceCanUseHttpOnlyCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthCookieService.ACCESS_COOKIE_NAME, "cookie-token"));

        assertEquals("cookie-token", ClientRequestUtils.getUploadResourceToken(request));
        assertNull(ClientRequestUtils.getBearerToken(request));
    }

    @Test
    void bearerHeaderTakesPrecedenceOverCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer header-token");
        request.setCookies(new Cookie(AuthCookieService.ACCESS_COOKIE_NAME, "cookie-token"));

        assertEquals("header-token", ClientRequestUtils.getUploadResourceToken(request));
    }
}
