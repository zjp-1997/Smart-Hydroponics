package com.smart_plant.smart_plant.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/** 启动时阻止空值、示例密钥和历史默认值进入运行环境。 */
@Component
@RequiredArgsConstructor
public class SecurityConfigurationValidator implements InitializingBean {

    private final AdminAuthProperties adminAuthProperties;

    private final SecurityStateProperties securityStateProperties;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @Override
    public void afterPropertiesSet() {
        String jwtSecret = adminAuthProperties.getJwtSecret();
        String normalizedJwtSecret = jwtSecret == null ? "" : jwtSecret.trim().toLowerCase();
        if (!StringUtils.hasText(jwtSecret)
                || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32
                || normalizedJwtSecret.contains("change-me")
                || normalizedJwtSecret.contains("default")) {
            throw new IllegalStateException("JWT_SECRET 必须是至少 32 字节的高熵随机值，且不能使用示例或默认密钥");
        }
        requireSecret(databasePassword, "DB_PASSWORD");
        if (securityStateProperties.getProvider() == SecurityStateProperties.Provider.REDIS) {
            requireSecret(redisPassword, "REDIS_PASSWORD");
        }
        if (securityStateProperties.getProvider() == SecurityStateProperties.Provider.MEMORY
                && !containsProfile(activeProfiles, "local")
                && !containsProfile(activeProfiles, "dev")
                && !containsProfile(activeProfiles, "test")) {
            throw new IllegalStateException("内存安全状态存储仅允许 local/dev/test 环境，生产环境必须配置 Redis");
        }
        if (!StringUtils.hasText(securityStateProperties.getRedisKeyPrefix())) {
            throw new IllegalStateException("SECURITY_REDIS_KEY_PREFIX 不能为空");
        }
    }

    private boolean containsProfile(String profiles, String expected) {
        if (!StringUtils.hasText(profiles)) {
            return false;
        }
        for (String profile : profiles.split(",")) {
            if (expected.equalsIgnoreCase(profile.trim())) {
                return true;
            }
        }
        return false;
    }

    private void requireSecret(String value, String environmentName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(environmentName + " 必须通过环境变量或外部密钥管理系统提供");
        }
    }
}
