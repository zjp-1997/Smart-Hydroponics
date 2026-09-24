package com.smart_plant.smart_plant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 第三方天气服务配置。
 *
 * <p>高德 Web 服务 Key 和空气质量 Token 统一放在后端配置中，
 * 避免直接暴露在 farm 移动端代码里。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "smart-plant.weather")
public class WeatherProperties {

    /** 高德 Web 服务 Key，用于逆地理编码和天气查询。 */
    private String amapWebServiceKey;

    /** WAQI 空气质量 Token，用于查询 PM2.5；为空时 PM2.5 返回 --。 */
    private String waqiToken;

    /** 是否启用 Open-Meteo 作为无 Key 的温湿度和 PM2.5 兜底数据源。 */
    private boolean openMeteoFallbackEnabled = true;

    /** 接口超时时间，避免首页等待第三方接口过久。 */
    private int timeoutSeconds = 5;

    /** 天气缓存分钟数，降低首页频繁刷新时的第三方接口调用量。 */
    private int cacheMinutes = 10;

    /** 高德 Key 未配置或第三方失败时的默认城市。 */
    private String defaultCity = "漳州";

    /** 默认行政区编码，作为兜底展示数据使用。 */
    private String defaultAdcode = "350600";
}
