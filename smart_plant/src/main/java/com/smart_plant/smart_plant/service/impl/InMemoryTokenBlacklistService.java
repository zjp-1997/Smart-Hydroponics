package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.service.TokenBlacklistService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 仅用于 local/dev/test 单实例调试；生产环境由启动校验禁止启用。 */
@Service
@ConditionalOnProperty(prefix = "smart-plant.security-state", name = "provider", havingValue = "memory")
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    private final ConcurrentMap<String, Long> expiryByJti = new ConcurrentHashMap<>();

    @Override
    public void blacklist(String jti, long expireAtEpochSeconds) {
        if (jti != null && !jti.isBlank() && expireAtEpochSeconds > now()) {
            expiryByJti.put(jti, expireAtEpochSeconds);
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Long expiry = expiryByJti.get(jti);
        if (expiry == null) {
            return false;
        }
        if (expiry <= now()) {
            expiryByJti.remove(jti, expiry);
            return false;
        }
        return true;
    }

    private long now() {
        return Instant.now().getEpochSecond();
    }
}
