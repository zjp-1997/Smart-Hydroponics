package com.smart_plant.smart_plant.service;

public interface LoginRateLimitService {

    void check(String ip, String username);

    void recordFailure(String ip, String username);

    void recordSuccess(String ip, String username);
}
