package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.service.LoginMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class LoginMetricsServiceImpl implements LoginMetricsService {

    private final Counter successCounter;

    private final Counter rateLimitedCounter;

    private final MeterRegistry meterRegistry;

    public LoginMetricsServiceImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.successCounter = Counter.builder("admin_login_success_total")
                .description("Total successful admin login attempts")
                .register(meterRegistry);
        this.rateLimitedCounter = Counter.builder("admin_login_rate_limited_total")
                .description("Total rate limited admin login attempts")
                .register(meterRegistry);
    }

    @Override
    public void recordSuccess() {
        successCounter.increment();
    }

    @Override
    public void recordFailure(String reason) {
        Counter.builder("admin_login_failure_total")
                .description("Total failed admin login attempts")
                .tag("reason", reason == null ? "unknown" : reason)
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void recordRateLimited() {
        rateLimitedCounter.increment();
    }
}
