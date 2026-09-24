package com.smart_plant.smart_plant.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.Map;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebMvcConfigTest {

    @Test
    void onlyPublicSystemAssetsBypassAuthentication() throws Exception {
        var field = WebMvcConfig.class.getDeclaredField("ANONYMOUS_PATHS");
        field.setAccessible(true);
        var paths = Arrays.asList((String[]) field.get(null));

        assertTrue(paths.contains("/uploads/system-settings/**"));
        assertFalse(paths.contains("/uploads/**"));
    }

    @Test
    void shouldAllowFarmFromTenDotPrivateNetwork() {
        TestCorsRegistry registry = new TestCorsRegistry();
        // CORS 配置不依赖拦截器实例，传入 null 即可单独验证来源匹配规则。
        new WebMvcConfig(null, null, null).addCorsMappings(registry);
        CorsConfiguration configuration = registry.configurations().get("/**");

        // 覆盖 farm 在局域网 H5 调试时常见的 HTTP 与 HTTPS 来源。
        assertEquals("http://10.98.81.227:5173", configuration.checkOrigin("http://10.98.81.227:5173"));
        assertEquals("https://10.98.81.227:5173", configuration.checkOrigin("https://10.98.81.227:5173"));
    }

    /**
     * CorsRegistry 将最终配置保存在 protected 方法中，测试子类只负责暴露只读结果。
     */
    private static final class TestCorsRegistry extends CorsRegistry {

        private Map<String, CorsConfiguration> configurations() {
            return super.getCorsConfigurations();
        }
    }
}
