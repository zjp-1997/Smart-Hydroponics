package com.smart_plant.smart_plant.service;

public interface TokenBlacklistService {

    void blacklist(String jti, long expireAtEpochSeconds);

    boolean isBlacklisted(String jti);
}
