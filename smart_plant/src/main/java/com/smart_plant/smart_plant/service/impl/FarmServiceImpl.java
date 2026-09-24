package com.smart_plant.smart_plant.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.config.WeatherProperties;
import com.smart_plant.smart_plant.dto.ClientFarmListResponse;
import com.smart_plant.smart_plant.dto.FarmAddressResolveResult;
import com.smart_plant.smart_plant.dto.FarmImageUploadResult;
import com.smart_plant.smart_plant.entity.Farm;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.FarmMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.FarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FarmServiceImpl implements FarmService {

    private static final String FARM_OWNER_ROLE_CODE = "farm_owner";

    private static final String DEFAULT_AREA_UNIT = "亩";

    private static final String FARM_CODE_PREFIX = "NC";

    private static final int FARM_CODE_MIN = 1;

    private static final int FARM_CODE_MAX = 999;

    private static final int FARM_CODE_RANDOM_ATTEMPTS = 1000;

    private static final long MAX_FARM_IMAGE_UPLOAD_BYTES = 5L * 1024L * 1024L;

    private static final String UPLOAD_ROOT = "uploads";

    private static final String FARM_IMAGE_UPLOAD_DIR = "farm-images";

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private static final String AMAP_RE_GEO_URL = "https://restapi.amap.com/v3/geocode/regeo";

    private static final String NOMINATIM_REVERSE_URL = "https://nominatim.openstreetmap.org/reverse";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final FarmMapper farmMapper;

    private final UserMapper userMapper;

    private final DataPermissionService dataPermissionService;

    private final WeatherProperties weatherProperties;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Farm addFarm(Farm farm) {
        if (!dataPermissionService.isAdmin() && farm != null) {
            farm.setUserId(dataPermissionService.currentUser().getId());
        }
        normalizeDefaults(farm);
        synchronizeCoordinate(farm);
        validateCreateFarm(farm);
        dataPermissionService.requireFarmManager(farm.getUserId());
        checkFarmOwnerUser(farm.getUserId());
        farm.setFarmCode(generateUniqueFarmCode());
        farmMapper.insert(farm);
        return farmMapper.selectById(farm.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Farm updateFarm(Farm farm) {
        if (farm == null || farm.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农场ID不能为空");
        }
        Farm oldFarm = farmMapper.selectById(farm.getId());
        if (oldFarm == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "农场不存在");
        }
        dataPermissionService.requireFarmManager(oldFarm.getUserId());
        if (!dataPermissionService.isAdmin()) {
            farm.setUserId(oldFarm.getUserId());
        }
        if (farm.getUserId() != null) {
            checkFarmOwnerUser(farm.getUserId());
            if (!farm.getUserId().equals(oldFarm.getUserId()) && farmMapper.countPlotByFarmId(farm.getId()) > 0) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "农场已关联地块，不能修改所属用户");
            }
        } else {
            farm.setUserId(oldFarm.getUserId());
        }
        normalizeDefaults(farm);
        synchronizeCoordinate(farm);
        validateUpdateFarm(farm);
        int rows = farmMapper.updateById(farm);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "农场修改失败");
        }
        return farmMapper.selectById(farm.getId());
    }

    @Override
    public FarmImageUploadResult uploadFarmImage(MultipartFile file) {
        validateFarmImage(file);

        String extension = resolveSafeExtension(file.getOriginalFilename(), file.getContentType());
        String filename = UUID.randomUUID() + "." + extension;
        String datePath = LocalDate.now().toString();
        Path uploadRoot = Paths.get(System.getProperty("user.dir"))
                .resolve(UPLOAD_ROOT)
                .toAbsolutePath()
                .normalize();
        Path targetDirectory = uploadRoot.resolve(FARM_IMAGE_UPLOAD_DIR).resolve(datePath).normalize();
        Path targetFile = targetDirectory.resolve(filename).normalize();

        if (!targetFile.startsWith(uploadRoot)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农场图片文件路径不合法");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.createDirectories(targetDirectory);
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store farm image at {}", targetFile, e);
            throw new BusinessException(ResponseCode.FAIL, "农场图片上传失败");
        }

        String url = "/" + UPLOAD_ROOT + "/" + FARM_IMAGE_UPLOAD_DIR + "/" + datePath + "/" + filename;
        return new FarmImageUploadResult(url, file.getSize());
    }

    @Override
    public FarmAddressResolveResult resolveAddressByCoordinate(String coordinate, String coordinateSystem) {
        Coordinate parsedCoordinate = parseCoordinate(coordinate);
        validateCoordinate(parsedCoordinate.longitude(), parsedCoordinate.latitude());
        String normalizedCoordinate = formatCoordinate(
                BigDecimal.valueOf(parsedCoordinate.longitude()),
                BigDecimal.valueOf(parsedCoordinate.latitude()));

        String address = firstNonBlank(
                requestAmapAddress(parsedCoordinate, coordinateSystem),
                requestNominatimAddress(parsedCoordinate));

        if (!StringUtils.hasText(address)) {
            throw new BusinessException(ResponseCode.SERVICE_UNAVAILABLE, "暂未获取到当前位置地址");
        }

        return new FarmAddressResolveResult(normalizedCoordinate, address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFarm(Long id) {
        requireId(id);
        dataPermissionService.requireFarmManager(getFarmById(id).getUserId());
        checkNoPlot(id);
        int rows = farmMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "农场不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteFarms(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的农场");
        }
        for (Long id : ids) {
            requireId(id);
            dataPermissionService.requireFarmManager(getFarmById(id).getUserId());
        }
        if (farmMapper.countPlotByFarmIds(ids) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "选中的农场已关联地块，不能删除");
        }
        return farmMapper.deleteBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireId(id);
        dataPermissionService.requireFarmManager(getFarmById(id).getUserId());
        validateStatus(status);
        int rows = farmMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "农场不存在");
        }
    }

    @Override
    public Farm getFarmById(Long id) {
        requireId(id);
        Farm farm = farmMapper.selectById(id);
        if (farm == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "农场不存在");
        }
        dataPermissionService.requireOwnedResource(farm.getUserId());
        return farm;
    }

    @Override
    public PageInfo<Farm> listFarms(String farmName, String farmCode, Long userId, Integer status,
                                    Integer pageNum, Integer pageSize) {
        validateStatus(status);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = dataPermissionService.restrictUserId(userId);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(farmMapper.selectList(farmName, farmCode, scopedUserId, status));
    }

    @Override
    public Map<String, Object> statisticsFarms() {
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        Map<String, Object> statistics = farmMapper.selectStatistics(scopedUserId);
        return statistics == null ? new HashMap<>() : statistics;
    }

    @Override
    public List<ClientFarmListResponse> listCurrentClientFarms() {
        // 普通用户读取注册时绑定的农场主名下农场，农场主仍读取本人农场。
        Long currentUserId = dataPermissionService.currentClientOwnerId();
        List<ClientFarmListResponse> farms = farmMapper.selectClientFarmListByUserId(currentUserId);
        farms.forEach(this::fillClientFarmDisplayFields);
        return farms;
    }

    private void validateCreateFarm(Farm farm) {
        if (farm == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农场信息不能为空");
        }
        if (farm.getUserId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属用户ID不能为空");
        }
        if (!StringUtils.hasText(farm.getFarmName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农场名称不能为空");
        }
        validateUpdateFarm(farm);
    }

    private void validateUpdateFarm(Farm farm) {
        validateStatus(farm.getStatus());
        validateNonNegative(farm.getTotalArea(), "农场面积不能小于0");
        validateLongitude(farm.getLongitude());
        validateLatitude(farm.getLatitude());
    }

    private void normalizeDefaults(Farm farm) {
        if (farm == null) {
            return;
        }
        if (StringUtils.hasText(farm.getFarmName())) {
            farm.setFarmName(farm.getFarmName().trim());
        }
        if (StringUtils.hasText(farm.getImgUrl())) {
            farm.setImgUrl(farm.getImgUrl().trim());
        }
        if (StringUtils.hasText(farm.getContactPhone())) {
            farm.setContactPhone(farm.getContactPhone().trim());
        }
        if (StringUtils.hasText(farm.getAddress())) {
            farm.setAddress(farm.getAddress().trim());
        }
        if (!StringUtils.hasText(farm.getAreaUnit())) {
            farm.setAreaUnit(DEFAULT_AREA_UNIT);
        }
        if (farm.getStatus() == null) {
            farm.setStatus(1);
        }
    }

    private void synchronizeCoordinate(Farm farm) {
        if (farm == null) {
            return;
        }
        if (StringUtils.hasText(farm.getCoordinate())) {
            String normalized = farm.getCoordinate().trim().replace('，', ',');
            String[] parts = normalized.split(",");
            if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "经纬度格式必须为“经度,纬度”");
            }
            try {
                BigDecimal longitude = new BigDecimal(parts[0].trim());
                BigDecimal latitude = new BigDecimal(parts[1].trim());
                farm.setLongitude(longitude);
                farm.setLatitude(latitude);
                farm.setCoordinate(formatCoordinate(longitude, latitude));
            } catch (NumberFormatException e) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "经纬度必须为数字");
            }
            return;
        }

        if (farm.getLongitude() != null && farm.getLatitude() != null) {
            farm.setCoordinate(formatCoordinate(farm.getLongitude(), farm.getLatitude()));
        }
    }

    private Coordinate parseCoordinate(String coordinate) {
        if (!StringUtils.hasText(coordinate)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "经纬度不能为空");
        }
        String normalized = coordinate.trim().replace('，', ',');
        String[] parts = normalized.split(",");
        if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "经纬度格式必须为“经度,纬度”");
        }
        try {
            return new Coordinate(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
        } catch (NumberFormatException e) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "经纬度必须为数字");
        }
    }

    private String formatCoordinate(BigDecimal longitude, BigDecimal latitude) {
        return longitude.stripTrailingZeros().toPlainString() + "," + latitude.stripTrailingZeros().toPlainString();
    }

    private void fillClientFarmDisplayFields(ClientFarmListResponse farm) {
        String unit = StringUtils.hasText(farm.getAreaUnit()) ? farm.getAreaUnit() : DEFAULT_AREA_UNIT;
        if (farm.getTotalArea() == null) {
            farm.setPlantArea("-");
            return;
        }
        farm.setPlantArea(farm.getTotalArea().stripTrailingZeros().toPlainString() + unit);
    }

    private void checkFarmOwnerUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "所属用户不存在");
        }
        if (!FARM_OWNER_ROLE_CODE.equalsIgnoreCase(user.getRoleCode())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属用户必须为农场主");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属农场主账号已禁用");
        }
    }

    private String generateUniqueFarmCode() {
        for (int i = 0; i < FARM_CODE_RANDOM_ATTEMPTS; i++) {
            String candidate = formatFarmCode(randomFarmCodeNumber());
            if (farmMapper.countByFarmCode(candidate, null) == 0) {
                return candidate;
            }
        }

        for (int value = FARM_CODE_MIN; value <= FARM_CODE_MAX; value++) {
            String candidate = formatFarmCode(value);
            if (farmMapper.countByFarmCode(candidate, null) == 0) {
                return candidate;
            }
        }

        throw new BusinessException(ResponseCode.FAIL, "农场编号已用尽，请联系管理员扩展编号规则");
    }

    private int randomFarmCodeNumber() {
        return SECURE_RANDOM.nextInt(FARM_CODE_MAX - FARM_CODE_MIN + 1) + FARM_CODE_MIN;
    }

    private String formatFarmCode(int value) {
        return String.format(Locale.ROOT, "%s%03d", FARM_CODE_PREFIX, value);
    }

    private void validateFarmImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择农场图片");
        }
        if (file.getSize() > MAX_FARM_IMAGE_UPLOAD_BYTES) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农场图片不能超过5MB");
        }
        String contentType = file.getContentType();
        String extension = resolveSafeExtension(file.getOriginalFilename(), contentType);
        boolean allowedContentType = StringUtils.hasText(contentType)
                && ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
        boolean allowedExtension = ALLOWED_IMAGE_EXTENSIONS.contains(extension);
        if (!allowedContentType && !allowedExtension) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农场图片仅支持 JPG、PNG、WEBP 格式");
        }
    }

    private String resolveSafeExtension(String originalFilename, String contentType) {
        String extension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.lastIndexOf('.') >= 0) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            String normalizedContentType = StringUtils.hasText(contentType) ? contentType.toLowerCase(Locale.ROOT) : "";
            extension = switch (normalizedContentType) {
                case "image/jpeg", "image/jpg" -> "jpg";
                case "image/png", "image/x-png" -> "png";
                case "image/webp" -> "webp";
                default -> "";
            };
        }
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农场图片文件扩展名不合法");
        }
        return extension;
    }

    private String requestAmapAddress(Coordinate coordinate, String coordinateSystem) {
        if (!StringUtils.hasText(weatherProperties.getAmapWebServiceKey())) {
            return "";
        }
        try {
            Coordinate amapCoordinate = toAmapCoordinate(
                    coordinate.longitude(),
                    coordinate.latitude(),
                    normalizeCoordinateSystem(coordinateSystem));
            String url = AMAP_RE_GEO_URL
                    + "?key=" + encode(weatherProperties.getAmapWebServiceKey())
                    + "&location=" + amapCoordinate.longitude() + "," + amapCoordinate.latitude()
                    + "&extensions=base";
            JsonNode root = requestJson(url);
            if (!"1".equals(root.path("status").asText())) {
                return "";
            }
            return root.path("regeocode").path("formatted_address").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    private String requestNominatimAddress(Coordinate coordinate) {
        try {
            String url = NOMINATIM_REVERSE_URL
                    + "?format=jsonv2"
                    + "&lat=" + coordinate.latitude()
                    + "&lon=" + coordinate.longitude()
                    + "&accept-language=" + encode("zh-CN");
            JsonNode root = requestJson(url);
            return root.path("display_name").asText("");
        } catch (Exception e) {
            return "";
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
            throw new BusinessException(ResponseCode.SERVICE_UNAVAILABLE, "地址解析服务暂不可用");
        }
        return objectMapper.readTree(response.body());
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

    private void validateCoordinate(Double longitude, Double latitude) {
        if (longitude == null || latitude == null
                || longitude < -180 || longitude > 180
                || latitude < -90 || latitude > 90) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "经纬度参数不正确");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private void checkNoPlot(Long farmId) {
        if (farmMapper.countPlotByFarmId(farmId) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农场已关联地块，不能删除");
        }
    }

    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农场状态只能为0或1");
        }
    }

    private void validateNonNegative(BigDecimal value, String message) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
    }

    private void validateLongitude(BigDecimal longitude) {
        if (longitude != null
                && (longitude.compareTo(new BigDecimal("-180")) < 0
                || longitude.compareTo(new BigDecimal("180")) > 0)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "经度范围必须在-180到180之间");
        }
    }

    private void validateLatitude(BigDecimal latitude) {
        if (latitude != null
                && (latitude.compareTo(new BigDecimal("-90")) < 0
                || latitude.compareTo(new BigDecimal("90")) > 0)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "纬度范围必须在-90到90之间");
        }
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农场ID不能为空");
        }
    }

    private record Coordinate(Double longitude, Double latitude) {
    }
}
