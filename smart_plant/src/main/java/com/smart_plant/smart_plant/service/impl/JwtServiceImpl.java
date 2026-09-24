package com.smart_plant.smart_plant.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_plant.smart_plant.config.AdminAuthProperties;
import com.smart_plant.smart_plant.dto.JwtPayload;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final AdminAuthProperties properties;

    private final ObjectMapper objectMapper;

    @Override
    public String generateToken(User admin, boolean rememberMe) {
        return generateAccessToken(admin, rememberMe);
    }

    @Override
    public String generateAccessToken(User admin, boolean rememberMe) {
        return generateToken(admin, rememberMe, "access", rememberMe
                ? properties.getRememberMeExpirationSeconds()
                : properties.getTokenExpirationSeconds());
    }

    @Override
    public String generateRefreshToken(User admin, boolean rememberMe) {
        return generateToken(admin, rememberMe, "refresh", rememberMe
                ? properties.getRememberMeRefreshTokenExpirationSeconds()
                : properties.getRefreshTokenExpirationSeconds());
    }

    private String generateToken(User admin, boolean rememberMe, String tokenType, long expirationSeconds) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + expirationSeconds;

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", properties.getIssuer());
        payload.put("sub", String.valueOf(admin.getId()));
        payload.put("adminId", admin.getId());
        payload.put("username", admin.getUsername());
        payload.put("role", admin.getRoleCode());
        payload.put("tokenType", tokenType);
        payload.put("rememberMe", rememberMe);
        payload.put("iat", issuedAt);
        payload.put("exp", expiresAt);
        payload.put("jti", UUID.randomUUID().toString());

        String headerPart = encodeJson(header);
        String payloadPart = encodeJson(payload);
        String unsignedToken = headerPart + "." + payloadPart;
        return unsignedToken + "." + sign(unsignedToken);
    }

    @Override
    public JwtPayload parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException(ResponseCode.UNAUTHORIZED, "令牌无效");
            }
            String unsignedToken = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(unsignedToken).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new BusinessException(ResponseCode.UNAUTHORIZED, "令牌无效");
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<>() {
            });
            long expiresAt = ((Number) payload.get("exp")).longValue();
            if (expiresAt <= Instant.now().getEpochSecond()) {
                throw new BusinessException(ResponseCode.UNAUTHORIZED, "令牌已过期");
            }
            return new JwtPayload(
                    ((Number) payload.get("adminId")).longValue(),
                    String.valueOf(payload.get("username")),
                    String.valueOf(payload.get("role")),
                    String.valueOf(payload.getOrDefault("tokenType", "access")),
                    Boolean.TRUE.equals(payload.get("rememberMe")),
                    ((Number) payload.get("iat")).longValue(),
                    expiresAt,
                    String.valueOf(payload.get("jti"))
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "令牌无效");
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new BusinessException(ResponseCode.FAIL, "令牌生成失败");
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new BusinessException(ResponseCode.FAIL, "令牌签名失败");
        }
    }
}
