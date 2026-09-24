package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.config.AdminAuthProperties;
import com.smart_plant.smart_plant.dto.AdminCaptchaResponse;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class InMemoryCaptchaService implements CaptchaService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AdminAuthProperties properties;

    private final Map<String, CaptchaState> captchas = new ConcurrentHashMap<>();

    @Override
    public AdminCaptchaResponse create() {
        String code = createCode();
        String key = UUID.randomUUID().toString();
        long expireAt = Instant.now().getEpochSecond() + properties.getCaptchaExpirationSeconds();
        captchas.put(key, new CaptchaState(code, expireAt));
        return new AdminCaptchaResponse(key, createSvgDataUri(code), properties.getCaptchaExpirationSeconds());
    }

    @Override
    public void verify(String captchaKey, String captcha) {
        if (!StringUtils.hasText(captchaKey)) {
            return;
        }
        CaptchaState state = captchas.remove(captchaKey);
        if (state == null || state.expireAt <= Instant.now().getEpochSecond()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "验证码已过期，请重新获取");
        }
        if (!StringUtils.hasText(captcha) || !state.code.equals(captcha.trim().toUpperCase(Locale.ROOT))) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "验证码不正确");
        }
    }

    private String createCode() {
        StringBuilder code = new StringBuilder(4);
        for (int index = 0; index < 4; index++) {
            code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    private String createSvgDataUri(String code) {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="132" height="48" viewBox="0 0 132 48">
                  <rect width="132" height="48" rx="8" fill="#ecfeff"/>
                  <path d="M8 34 C32 18, 58 44, 82 23 S116 16, 126 30" fill="none" stroke="#2aa0ad" stroke-width="2" opacity=".35"/>
                  <text x="66" y="31" text-anchor="middle" font-family="Arial, sans-serif" font-size="24" font-weight="700" letter-spacing="5" fill="#0f766e">%s</text>
                </svg>
                """.formatted(code);
        String encoded = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return "data:image/svg+xml;base64," + encoded;
    }

    private record CaptchaState(String code, long expireAt) {
    }
}
