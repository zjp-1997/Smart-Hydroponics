package com.smart_plant.smart_plant.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_plant.smart_plant.config.WeatherProperties;
import com.smart_plant.smart_plant.dto.ClientWeatherResponse;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.ClientWeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * farm 首页天气服务实现。
 *
 * <p>高德负责经纬度转行政区和基础天气，WAQI 作为可选空气质量来源提供 PM2.5。</p>
 */
@Service
@RequiredArgsConstructor
public class ClientWeatherServiceImpl implements ClientWeatherService {

    private static final String AMAP_RE_GEO_URL = "https://restapi.amap.com/v3/geocode/regeo";
    private static final String AMAP_WEATHER_URL = "https://restapi.amap.com/v3/weather/weatherInfo";
    private static final String WAQI_GEO_URL = "https://api.waqi.info/feed/geo:%s;%s/";
    private static final String OPEN_METEO_WEATHER_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String OPEN_METEO_AIR_URL = "https://air-quality-api.open-meteo.com/v1/air-quality";
    private static final String NOMINATIM_REVERSE_URL = "https://nominatim.openstreetmap.org/reverse";

    private final WeatherProperties weatherProperties;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry> weatherCache = new ConcurrentHashMap<>();

    @Override
    public ClientWeatherResponse getCurrentWeather(Double longitude, Double latitude, String coordinateSystem) {
        validateCoordinate(longitude, latitude);
        String normalizedCoordinateSystem = normalizeCoordinateSystem(coordinateSystem);
        String cacheKey = buildCacheKey(longitude, latitude, normalizedCoordinateSystem);
        CacheEntry cached = weatherCache.get(cacheKey);
        if (cached != null && cached.isValid(weatherProperties.getCacheMinutes())) {
            return cached.response();
        }

        ClientWeatherResponse response = requestCurrentWeather(longitude, latitude, normalizedCoordinateSystem);
        weatherCache.put(cacheKey, new CacheEntry(response, Instant.now()));
        return response;
    }

    private ClientWeatherResponse requestCurrentWeather(Double longitude, Double latitude, String coordinateSystem) {
        try {
            Coordinate amapCoordinate = toAmapCoordinate(longitude, latitude, coordinateSystem);
            LocationInfo locationInfo = resolveLocationInfo(amapCoordinate, longitude, latitude);
            ClientWeatherResponse weather = resolveWeatherInfo(locationInfo, longitude, latitude);
            weather.setPm25(requestPm25(longitude, latitude));
            return weather;
        } catch (Exception exception) {
            // 首页天气属于辅助信息，第三方接口异常时返回兜底数据，避免影响首页主体功能。
            return buildFallbackResponse();
        }
    }

    private LocationInfo resolveLocationInfo(Coordinate amapCoordinate, Double longitude, Double latitude) {
        if (StringUtils.hasText(weatherProperties.getAmapWebServiceKey())) {
            try {
                return requestLocationInfo(amapCoordinate.longitude(), amapCoordinate.latitude());
            } catch (Exception exception) {
                // Key 无效或高德临时不可用时，仍按当前位置走无 Key 的逆地理编码兜底。
            }
        }
        return requestNominatimLocation(longitude, latitude);
    }

    private LocationInfo requestNominatimLocation(Double longitude, Double latitude) {
        try {
            String url = NOMINATIM_REVERSE_URL
                    + "?format=jsonv2&addressdetails=1"
                    + "&lat=" + latitude
                    + "&lon=" + longitude
                    + "&accept-language=zh-CN";
            JsonNode address = requestJson(url).path("address");
            String location = firstNonBlank(
                    address.path("district").asText(""),
                    address.path("city_district").asText(""),
                    address.path("county").asText(""),
                    address.path("city").asText(""),
                    address.path("town").asText(""),
                    address.path("state").asText(""));
            return new LocationInfo("", firstNonBlank(location, weatherProperties.getDefaultCity()));
        } catch (Exception exception) {
            return new LocationInfo("", weatherProperties.getDefaultCity());
        }
    }

    private ClientWeatherResponse resolveWeatherInfo(LocationInfo locationInfo, Double longitude, Double latitude) throws Exception {
        if (StringUtils.hasText(weatherProperties.getAmapWebServiceKey())) {
            try {
                return requestAmapWeather(locationInfo);
            } catch (Exception exception) {
                // 区县 adcode 偶发无 live 数据时降级到经纬度天气，保留高德解析出来的位置名称。
                return safeOpenMeteoWeather(locationInfo, longitude, latitude);
            }
        }
        return safeOpenMeteoWeather(locationInfo, longitude, latitude);
    }

    private ClientWeatherResponse safeOpenMeteoWeather(LocationInfo locationInfo, Double longitude, Double latitude) {
        try {
            return requestOpenMeteoWeather(locationInfo, longitude, latitude);
        } catch (Exception exception) {
            return buildFallbackResponse(locationInfo);
        }
    }

