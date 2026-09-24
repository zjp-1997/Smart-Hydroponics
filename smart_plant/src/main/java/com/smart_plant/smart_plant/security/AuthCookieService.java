package com.smart_plant.smart_plant.security;

import com.smart_plant.smart_plant.dto.JwtPayload;
import com.smart_plant.smart_plant.service.JwtService;
import com.smart_plant.smart_plant.utils.ClientRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * 为浏览器图片请求写入 HttpOnly 辅助 Cookie。
 * Cookie 仅由鉴权拦截器在 GET /uploads/** 场景读取，不能用于普通 API 或写操作。
 */
@Component
@RequiredArgsConstructor
public class AuthCookieService {

    public static final String ACCESS_COOKIE_NAME = "smart_plant_access";

    private final JwtService jwtService;

    public void writeAccessCookie(String accessToken, HttpServletRequest request, HttpServletResponse response) {
        JwtPayload payload = jwtService.parseToken(accessToken);
        long ttlSeconds = Math.max(1, payload.getExp() - Instant.now().getEpochSecond());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(accessToken, request, Duration.ofSeconds(ttlSeconds)).toString());
    }

    public void clearAccessCookie(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", request, Duration.ZERO).toString());
    }

    private ResponseCookie cookie(String value, HttpServletRequest request, Duration maxAge) {
        return ResponseCookie.from(ACCESS_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(ClientRequestUtils.isHttps(request))
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
