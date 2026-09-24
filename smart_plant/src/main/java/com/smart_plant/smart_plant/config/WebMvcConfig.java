package com.smart_plant.smart_plant.config;

import com.smart_plant.smart_plant.security.JwtAuthenticationInterceptor;
import com.smart_plant.smart_plant.security.ExpertClientAccessInterceptor;
import com.smart_plant.smart_plant.security.PermissionAuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 仅保留完成登录所必需的入口和不泄露内部状态的健康探针。
     * Swagger 在配置层默认关闭；即使临时开启，也必须通过 JWT 鉴权。
     */
    private static final String[] ANONYMOUS_PATHS = {
            "/admin/auth/login",
            "/admin/auth/captcha",
            "/admin/auth/refresh",
            "/api/auth/login",
            "/api/auth/captcha",
            "/api/auth/refresh",
            "/client/auth/register",
            "/client/auth/register/farm-owners",
            "/client/auth/login",
            "/client/auth/sms-code/login",
            "/client/auth/sms-login",
            "/api/client/auth/register",
            "/api/client/auth/register/farm-owners",
            "/api/client/auth/login",
            "/api/client/auth/sms-code/login",
            "/api/client/auth/sms-login",
            // 登录页需在未认证状态下读取系统名称、图标和背景等公开配置。
            "/public/system-settings",
            // 登录页品牌图片属于明确公开资源；其他上传内容必须经过 JWT 和资源授权。
            "/uploads/system-settings/**",
            "/ws/**",
            "/error",
            "/favicon.ico",
            "/actuator/health",
            "/actuator/health/**"
    };

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    private final PermissionAuthorizationInterceptor permissionAuthorizationInterceptor;
    private final ExpertClientAccessInterceptor expertClientAccessInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "https://localhost:*",
                        "https://127.0.0.1:*",
                        "http://192.168.*.*:*",
                        "https://192.168.*.*:*",
                        // farm 在真机或局域网 H5 调试时会使用 10.x 地址，需允许该私网来源读取 API 与图片响应。
                        "http://10.*.*.*:*",
                        "https://10.*.*.*:*"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                // 下载接口会返回 Content-Disposition，跨域时必须显式暴露该响应头供前端读取文件信息。
                .exposedHeaders(
                        "Authorization",
                        "Content-Disposition",
                        "Content-Length",
                        "Content-Type"
                )
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadLocation = Paths.get(System.getProperty("user.dir"))
                .resolve("uploads")
                .toUri()
                .toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT 鉴权默认保护全部接口，上传文件、指标和临时开启的接口文档均不再匿名。
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(ANONYMOUS_PATHS);
        // 权限拦截器在 JWT 之后执行，并与鉴权层共享唯一匿名白名单。
        registry.addInterceptor(permissionAuthorizationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(ANONYMOUS_PATHS);
        // 专家角色在通过 JWT 后仍只能调用账号和本人咨询接口，页面隐藏不作为权限依据。
        registry.addInterceptor(expertClientAccessInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(ANONYMOUS_PATHS);
    }
}