    private LocationInfo requestLocationInfo(Double longitude, Double latitude) throws Exception {
        String url = AMAP_RE_GEO_URL
                + "?key=" + encode(weatherProperties.getAmapWebServiceKey())
                + "&location=" + longitude + "," + latitude
                + "&extensions=base";
        JsonNode root = requestJson(url);
        assertAmapSuccess(root, "高德逆地理编码失败");
        JsonNode component = root.path("regeocode").path("addressComponent");
        String adcode = component.path("adcode").asText(weatherProperties.getDefaultAdcode());
        String city = firstText(component.path("city"));
        String district = component.path("district").asText("");
        String province = component.path("province").asText("");
        String location = firstNonBlank(district, city, province, weatherProperties.getDefaultCity());
        return new LocationInfo(adcode, location);
    }

    private ClientWeatherResponse requestAmapWeather(LocationInfo locationInfo) throws Exception {
        JsonNode live = requestAmapWeatherLive(locationInfo.adcode());
        if ((live.isMissingNode() || live.isNull()) && locationInfo.adcode().length() >= 4) {
            // 个别区县天气没有实时 live 时，尝试使用地市级 adcode 查询，提升温湿度命中率。
            live = requestAmapWeatherLive(locationInfo.adcode().substring(0, 4) + "00");
        }
        if (live.isMissingNode() || live.isNull()) {
            throw new BusinessException(ResponseCode.SERVICE_UNAVAILABLE, "高德天气未返回实时数据");
        }
        return new ClientWeatherResponse(
                firstNonBlank(locationInfo.location(), live.path("city").asText(""), weatherProperties.getDefaultCity()),
                firstNonBlank(live.path("adcode").asText(""), locationInfo.adcode(), weatherProperties.getDefaultAdcode()),
                live.path("weather").asText("--"),
                live.path("temperature").asText("--"),
                live.path("humidity").asText("--"),
                "--",
                live.path("reporttime").asText(""));
    }

    private JsonNode requestAmapWeatherLive(String adcode) throws Exception {
        String url = AMAP_WEATHER_URL
                + "?key=" + encode(weatherProperties.getAmapWebServiceKey())
                + "&city=" + encode(adcode)
                + "&extensions=base";
        JsonNode root = requestJson(url);
        assertAmapSuccess(root, "高德天气查询失败");
        return root.path("lives").path(0);
    }

    private ClientWeatherResponse requestOpenMeteoWeather(LocationInfo locationInfo, Double longitude, Double latitude) throws Exception {
        if (!weatherProperties.isOpenMeteoFallbackEnabled()) {
            return buildFallbackResponse(locationInfo);
        }
        String url = OPEN_METEO_WEATHER_URL
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=temperature_2m,relative_humidity_2m,weather_code"
                + "&timezone=auto";
        JsonNode current = requestJson(url).path("current");
        // Open-Meteo 按经纬度返回当前天气，不依赖高德 Web 服务 Key。
        return new ClientWeatherResponse(
                firstNonBlank(locationInfo.location(), weatherProperties.getDefaultCity()),
                firstNonBlank(locationInfo.adcode(), weatherProperties.getDefaultAdcode()),
                weatherName(current.path("weather_code").asInt(-1)),
                formatNumber(current.path("temperature_2m")),
                formatNumber(current.path("relative_humidity_2m")),
                "--",
                current.path("time").asText(""));
    }

    private String requestPm25(Double longitude, Double latitude) {
        if (!StringUtils.hasText(weatherProperties.getWaqiToken())) {
            return requestOpenMeteoPm25(longitude, latitude);
        }
        try {
            String url = String.format(WAQI_GEO_URL, latitude, longitude)
                    + "?token=" + encode(weatherProperties.getWaqiToken());
            JsonNode root = requestJson(url);
            if (!"ok".equalsIgnoreCase(root.path("status").asText())) {
                return "--";
            }
            JsonNode pm25 = root.path("data").path("iaqi").path("pm25").path("v");
            // WAQI 有时只返回 AQI 没有 PM2.5，此时继续使用 Open-Meteo 空气质量接口兜底。
            return pm25.isNumber() ? pm25.asText() : requestOpenMeteoPm25(longitude, latitude);
        } catch (Exception exception) {
            return requestOpenMeteoPm25(longitude, latitude);
        }
    }

    private String requestOpenMeteoPm25(Double longitude, Double latitude) {
        if (!weatherProperties.isOpenMeteoFallbackEnabled()) {
            return "--";
        }
        try {
            String url = OPEN_METEO_AIR_URL
                    + "?latitude=" + latitude
                    + "&longitude=" + longitude
                    + "&current=pm2_5"
                    + "&timezone=auto";
            JsonNode pm25 = requestJson(url).path("current").path("pm2_5");
            // Open-Meteo 空气质量接口不需要 Token，可作为 PM2.5 的默认数据来源。
            return formatNumber(pm25);
        } catch (Exception exception) {
            return "--";
        }
    }

