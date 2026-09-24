package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.IotDevicePlanPageResponse;
import com.smart_plant.smart_plant.dto.IotDevicePlanResponse;
import com.smart_plant.smart_plant.dto.IotDevicePlanSaveRequest;
import com.smart_plant.smart_plant.entity.IotDevice;
import com.smart_plant.smart_plant.entity.IotDevicePlan;
import com.smart_plant.smart_plant.entity.IotDeviceSchedule;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.IotDeviceMapper;
import com.smart_plant.smart_plant.mapper.IotDevicePlanMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.IotDevicePlanService;
import com.smart_plant.smart_plant.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 设备计划领域服务：负责数据权限、参数校验、乐观锁以及运行时段冲突检查。
 * 当前阶段只保存“软件期望配置”，不会直接改变硬件在线状态或伪造采集记录。
 */
@Service
@RequiredArgsConstructor
public class IotDevicePlanServiceImpl implements IotDevicePlanService {
    private static final Set<String> SUPPORTED_TYPES = Set.of("ENV_SENSOR", "WATER_QUALITY", "GROW_LIGHT", "WATER_PUMP");
    private static final Set<String> ACTUATOR_TYPES = Set.of("GROW_LIGHT", "WATER_PUMP");
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    private static final int WEEK_MINUTES = 7 * 24 * 60;

    private final IotDevicePlanMapper planMapper;
    private final IotDeviceMapper deviceMapper;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;

    /** 查询当前用户可管理的四类设备及其计划，尚未建计划的设备也会返回默认配置。 */
    @Override
    public IotDevicePlanPageResponse listPlans(Long plotId, String typeCode, String keyword,
                                               Integer pageNum, Integer pageSize) {
        String normalizedType = normalizeType(typeCode);
        if (StringUtils.hasText(normalizedType) && !SUPPORTED_TYPES.contains(normalizedType)) {
            throw paramError("不支持的设备类型");
        }
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        PageInfo<IotDevicePlan> page = new PageInfo<>(planMapper.selectDevicePlans(
                scopedUserId, plotId, normalizedType, normalize(keyword)));
        List<IotDevicePlanResponse> responses = attachSchedules(page.getList());
        return new IotDevicePlanPageResponse(responses, page.getTotal(), page.getPageNum(), page.getPageSize());
    }

    /** 按设备读取计划详情，并在服务端再次执行设备归属校验。 */
    @Override
    public IotDevicePlanResponse getByDeviceId(Long deviceId) {
        IotDevice device = requireManageableDevice(deviceId);
        IotDevicePlan plan = requireSupportedPlanView(device.getId());
        return attachSchedules(List.of(plan)).getFirst();
    }

