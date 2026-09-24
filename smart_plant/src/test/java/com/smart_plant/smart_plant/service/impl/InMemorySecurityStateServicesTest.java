package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.config.AdminAuthProperties;
import com.smart_plant.smart_plant.exception.RateLimitException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemorySecurityStateServicesTest {

    @Test
    void storesAndExpiresBlacklistEntries() {
        InMemoryTokenBlacklistService service = new InMemoryTokenBlacklistService();
        long now = Instant.now().getEpochSecond();
        service.blacklist("active", now + 60);
        service.blacklist("expired", now - 1);
        assertTrue(service.isBlacklisted("active"));
        assertFalse(service.isBlacklisted("expired"));
    }

    @Test
    void storesValidatesAndRevokesOneSessionPerUser() {
        InMemoryLoginSessionService service = new InMemoryLoginSessionService();
        long now = Instant.now().getEpochSecond();
        service.save(7L, "access", now + 60, "refresh", now + 120);
        assertTrue(service.isAccessTokenActive(7L, "access"));
        assertTrue(service.isRefreshTokenActive(7L, "refresh"));
        service.revoke(7L);
        assertFalse(service.isAccessTokenActive(7L, "access"));
    }

    @Test
    void limitsAndClearsLocalLoginFailures() {
        AdminAuthProperties properties = new AdminAuthProperties();
        properties.setIpMaxFailures(2);
        properties.setAccountMaxFailures(2);
        properties.setIpLockMinutes(1);
        properties.setAccountLockMinutes(1);
        InMemoryLoginRateLimitService service = new InMemoryLoginRateLimitService(properties);
        service.recordFailure("127.0.0.1", "admin");
        service.recordFailure("127.0.0.1", "admin");
        assertThrows(RateLimitException.class, () -> service.check("127.0.0.1", "admin"));
        service.recordSuccess("127.0.0.1", "admin");
        service.check("127.0.0.1", "admin");
    }
}
