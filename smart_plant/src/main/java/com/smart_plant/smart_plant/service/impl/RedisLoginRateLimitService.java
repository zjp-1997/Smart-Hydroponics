package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.config.AdminAuthProperties;
import com.smart_plant.smart_plant.config.SecurityStateProperties;
import com.smart_plant.smart_plant.exception.RateLimitException;
import com.smart_plant.smart_plant.service.LoginRateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(prefix = "smart-plant.security-state", name = "provider", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisLoginRateLimitService implements LoginRateLimitService {

    /** INCR 与首次设置 TTL 在 Redis 内原子执行，避免多实例并发时丢失计数或产生永久 Key。 */
    private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>("""
            local attempts = redis.call('INCR', KEYS[1])
            if attempts == 1 or attempts == tonumber(ARGV[2]) then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return attempts
            """, Long.class);

    private final AdminAuthProperties authProperties;

    private final SecurityStateProperties stateProperties;

    private final StringRedisTemplate redisTemplate;

    @Override
    public void check(String ip, String username) {
        checkKey(ipKey(ip), authProperties.getIpMaxFailures(), "IP登录失败次数过多，请稍后再试");
        checkKey(accountKey(username), authProperties.getAccountMaxFailures(), "账号登录失败次数过多，请稍后再试");
    }

    @Override
    public void recordFailure(String ip, String username) {
        increment(ipKey(ip), authProperties.getIpMaxFailures(), Duration.ofMinutes(authProperties.getIpLockMinutes()));
        increment(accountKey(username), authProperties.getAccountMaxFailures(), Duration.ofMinutes(authProperties.getAccountLockMinutes()));
    }

    @Override
    public void recordSuccess(String ip, String username) {
        redisTemplate.delete(List.of(ipKey(ip), accountKey(username)));
    }

    private void checkKey(String key, int maxFailures, String message) {
        String storedAttempts = redisTemplate.opsForValue().get(key);
        if (storedAttempts == null) {
            return;
        }
        try {
            if (Long.parseLong(storedAttempts) < maxFailures) {
                return;
            }
        } catch (NumberFormatException exception) {
            redisTemplate.delete(key);
            return;
        }
        Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        long waitSeconds = ttlSeconds == null || ttlSeconds < 0 ? 1 : Math.max(1, ttlSeconds);
        throw new RateLimitException(message + "，请等待" + waitSeconds + "秒后重试");
    }

    private void increment(String key, int maxFailures, Duration ttl) {
        redisTemplate.execute(
                INCREMENT_WITH_TTL,
                List.of(key),
                Long.toString(Math.max(1, ttl.toSeconds())),
                Integer.toString(maxFailures)
        );
    }

    private String ipKey(String ip) {
        return RedisSecurityKeys.key(stateProperties.getRedisKeyPrefix(), "login-rate:ip", normalize(ip));
    }

    private String accountKey(String username) {
        return RedisSecurityKeys.key(stateProperties.getRedisKeyPrefix(), "login-rate:account", normalize(username));
    }

    private String normalize(String value) {
        return value == null ? "unknown" : value.trim().toLowerCase();
    }
}
