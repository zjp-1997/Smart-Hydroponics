package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.config.AdminAuthProperties;
import com.smart_plant.smart_plant.config.SecurityStateProperties;
import com.smart_plant.smart_plant.exception.RateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisSecurityStateServiceTest {

    private StringRedisTemplate redisTemplate;

    private ValueOperations<String, String> valueOperations;

    private SecurityStateProperties stateProperties;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        stateProperties = new SecurityStateProperties();
        stateProperties.setRedisKeyPrefix("test:security");
    }

    @Test
    void blacklistUsesRedisTtlAndHashedKey() {
        RedisTokenBlacklistService service = new RedisTokenBlacklistService(redisTemplate, stateProperties);
        service.blacklist("sensitive-jti", Instant.now().plusSeconds(60).getEpochSecond());

        verify(valueOperations).set(anyString(), eq("1"), any(Duration.class));
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        assertTrue(service.isBlacklisted("sensitive-jti"));
        assertFalse(service.isBlacklisted(""));
    }

    @Test
    void loginSessionValidatesJtiAndExpiryFromRedis() {
        RedisLoginSessionService service = new RedisLoginSessionService(redisTemplate, stateProperties);
        long now = Instant.now().getEpochSecond();
        when(valueOperations.get(anyString())).thenReturn("access-jti|" + (now + 60) + "|refresh-jti|" + (now + 120));

        assertTrue(service.isAccessTokenActive(1L, "access-jti"));
        assertTrue(service.isRefreshTokenActive(1L, "refresh-jti"));
        assertFalse(service.isAccessTokenActive(1L, "wrong-jti"));
    }

    @Test
    void rateLimitUsesSharedCounterTtl() {
        AdminAuthProperties authProperties = new AdminAuthProperties();
        RedisLoginRateLimitService service = new RedisLoginRateLimitService(authProperties, stateProperties, redisTemplate);
        when(valueOperations.get(anyString())).thenReturn("5");
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(90L);

        assertThrows(RateLimitException.class, () -> service.check("127.0.0.1", "admin"));
    }
}
