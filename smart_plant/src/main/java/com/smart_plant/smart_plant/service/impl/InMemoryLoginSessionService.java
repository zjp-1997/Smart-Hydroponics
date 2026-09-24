package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.service.LoginSessionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 仅用于 local/dev/test 单实例调试；应用重启后会话主动失效。 */
@Service
@ConditionalOnProperty(prefix = "smart-plant.security-state", name = "provider", havingValue = "memory")
public class InMemoryLoginSessionService implements LoginSessionService {

    private final ConcurrentMap<Long, LoginSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(Long userId, String accessJti, long accessExpireAt, String refreshJti, long refreshExpireAt) {
        if (userId == null || accessJti == null || refreshJti == null || refreshExpireAt <= now()) {
            revoke(userId);
            return;
        }
        sessions.put(userId, new LoginSession(accessJti, accessExpireAt, refreshJti, refreshExpireAt));
    }

    @Override
    public boolean isAccessTokenActive(Long userId, String accessJti) {
        LoginSession session = read(userId);
        return session != null && session.accessExpireAt() > now() && session.accessJti().equals(accessJti);
    }

    @Override
    public boolean isRefreshTokenActive(Long userId, String refreshJti) {
        LoginSession session = read(userId);
        return session != null && session.refreshExpireAt() > now() && session.refreshJti().equals(refreshJti);
    }

    @Override
    public void revoke(Long userId) {
        if (userId != null) {
            sessions.remove(userId);
        }
    }

    private LoginSession read(Long userId) {
        if (userId == null) {
            return null;
        }
        LoginSession session = sessions.get(userId);
        if (session != null && session.refreshExpireAt() <= now()) {
            sessions.remove(userId, session);
            return null;
        }
        return session;
    }

    private long now() {
        return Instant.now().getEpochSecond();
    }

    private record LoginSession(String accessJti, long accessExpireAt, String refreshJti, long refreshExpireAt) {
    }
}
