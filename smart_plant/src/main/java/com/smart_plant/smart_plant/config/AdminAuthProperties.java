package com.smart_plant.smart_plant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "smart-plant.admin-auth")
public class AdminAuthProperties {

    private String jwtSecret;

    private String issuer = "smart_plant";

    private long tokenExpirationSeconds = 7200;

    private long refreshTokenExpirationSeconds = 2592000;

    private long rememberMeExpirationSeconds = 604800;

    private long rememberMeRefreshTokenExpirationSeconds = 2592000;

    private long captchaExpirationSeconds = 300;

    private boolean requireHttps = true;

    private int ipMaxFailures = 5;

    private int ipLockMinutes = 15;

    private int accountMaxFailures = 5;

    private int accountLockMinutes = 30;
}
