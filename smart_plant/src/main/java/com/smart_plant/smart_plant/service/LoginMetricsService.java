package com.smart_plant.smart_plant.service;

public interface LoginMetricsService {

    void recordSuccess();

    void recordFailure(String reason);

    void recordRateLimited();
}
