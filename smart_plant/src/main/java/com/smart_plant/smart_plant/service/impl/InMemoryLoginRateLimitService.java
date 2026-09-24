package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.config.AdminAuthProperties;
import com.smart_plant.smart_plant.exception.RateLimitException;
import com.smart_plant.smart_plant.service.LoginRateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 仅用于 local/dev/test 单实例调试；生产环境必须使用 Redis 原子计数。 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smart-plant.security-state", name = "provider", havingValue = "memory")
public class InMemoryLoginRateLimitService implements LoginRateLimitService {

    private final AdminAuthProperties authProperties;
    private final ConcurrentMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    @Override
    public void check(String ip, String username) {
        checkKey(ipKey(ip), authProperties.getIpMaxFailures(), "IP登录失败次数过多，请稍后再试");
        checkKey(accountKey(username), authProperties.getAccountMaxFailures(), "账号登录失败次数过多，请稍后再试");
    }

    @Override
    public void recordFailure(String ip, String username) {
        increment(ipKey(ip), Duration.ofMinutes(authProperties.getIpLockMinutes()));
        increment(accountKey(username), Duration.ofMinutes(authProperties.getAccountLockMinutes()));
    }

    @Override
    public void recordSuccess(String ip, String username) {
        attempts.remove(ipKey(ip));
        attempts.remove(accountKey(username));
    }

    private void checkKey(String key, int maxFailures, String message) {
        Attempt attempt = current(key);
        if (attempt != null && attempt.count() >= maxFailures) {
            long waitSeconds = Math.max(1, attempt.expireAtEpochSecond() - now());
            throw new RateLimitException(message + "，请等待" + waitSeconds + "秒后重试");
        }
    }

    private void increment(String key, Duration ttl) {
        long now = now();
        attempts.compute(key, (ignored, previous) -> {
            if (previous == null || previous.expireAtEpochSecond() <= now) {
                return new Attempt(1, now + Math.max(1, ttl.toSeconds()));
            }
            return new Attempt(previous.count() + 1, previous.expireAtEpochSecond());
        });
    }

    private Attempt current(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt != null && attempt.expireAtEpochSecond() <= now()) {
            attempts.remove(key, attempt);
            return null;
        }
        return attempt;
    }

    private String ipKey(String ip) {
        return "ip:" + normalize(ip);
    }

    private String accountKey(String username) {
        return "account:" + normalize(username);
    }

    private String normalize(String value) {
        return value == null ? "unknown" : value.trim().toLowerCase();
    }

    private long now() {
        return Instant.now().getEpochSecond();
    }

    private record Attempt(long count, long expireAtEpochSecond) {
    }
}
