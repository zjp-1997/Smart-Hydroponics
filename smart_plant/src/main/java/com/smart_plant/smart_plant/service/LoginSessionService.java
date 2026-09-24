package com.smart_plant.smart_plant.service;

public interface LoginSessionService {

    void save(Long userId, String accessJti, long accessExpireAt, String refreshJti, long refreshExpireAt);

    boolean isAccessTokenActive(Long userId, String accessJti);

    boolean isRefreshTokenActive(Long userId, String refreshJti);

    void revoke(Long userId);
}
