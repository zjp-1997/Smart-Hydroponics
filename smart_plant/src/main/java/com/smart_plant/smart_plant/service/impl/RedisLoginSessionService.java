package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.config.SecurityStateProperties;
import com.smart_plant.smart_plant.service.LoginSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@ConditionalOnProperty(prefix = "smart-plant.security-state", name = "provider", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisLoginSessionService implements LoginSessionService {

    private static final String FIELD_SEPARATOR = "|";

    private final StringRedisTemplate redisTemplate;

    private final SecurityStateProperties properties;

    @Override
    public void save(Long userId, String accessJti, long accessExpireAt, String refreshJti, long refreshExpireAt) {
        if (userId == null || accessJti == null || refreshJti == null) {
            return;
        }
        long ttlSeconds = refreshExpireAt - Instant.now().getEpochSecond();
        if (ttlSeconds <= 0) {
            revoke(userId);
            return;
        }
        String value = String.join(FIELD_SEPARATOR,
                accessJti,
                Long.toString(accessExpireAt),
                refreshJti,
                Long.toString(refreshExpireAt));
        redisTemplate.opsForValue().set(key(userId), value, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public boolean isAccessTokenActive(Long userId, String accessJti) {
        LoginSession session = read(userId);
        return session != null
                && session.accessExpireAt() > Instant.now().getEpochSecond()
                && session.accessJti().equals(accessJti);
    }

    @Override
    public boolean isRefreshTokenActive(Long userId, String refreshJti) {
        LoginSession session = read(userId);
        return session != null
                && session.refreshExpireAt() > Instant.now().getEpochSecond()
                && session.refreshJti().equals(refreshJti);
    }

    @Override
    public void revoke(Long userId) {
        if (userId != null) {
            redisTemplate.delete(key(userId));
        }
    }

    private LoginSession read(Long userId) {
        if (userId == null) {
            return null;
        }
        String value = redisTemplate.opsForValue().get(key(userId));
        if (value == null) {
            return null;
        }
        String[] parts = value.split("\\|", -1);
        if (parts.length != 4) {
            redisTemplate.delete(key(userId));
            return null;
        }
        try {
            return new LoginSession(parts[0], Long.parseLong(parts[1]), parts[2], Long.parseLong(parts[3]));
        } catch (NumberFormatException exception) {
            redisTemplate.delete(key(userId));
            return null;
        }
    }

    private String key(Long userId) {
        return RedisSecurityKeys.key(properties.getRedisKeyPrefix(), "login-session", userId);
    }

    private record LoginSession(String accessJti, long accessExpireAt, String refreshJti, long refreshExpireAt) {
    }
}
