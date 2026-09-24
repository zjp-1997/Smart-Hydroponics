package com.smart_plant.smart_plant.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;

import com.smart_plant.smart_plant.security.AuthCookieService;

public final class ClientRequestUtils {

    private ClientRequestUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    public static String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "";
        }
        return userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
    }

    public static String getBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        String token = request.getHeader("pdmtoken");
        return token == null || token.isBlank() ? null : token.trim();
    }

    public static String getUploadResourceToken(HttpServletRequest request) {
        String bearerToken = getBearerToken(request);
        if (bearerToken != null) {
            return bearerToken;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AuthCookieService.ACCESS_COOKIE_NAME.equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static boolean isHttps(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String proto = request.getHeader("X-Forwarded-Proto");
        return "https".equalsIgnoreCase(proto);
    }
}
