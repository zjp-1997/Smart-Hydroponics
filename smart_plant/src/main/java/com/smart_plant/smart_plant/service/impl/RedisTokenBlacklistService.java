package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.config.SecurityStateProperties;
import com.smart_plant.smart_plant.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@ConditionalOnProperty(prefix = "smart-plant.security-state", name = "provider", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    private final SecurityStateProperties properties;

    @Override
    public void blacklist(String jti, long expireAtEpochSeconds) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        long ttlSeconds = expireAtEpochSeconds - Instant.now().getEpochSecond();
        if (ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(key(jti), "1", Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return jti != null && !jti.isBlank() && Boolean.TRUE.equals(redisTemplate.hasKey(key(jti)));
    }

    private String key(String jti) {
        return RedisSecurityKeys.key(properties.getRedisKeyPrefix(), "token-blacklist", jti);
    }
}