    /** 新增或更新设备计划；版本号用于避免多人同时修改时互相覆盖。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public IotDevicePlanResponse save(IotDevicePlanSaveRequest request) {
        if (request == null || request.getDeviceId() == null) {
            throw paramError("设备ID不能为空");
        }
        IotDevice device = requireManageableDevice(request.getDeviceId());
        String typeCode = normalizeType(device.getTypeCode());
        if (!SUPPORTED_TYPES.contains(typeCode)) {
            throw paramError("该设备类型不支持采集或运行计划");
        }
        validate(request, typeCode);

        User operator = dataPermissionService.currentUser();
        IotDevicePlan existing = planMapper.selectByDeviceId(device.getId());
        IotDevicePlan plan = toEntity(request, operator.getId());
        if (existing.getId() == null) {
            if (request.getVersion() != null) {
                throw paramError("计划已发生变化，请刷新后重试");
            }
            planMapper.insertPlan(plan);
        } else {
            if (request.getVersion() == null || !request.getVersion().equals(existing.getVersion())) {
                throw paramError("计划已被其他用户修改，请刷新后重试");
            }
            plan.setId(existing.getId());
            if (planMapper.updatePlan(plan) != 1) {
                throw paramError("计划已被其他用户修改，请刷新后重试");
            }
            planMapper.deleteSchedulesByPlanId(existing.getId());
        }

        List<IotDeviceSchedule> schedules = toSchedules(request.getSchedules(), plan.getId());
        if (!schedules.isEmpty()) {
            planMapper.insertSchedules(schedules);
        }
        operationLogService.record("保存设备计划：" + device.getDeviceCode());
        return attachSchedules(List.of(requireSupportedPlanView(device.getId()))).getFirst();
    }

    /** 删除设备的软件计划，不删除设备本身，也不向未接入的硬件发送指令。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByDeviceId(Long deviceId) {
        IotDevice device = requireManageableDevice(deviceId);
        IotDevicePlan existing = planMapper.selectByDeviceId(deviceId);
        if (existing == null || existing.getId() == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备尚未保存计划");
        }
        planMapper.deletePlanByDeviceId(deviceId);
        operationLogService.record("删除设备计划：" + device.getDeviceCode());
    }

    private IotDevice requireManageableDevice(Long deviceId) {
        if (deviceId == null || deviceId <= 0) {
            throw paramError("设备ID不正确");
        }
        IotDevice device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备不存在");
        }
        dataPermissionService.requireDeviceManager(device);
        return device;
    }

    private IotDevicePlan requireSupportedPlanView(Long deviceId) {
        IotDevicePlan plan = planMapper.selectByDeviceId(deviceId);
        if (plan == null || !SUPPORTED_TYPES.contains(normalizeType(plan.getTypeCode()))) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备不支持计划管理");
        }
        return plan;
    }

    private void validate(IotDevicePlanSaveRequest request, String typeCode) {
        if (request.getCollectionIntervalMinutes() == null
                || request.getCollectionIntervalMinutes() < 1
                || request.getCollectionIntervalMinutes() > 1440) {
            throw paramError("采集间隔必须在1到1440分钟之间");
        }
        String timezone = StringUtils.hasText(request.getTimezone()) ? request.getTimezone().trim() : DEFAULT_TIMEZONE;
        try {
            ZoneId.of(timezone);
        } catch (ZoneRulesException exception) {
            throw paramError("时区格式不正确");
        }
        if (request.getEffectiveFrom() != null && request.getEffectiveTo() != null
                && request.getEffectiveFrom().isAfter(request.getEffectiveTo())) {
            throw paramError("结束日期不能早于开始日期");
        }
        boolean controlEnabled = Boolean.TRUE.equals(request.getControlEnabled());
        List<IotDevicePlanSaveRequest.ScheduleItem> schedules = request.getSchedules() == null
                ? List.of() : request.getSchedules();
        if (!ACTUATOR_TYPES.contains(typeCode) && (controlEnabled || !schedules.isEmpty())) {
            throw paramError("环境和水质设备不能配置自动开关时段");
        }
        if (controlEnabled && schedules.stream().noneMatch(item -> item != null && !Boolean.FALSE.equals(item.getEnabled()))) {
            throw paramError("启用自动控制时至少需要一个有效时段");
        }
        for (int index = 0; index < schedules.size(); index++) {
            validateSchedule(schedules.get(index), index);
        }
        validateNoOverlap(schedules);
    }

    private void validateSchedule(IotDevicePlanSaveRequest.ScheduleItem item, int index) {
        if (item == null) {
            throw paramError("第" + (index + 1) + "个运行时段不能为空");
        }
        if (!StringUtils.hasText(item.getScheduleName()) || item.getScheduleName().trim().length() > 100) {
            throw paramError("运行时段名称不能为空且不能超过100个字符");
        }
        if (item.getWeekdaysMask() == null || item.getWeekdaysMask() < 1 || item.getWeekdaysMask() > 127) {
            throw paramError("运行星期设置不正确");
        }
        if (item.getStartTime() == null || item.getEndTime() == null || item.getStartTime().equals(item.getEndTime())) {
            throw paramError("运行时段的开始和结束时间必须不同");
        }
    }

    /**
     * 将每周时段展开成分钟区间后检查重叠；跨午夜时段会拆分到本周末和下周初，
     * 因而可以正确识别“周日晚跨到周一”这类边界冲突。
     */
    private void validateNoOverlap(List<IotDevicePlanSaveRequest.ScheduleItem> schedules) {
        List<MinuteRange> ranges = new ArrayList<>();
        for (IotDevicePlanSaveRequest.ScheduleItem item : schedules) {
            if (item == null || Boolean.FALSE.equals(item.getEnabled())) {
                continue;
            }
            for (int day = 0; day < 7; day++) {
                if ((item.getWeekdaysMask() & (1 << day)) == 0) {
                    continue;
                }
                int start = day * 1440 + minuteOfDay(item.getStartTime());
                int endMinute = minuteOfDay(item.getEndTime());
                int end = day * 1440 + endMinute;
                if (endMinute <= minuteOfDay(item.getStartTime())) {
                    end += 1440;
                }
                if (end <= WEEK_MINUTES) {
                    ranges.add(new MinuteRange(start, end, item.getScheduleName()));
                } else {
                    ranges.add(new MinuteRange(start, WEEK_MINUTES, item.getScheduleName()));
                    ranges.add(new MinuteRange(0, end - WEEK_MINUTES, item.getScheduleName()));
                }
            }
        }
        ranges.sort(Comparator.comparingInt(MinuteRange::start));
        for (int index = 1; index < ranges.size(); index++) {
            MinuteRange previous = ranges.get(index - 1);
            MinuteRange current = ranges.get(index);
            if (current.start() < previous.end()) {
                throw paramError("运行时段存在重叠：" + previous.name() + " 与 " + current.name());
            }
        }
    }

