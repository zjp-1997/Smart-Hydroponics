package com.smart_plant.smart_plant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "smart-plant.security-state")
public class SecurityStateProperties {

    private Provider provider = Provider.REDIS;

    private String redisKeyPrefix = "smart-plant:security";

    public enum Provider {
        REDIS,
        MEMORY
    }
}
