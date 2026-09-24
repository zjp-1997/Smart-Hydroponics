package com.smart_plant.smart_plant.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigurationValidatorTest {

    @Test
    void acceptsExternallyProvidedStrongSecrets() {
        SecurityConfigurationValidator validator = validatorWith("vG7!2mQ9#xR4$pL8@kN6&cT3*eW5zY1u");
        assertDoesNotThrow(validator::afterPropertiesSet);
    }

    @Test
    void rejectsWeakJwtSecret() {
        SecurityConfigurationValidator validator = validatorWith("too-short");
        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
    }

    @Test
    void acceptsMemoryProviderForLocalDevelopmentWithoutRedisPassword() {
        SecurityConfigurationValidator validator = validatorWith("vG7!2mQ9#xR4$pL8@kN6&cT3*eW5zY1u");
        SecurityStateProperties properties = (SecurityStateProperties) ReflectionTestUtils.getField(validator, "securityStateProperties");
        properties.setProvider(SecurityStateProperties.Provider.MEMORY);
        ReflectionTestUtils.setField(validator, "redisPassword", "");
        ReflectionTestUtils.setField(validator, "activeProfiles", "local");
        assertDoesNotThrow(validator::afterPropertiesSet);
    }

    @Test
    void rejectsMemoryProviderInProduction() {
        SecurityConfigurationValidator validator = validatorWith("vG7!2mQ9#xR4$pL8@kN6&cT3*eW5zY1u");
        SecurityStateProperties properties = (SecurityStateProperties) ReflectionTestUtils.getField(validator, "securityStateProperties");
        properties.setProvider(SecurityStateProperties.Provider.MEMORY);
        ReflectionTestUtils.setField(validator, "activeProfiles", "prod");
        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
    }

    private SecurityConfigurationValidator validatorWith(String jwtSecret) {
        AdminAuthProperties authProperties = new AdminAuthProperties();
        authProperties.setJwtSecret(jwtSecret);
        SecurityStateProperties stateProperties = new SecurityStateProperties();
        SecurityConfigurationValidator validator = new SecurityConfigurationValidator(authProperties, stateProperties);
        ReflectionTestUtils.setField(validator, "databasePassword", "database-test-secret");
        ReflectionTestUtils.setField(validator, "redisPassword", "redis-test-secret");
        ReflectionTestUtils.setField(validator, "activeProfiles", "");
        return validator;
    }
}