    private int minuteOfDay(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    private IotDevicePlan toEntity(IotDevicePlanSaveRequest request, Long operatorId) {
        IotDevicePlan plan = new IotDevicePlan();
        plan.setDeviceId(request.getDeviceId());
        plan.setCollectionEnabled(Boolean.FALSE.equals(request.getCollectionEnabled()) ? 0 : 1);
        plan.setCollectionIntervalMinutes(request.getCollectionIntervalMinutes());
        plan.setControlEnabled(Boolean.TRUE.equals(request.getControlEnabled()) ? 1 : 0);
        plan.setTimezone(StringUtils.hasText(request.getTimezone()) ? request.getTimezone().trim() : DEFAULT_TIMEZONE);
        plan.setEffectiveFrom(request.getEffectiveFrom());
        plan.setEffectiveTo(request.getEffectiveTo());
        plan.setVersion(request.getVersion());
        plan.setCreateBy(operatorId);
        plan.setUpdateBy(operatorId);
        return plan;
    }

    private List<IotDeviceSchedule> toSchedules(List<IotDevicePlanSaveRequest.ScheduleItem> items, Long planId) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<IotDeviceSchedule> schedules = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            IotDevicePlanSaveRequest.ScheduleItem item = items.get(index);
            IotDeviceSchedule schedule = new IotDeviceSchedule();
            schedule.setPlanId(planId);
            schedule.setScheduleName(item.getScheduleName().trim());
            schedule.setWeekdaysMask(item.getWeekdaysMask());
            schedule.setStartTime(item.getStartTime().withSecond(0).withNano(0));
            schedule.setEndTime(item.getEndTime().withSecond(0).withNano(0));
            schedule.setEnabled(Boolean.FALSE.equals(item.getEnabled()) ? 0 : 1);
            schedule.setSortOrder(item.getSortOrder() == null ? index : item.getSortOrder());
            schedules.add(schedule);
        }
        return schedules;
    }

    private List<IotDevicePlanResponse> attachSchedules(List<IotDevicePlan> plans) {
        List<Long> planIds = plans.stream().map(IotDevicePlan::getId).filter(id -> id != null).toList();
        Map<Long, List<IotDeviceSchedule>> schedulesByPlan = new HashMap<>();
        if (!planIds.isEmpty()) {
            for (IotDeviceSchedule schedule : planMapper.selectSchedulesByPlanIds(planIds)) {
                schedulesByPlan.computeIfAbsent(schedule.getPlanId(), ignored -> new ArrayList<>()).add(schedule);
            }
        }
        return plans.stream().map(plan -> toResponse(plan, schedulesByPlan.getOrDefault(plan.getId(), List.of()))).toList();
    }

    private IotDevicePlanResponse toResponse(IotDevicePlan plan, List<IotDeviceSchedule> schedules) {
        IotDevicePlanResponse response = new IotDevicePlanResponse();
        response.setId(plan.getId());
        response.setDeviceId(plan.getDeviceId());
        response.setPlotId(plan.getPlotId());
        response.setPlotName(plan.getPlotName());
        response.setDeviceCode(plan.getDeviceCode());
        response.setDeviceName(plan.getDeviceName());
        response.setTypeCode(plan.getTypeCode());
        response.setTypeName(plan.getTypeName());
        response.setOnlineStatus(plan.getOnlineStatus());
        response.setHealthStatus(plan.getHealthStatus());
        response.setCollectionEnabled(plan.getId() == null || Integer.valueOf(1).equals(plan.getCollectionEnabled()));
        response.setCollectionIntervalMinutes(plan.getId() == null ? defaultInterval(plan.getTypeCode()) : plan.getCollectionIntervalMinutes());
        response.setControlEnabled(plan.getId() != null && Integer.valueOf(1).equals(plan.getControlEnabled()));
        response.setTimezone(plan.getId() == null ? DEFAULT_TIMEZONE : plan.getTimezone());
        response.setEffectiveFrom(plan.getEffectiveFrom());
        response.setEffectiveTo(plan.getEffectiveTo());
        response.setApplyStatus(plan.getId() == null ? 0 : plan.getApplyStatus());
        response.setLastAppliedAt(plan.getLastAppliedAt());
        response.setLastApplyError(plan.getLastApplyError());
        response.setVersion(plan.getVersion());
        response.setUpdateTime(plan.getUpdateTime());
        response.setSchedules(new ArrayList<>(schedules));
        return response;
    }

    private int defaultInterval(String typeCode) {
        return ACTUATOR_TYPES.contains(normalizeType(typeCode)) ? 15 : 60;
    }

    private String normalizeType(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : "";
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BusinessException paramError(String message) {
        return new BusinessException(ResponseCode.PARAM_ERROR, message);
    }

    private record MinuteRange(int start, int end, String name) { }
}
