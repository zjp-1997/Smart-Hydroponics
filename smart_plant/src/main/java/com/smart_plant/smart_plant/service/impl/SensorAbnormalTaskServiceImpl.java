package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.EnvironmentData;
import com.smart_plant.smart_plant.entity.FarmTask;
import com.smart_plant.smart_plant.entity.GrowthStage;
import com.smart_plant.smart_plant.entity.IotDevice;
import com.smart_plant.smart_plant.entity.LightData;
import com.smart_plant.smart_plant.entity.Notification;
import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.entity.PumpData;
import com.smart_plant.smart_plant.entity.WaterQualityData;
import com.smart_plant.smart_plant.mapper.FarmTaskMapper;
import com.smart_plant.smart_plant.mapper.GrowthStageMapper;
import com.smart_plant.smart_plant.mapper.IotDeviceMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.service.NotificationService;
import com.smart_plant.smart_plant.service.SensorAbnormalTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 采集数据异常判定与农事任务联动实现。
 *
 * <p>异常判定以设备所在地块当前种植批次的 growth_stage 为主：环境使用温湿度范围，
 * 水质使用水温、PH、EC 范围，补光灯使用光照时长折算的参考照度，水泵使用浇水间隔折算的运行范围。</p>
 */
@Service
@RequiredArgsConstructor
public class SensorAbnormalTaskServiceImpl implements SensorAbnormalTaskService {

    private static final int DATA_STATUS_NORMAL = 1;
    private static final int DATA_STATUS_ABNORMAL = 2;
    private static final int TASK_STATUS_PENDING = 1;
    private static final int TASK_TYPE_WATER = 1;
    private static final int TASK_TYPE_INSPECTION = 5;
    private static final int TASK_TYPE_LIGHT = 7;
    private static final int PRIORITY_HIGH = 3;
    private static final int PRIORITY_URGENT = 4;
    private static final int NOTICE_TYPE_FARM_TASK = 2;
    private static final String REF_TYPE_FARM_TASK_MESSAGE = "farm_task_message";
    private static final String SOURCE_ENVIRONMENT = "SENSOR_ABNORMAL_ENVIRONMENT";
    private static final String SOURCE_WATER_QUALITY = "SENSOR_ABNORMAL_WATER_QUALITY";
    private static final String SOURCE_LIGHT = "SENSOR_ABNORMAL_LIGHT";
    private static final String SOURCE_PUMP = "SENSOR_ABNORMAL_PUMP";
    private static final Pattern ABNORMAL_METRIC_PATTERN =
            Pattern.compile("^(.+?)(偏低|偏高|过低|过高|超标|异常)(?:[：:].*)?$");

    private final IotDeviceMapper iotDeviceMapper;
    private final PlotMapper plotMapper;
    private final GrowthStageMapper growthStageMapper;
    private final FarmTaskMapper farmTaskMapper;
    private final NotificationService notificationService;