    private JsonNode requestJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(Math.max(1, weatherProperties.getTimeoutSeconds())))
                .header("User-Agent", "smart-plant/1.0")
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException(ResponseCode.SERVICE_UNAVAILABLE, "天气服务暂不可用");
        }
        return objectMapper.readTree(response.body());
    }

    private void assertAmapSuccess(JsonNode root, String message) {
        if (!"1".equals(root.path("status").asText())) {
            throw new BusinessException(ResponseCode.SERVICE_UNAVAILABLE, message);
        }
    }

    private void validateCoordinate(Double longitude, Double latitude) {
        if (longitude == null || latitude == null
                || longitude < -180 || longitude > 180
                || latitude < -90 || latitude > 90) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "经纬度参数不正确");
        }
    }

    private ClientWeatherResponse buildFallbackResponse() {
        return new ClientWeatherResponse(
                weatherProperties.getDefaultCity(),
                weatherProperties.getDefaultAdcode(),
                "--",
                "--",
                "--",
                "--",
                "");
    }

    private ClientWeatherResponse buildFallbackResponse(LocationInfo locationInfo) {
        return new ClientWeatherResponse(
                firstNonBlank(locationInfo.location(), weatherProperties.getDefaultCity()),
                firstNonBlank(locationInfo.adcode(), weatherProperties.getDefaultAdcode()),
                "--",
                "--",
                "--",
                "--",
                "");
    }

    private String normalizeCoordinateSystem(String coordinateSystem) {
        if (!StringUtils.hasText(coordinateSystem)) {
            return "wgs84";
        }
        String value = coordinateSystem.trim().toLowerCase(Locale.ROOT);
        return "gcj02".equals(value) ? "gcj02" : "wgs84";
    }

    private Coordinate toAmapCoordinate(Double longitude, Double latitude, String coordinateSystem) {
        if ("gcj02".equals(coordinateSystem) || isOutOfChina(longitude, latitude)) {
            return new Coordinate(longitude, latitude);
        }
        return wgs84ToGcj02(longitude, latitude);
    }

    private Coordinate wgs84ToGcj02(Double longitude, Double latitude) {
        double a = 6378245.0;
        double ee = 0.00669342162296594323;
        double dLat = transformLat(longitude - 105.0, latitude - 35.0);
        double dLng = transformLng(longitude - 105.0, latitude - 35.0);
        double radLat = latitude / 180.0 * Math.PI;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * Math.PI);
        dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * Math.PI);
        // 高德 Web 服务使用 gcj02 坐标，WGS84 定位入参需要先转换后再做逆地理编码。
        return new Coordinate(longitude + dLng, latitude + dLat);
    }

    private boolean isOutOfChina(Double longitude, Double latitude) {
        return longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271;
    }

    private double transformLat(double x, double y) {
        double result = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        result += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        result += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
        result += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
        return result;
    }

    private double transformLng(double x, double y) {
        double result = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        result += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        result += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
        result += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
        return result;
    }

    private String buildCacheKey(Double longitude, Double latitude, String coordinateSystem) {
        // 经纬度保留两位约等于公里级缓存，适合城市天气展示并能显著减少第三方调用。
        return String.format(Locale.ROOT, "%s:%.2f,%.2f", coordinateSystem, longitude, latitude);
    }

    private String firstText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || node.isArray()) {
            return "";
        }
        return node.asText("");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String formatNumber(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isNumber()) {
            return "--";
        }
        double value = node.asDouble();
        if (Math.abs(value - Math.rint(value)) < 0.000001) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    static String weatherName(int code) {
        return switch (code) {
            case 0 -> "晴";
            case 1 -> "晴间多云";
            case 2 -> "多云";
            case 3 -> "阴";
            case 45, 48 -> "雾";
            case 51, 61 -> "小雨";
            case 53, 63 -> "中雨";
            case 55, 65 -> "大雨";
            case 56, 57, 66, 67 -> "冻雨";
            case 71, 77 -> "小雪";
            case 73 -> "中雪";
            case 75, 86 -> "大雪";
            case 80, 81 -> "阵雨";
            case 82 -> "暴雨";
            case 85 -> "阵雪";
            case 95 -> "雷阵雨";
            case 96, 99 -> "雷阵雨伴冰雹";
            default -> "未知";
        };
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private record LocationInfo(String adcode, String location) {
    }

    private record Coordinate(Double longitude, Double latitude) {
    }

    private record CacheEntry(ClientWeatherResponse response, Instant createdAt) {

        private boolean isValid(int cacheMinutes) {
            return Duration.between(createdAt, Instant.now()).toMinutes() < Math.max(1, cacheMinutes);
        }
    }
}