    @Override
    public void prepareEnvironmentData(EnvironmentData data) {
        if (data == null) {
            return;
        }
        GrowthContext context = resolveGrowthContext(data == null ? null : data.getDeviceId());
        List<String> abnormalities = new ArrayList<>();
        GrowthStage stage = context.stage();
        if (stage != null) {
            compareRange(abnormalities, "空气温度", data.getAirTemperature(), stage.getTempMin(), stage.getTempMax(), "℃");
            compareRange(abnormalities, "空气湿度", data.getAirHumidity(), stage.getHumidityMin(), stage.getHumidityMax(), "%");
        }
        applyStatus(data, abnormalities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createEnvironmentTaskIfAbnormal(EnvironmentData data) {
        createTaskIfAbnormal(data == null ? null : data.getDeviceId(), data == null ? null : data.getId(),
                data == null ? null : data.getAbnormalDetail(), data == null ? null : data.getCollectTime(),
                SOURCE_ENVIRONMENT, "环境数据异常处理", TASK_TYPE_INSPECTION, PRIORITY_URGENT);
    }

    @Override
    public void prepareWaterQualityData(WaterQualityData data) {
        if (data == null) {
            return;
        }
        GrowthContext context = resolveGrowthContext(data == null ? null : data.getDeviceId());
        List<String> abnormalities = new ArrayList<>();
        GrowthStage stage = context.stage();
        if (stage != null) {
            compareRange(abnormalities, "水温", data.getWaterTemperature(), stage.getTempMin(), stage.getTempMax(), "℃");
            compareRange(abnormalities, "PH值", data.getPh(), stage.getPhMin(), stage.getPhMax(), "");
            compareRange(abnormalities, "EC值", data.getEcValue(), stage.getEcMin(), stage.getEcMax(), "mS/cm");
        }
        applyStatus(data, abnormalities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createWaterQualityTaskIfAbnormal(WaterQualityData data) {
        createTaskIfAbnormal(data == null ? null : data.getDeviceId(), data == null ? null : data.getId(),
                data == null ? null : data.getAbnormalDetail(), data == null ? null : data.getCollectTime(),
                SOURCE_WATER_QUALITY, "水质数据异常处理", TASK_TYPE_INSPECTION, PRIORITY_URGENT);
    }

    @Override
    public void prepareLightData(LightData data) {
        if (data == null) {
            return;
        }
        GrowthContext context = resolveGrowthContext(data == null ? null : data.getDeviceId());
        List<String> abnormalities = new ArrayList<>();
        GrowthStage stage = context.stage();
        if (stage != null && stage.getLightHours() != null) {
            BigDecimal min = stage.getLightHours().multiply(new BigDecimal("1200"));
            BigDecimal max = stage.getLightHours().multiply(new BigDecimal("3000"));
            compareRange(abnormalities, "光照强度", data.getLightIntensity(), min, max, "lux");
        }
        applyStatus(data, abnormalities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createLightTaskIfAbnormal(LightData data) {
        createTaskIfAbnormal(data == null ? null : data.getDeviceId(), data == null ? null : data.getId(),
                data == null ? null : data.getAbnormalDetail(), data == null ? null : data.getCollectTime(),
                SOURCE_LIGHT, "补光灯数据异常处理", TASK_TYPE_LIGHT, PRIORITY_HIGH);
    }

    @Override
    public void preparePumpData(PumpData data) {
        if (data == null) {
            return;
        }
        GrowthContext context = resolveGrowthContext(data == null ? null : data.getDeviceId());
        List<String> abnormalities = new ArrayList<>();
        GrowthStage stage = context.stage();
        if (stage != null) {
            PumpRange range = pumpRange(stage.getWaterIntervalDays());
            compareRange(abnormalities, "水流量", data.getWaterFlow(), range.flowMin(), range.flowMax(), "m³/h");
            compareRange(abnormalities, "水压", data.getWaterPressure(), range.pressureMin(), range.pressureMax(), "MPa");
        }
        applyStatus(data, abnormalities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPumpTaskIfAbnormal(PumpData data) {
        createTaskIfAbnormal(data == null ? null : data.getDeviceId(), data == null ? null : data.getId(),
                data == null ? null : data.getAbnormalDetail(), data == null ? null : data.getCollectTime(),
                SOURCE_PUMP, "水泵数据异常处理", TASK_TYPE_WATER, PRIORITY_URGENT);
    }

    private void createTaskIfAbnormal(Long deviceId, Long sourceId, String abnormalDetail, LocalDateTime collectTime,
                                      String sourceType, String title, Integer taskType, Integer priority) {
        if (deviceId == null || sourceId == null || !StringUtils.hasText(abnormalDetail)) {
            return;
        }
        GrowthContext context = resolveGrowthContext(deviceId);
        IotDevice device = context.device();
        Plot plot = context.plot();
        FarmTask existingTask = farmTaskMapper.selectBySource(sourceType, sourceId);
        if (existingTask != null) {
            ensureFarmTaskMessage(existingTask, device, plot, abnormalDetail);
            return;
        }
        if (device == null) {
            return;
        }
        FarmTask task = new FarmTask();
        task.setUserId(firstNonNull(device.getUserId(), plot == null ? null : plot.getUserId()));
        task.setPlotId(device.getPlotId());
        task.setBatchId(plot == null ? null : plot.getCurrentBatchId());
        task.setTaskTitle(limit(buildTaskTitle(title, plot, context.stage()), 120));
        task.setTaskType(taskType);
        task.setTaskContent(buildTaskContent(device, plot, context.stage(), abnormalDetail, collectTime));
        task.setPriority(priority);
        // 传感器异常任务按优先级计算截至时间，紧急任务需要在 1 天内处理。
        task.setDeadlineTime(LocalDateTime.now().plusDays(priority != null && priority >= PRIORITY_URGENT ? 1 : 3));
        task.setStatus(TASK_STATUS_PENDING);
        task.setExecutorId(task.getUserId());
        task.setRemark("采集数据异常自动生成");
        task.setSourceType(sourceType);
        task.setSourceId(sourceId);
        farmTaskMapper.insert(task);
        ensureFarmTaskMessage(task, device, plot, abnormalDetail);
    }

    private void ensureFarmTaskMessage(FarmTask task, IotDevice device, Plot plot, String abnormalDetail) {
        if (task == null || task.getId() == null || task.getUserId() == null) {
            return;
        }
        for (Long receiverId : resolveFarmTaskReceivers(task)) {
            Notification notification = new Notification();
            notification.setUserId(receiverId);
            notification.setTitle(limit(buildAbnormalMessageTitle(device, task), 100));
            notification.setContent(buildAbnormalMessageContent(device, plot, abnormalDetail));
            notification.setNoticeType(NOTICE_TYPE_FARM_TASK);
            notification.setRefType(REF_TYPE_FARM_TASK_MESSAGE);
            notification.setRefId(task.getId());
            notification.setTaskId(task.getId());
            notification.setLevel(priorityToNoticeLevel(task.getPriority()));
            notification.setIsRead(0);
            notification.setSendTime(LocalDateTime.now());
            notification.setPublisherId(task.getUserId());
            notification.setPublisherName("系统自动生成");
            notification.setStatus(1);
            notification.setRemark("农事任务产生后自动生成");
            notificationService.createIfAbsent(notification);
        }
    }

    private List<Long> resolveFarmTaskReceivers(FarmTask task) {
        List<Long> receiverIds = new ArrayList<>();
        if (task.getExecutorId() != null) {
            receiverIds.add(task.getExecutorId());
        }
        if (task.getUserId() != null && !receiverIds.contains(task.getUserId())) {
            receiverIds.add(task.getUserId());
        }
        return receiverIds;
    }

    /**
     * 消息标题只表达发生异常的具体设备，不再复用内部农事任务标题。
     */
    private String buildAbnormalMessageTitle(IotDevice device, FarmTask task) {
        String deviceName = firstNonBlank(device == null ? null : device.getName(), task.getTaskTitle(), "设备");
        // 数据库演示名称形如“水培_02号-水质检测仪”，标题按产品文案压缩为“水培_02水质检测仪”。
        return deviceName.replaceFirst("号\\s*[-—–]\\s*", "") + "数据异常";
    }

    /**
     * 每个采集来源独立生成一条设备级消息，正文只保留地块、设备和本次异常指标。
     */
    private String buildAbnormalMessageContent(IotDevice device, Plot plot, String abnormalDetail) {
        String plotName = firstNonBlank(plot == null ? null : plot.getPlotName(), "未知地块");
        String plotDisplayName = plotName.endsWith("地块") ? plotName : plotName + "地块";
        String deviceName = firstNonBlank(device == null ? null : device.getName(), "未知设备")
                .replaceAll("\\s*[-—–]\\s*", "");
        return plotDisplayName + "的" + deviceName + "的" + summarizeAbnormalDetail(abnormalDetail) + "，请及时处理。";
    }

    private String summarizeAbnormalDetail(String abnormalDetail) {
        if (!StringUtils.hasText(abnormalDetail)) {
            return "采集数据异常";
        }
        Map<String, List<String>> metricsByState = new LinkedHashMap<>();
        List<String> unmatched = new ArrayList<>();
        for (String item : abnormalDetail.split("[；;]")) {
            String detail = item.trim();
            if (!StringUtils.hasText(detail)) {
                continue;
            }
            Matcher matcher = ABNORMAL_METRIC_PATTERN.matcher(detail);
            if (matcher.matches()) {
                metricsByState.computeIfAbsent(matcher.group(2), ignored -> new ArrayList<>())
                        .add(matcher.group(1).trim());
            } else {
                unmatched.add(detail.replaceFirst("[：:].*$", ""));
            }
        }
        List<String> summaries = new ArrayList<>();
        metricsByState.forEach((state, metrics) -> summaries.add(joinChinese(metrics) + state));
        summaries.addAll(unmatched);
        return summaries.isEmpty() ? "采集数据异常" : String.join("，", summaries);
    }

    private String joinChinese(List<String> values) {
        if (values.size() <= 1) {
            return values.isEmpty() ? "数据" : values.getFirst();
        }
        return String.join("、", values.subList(0, values.size() - 1)) + "和" + values.getLast();
    }

    private Integer priorityToNoticeLevel(Integer priority) {
        if (priority == null) {
            return 1;
        }
        if (priority >= PRIORITY_URGENT) {
            return 3;
        }
        if (priority >= PRIORITY_HIGH) {
            return 2;
        }
        return 1;
    }

    private GrowthContext resolveGrowthContext(Long deviceId) {
        if (deviceId == null) {
            return new GrowthContext(null, null, null);
        }
        IotDevice device = iotDeviceMapper.selectById(deviceId);
        if (device == null || device.getPlotId() == null) {
            return new GrowthContext(device, null, null);
        }
        Plot plot = plotMapper.selectById(device.getPlotId());
        GrowthStage stage = plot == null || plot.getCurrentGrowthStageId() == null
                ? null
                : growthStageMapper.selectById(plot.getCurrentGrowthStageId());
        return new GrowthContext(device, plot, stage);
    }

    private void compareRange(List<String> abnormalities, String metricName, BigDecimal value,
                              BigDecimal min, BigDecimal max, String unit) {
        if (value == null || (min == null && max == null)) {
            return;
        }
        if (min != null && value.compareTo(min) < 0) {
            abnormalities.add(metricName + "偏低：" + format(value) + unit + "，低于适宜下限" + format(min) + unit);
        }
        if (max != null && value.compareTo(max) > 0) {
            abnormalities.add(metricName + "偏高：" + format(value) + unit + "，高于适宜上限" + format(max) + unit);
        }
    }

    private void applyStatus(EnvironmentData data, List<String> abnormalities) {
        if (data == null) {
            return;
        }
        data.setDataStatus(abnormalities.isEmpty() ? DATA_STATUS_NORMAL : DATA_STATUS_ABNORMAL);
        data.setAbnormalDetail(abnormalities.isEmpty() ? null : String.join("；", abnormalities));
    }

    private void applyStatus(WaterQualityData data, List<String> abnormalities) {
        if (data == null) {
            return;
        }
        data.setDataStatus(abnormalities.isEmpty() ? DATA_STATUS_NORMAL : DATA_STATUS_ABNORMAL);
        data.setAbnormalDetail(abnormalities.isEmpty() ? null : String.join("；", abnormalities));
    }

    private void applyStatus(LightData data, List<String> abnormalities) {
        if (data == null) {
            return;
        }
        data.setDataStatus(abnormalities.isEmpty() ? DATA_STATUS_NORMAL : DATA_STATUS_ABNORMAL);
        data.setAbnormalDetail(abnormalities.isEmpty() ? null : String.join("；", abnormalities));
    }

    private void applyStatus(PumpData data, List<String> abnormalities) {
        if (data == null) {
            return;
        }
        data.setDataStatus(abnormalities.isEmpty() ? DATA_STATUS_NORMAL : DATA_STATUS_ABNORMAL);
        data.setAbnormalDetail(abnormalities.isEmpty() ? null : String.join("；", abnormalities));
    }

    private PumpRange pumpRange(Integer waterIntervalDays) {
        int interval = waterIntervalDays == null || waterIntervalDays < 1 ? 3 : waterIntervalDays;
        BigDecimal flowMin = interval <= 2 ? new BigDecimal("1.20") : new BigDecimal("0.80");
        BigDecimal flowMax = interval <= 2 ? new BigDecimal("3.50") : new BigDecimal("2.60");
        return new PumpRange(flowMin, flowMax, new BigDecimal("0.12"), new BigDecimal("0.45"));
    }

    private String buildTaskTitle(String prefix, Plot plot, GrowthStage stage) {
        String plotName = plot == null ? "未知地块" : plot.getPlotName();
        String stageName = stage == null ? "未知生长期" : stage.getStageName();
        return prefix + " - " + plotName + "（" + stageName + "）";
    }

    private String buildTaskContent(IotDevice device, Plot plot, GrowthStage stage, String abnormalDetail, LocalDateTime collectTime) {
        StringBuilder content = new StringBuilder();
        appendLine(content, "设备", firstNonBlank(device == null ? null : device.getName(), "未知设备")
                + "（" + firstNonBlank(device == null ? null : device.getDeviceCode(), "无编码") + "）");
        appendLine(content, "地块", plot == null ? "未知地块" : firstNonBlank(plot.getPlotName(), plot.getPlotCode()));
        appendLine(content, "作物生长期", stage == null ? "未匹配到当前生长期" : stage.getStageName());
        appendLine(content, "异常详情", abnormalDetail);
        appendLine(content, "采集时间", collectTime == null ? null : collectTime.toString());
        appendLine(content, "处理建议", "请现场复核设备读数与作物状态，按异常指标调整环境、水质或灌溉设备。");
        return content.toString().trim();
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(label).append("：").append(value.trim()).append(System.lineSeparator());
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

    private Long firstNonNull(Long first, Long second) {
        return first != null ? first : second;
    }

    private String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record GrowthContext(IotDevice device, Plot plot, GrowthStage stage) {
    }

    private record PumpRange(BigDecimal flowMin, BigDecimal flowMax, BigDecimal pressureMin, BigDecimal pressureMax) {
    }
}
