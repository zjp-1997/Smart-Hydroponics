package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.CameraCapturePlanSaveRequest;
import com.smart_plant.smart_plant.entity.CameraCapturePlan;
import com.smart_plant.smart_plant.entity.CameraDevice;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CameraCapturePlanMapper;
import com.smart_plant.smart_plant.mapper.CameraDeviceMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.CameraCapturePlanService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;

/** 摄像头采集计划实现，负责权限、字段校验、乐观锁及下一次抓拍时间推算。 */
@Service
@RequiredArgsConstructor
public class CameraCapturePlanServiceImpl implements CameraCapturePlanService {
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    private static final int ALL_WEEKDAYS = 127;

    private final CameraCapturePlanMapper planMapper;
    private final CameraDeviceMapper cameraMapper;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;

    /** 新增时由服务端根据摄像头写入可信的数据归属。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CameraCapturePlan add(CameraCapturePlanSaveRequest request) {
        validateRequest(request, false);
        CameraDevice camera = requireManageableCamera(request.getDeviceId());
        CameraCapturePlan plan = toEntity(request, camera, dataPermissionService.currentUser().getId());
        planMapper.insert(plan);
        operationLogService.record("新增摄像头采集计划：" + plan.getPlanName());
        return planMapper.selectById(plan.getId());
    }

    /** 修改时通过版本号阻止多人并发保存覆盖。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CameraCapturePlan update(CameraCapturePlanSaveRequest request) {
        validateRequest(request, true);
        CameraCapturePlan existing = requirePlan(request.getId());
        CameraDevice camera = requireManageableCamera(request.getDeviceId());
        CameraCapturePlan plan = toEntity(request, camera, dataPermissionService.currentUser().getId());
        plan.setId(existing.getId());
        plan.setVersion(request.getVersion());
        plan.setLastCaptureTime(existing.getLastCaptureTime());
        if (planMapper.update(plan) != 1) {
            throw paramError("计划已被其他用户修改，请刷新后重试");
        }
        operationLogService.record("修改摄像头采集计划：" + plan.getPlanName());
        return planMapper.selectById(plan.getId());
    }

    /** 删除前校验计划归属权限。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CameraCapturePlan plan = requirePlan(id);
        if (planMapper.deleteById(id) != 1) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "摄像头采集计划不存在");
        }
        operationLogService.record("删除摄像头采集计划：" + plan.getPlanName());
    }

    /** 查询详情时复用统一的存在性和数据权限检查。 */
    @Override
    public CameraCapturePlan get(Long id) {
        return requirePlan(id);
    }

    /** 查询列表时按登录用户限制数据范围并限制最大页大小。 */
    @Override
    public PageInfo<CameraCapturePlan> list(Long plotId, String keyword, Integer enabled,
                                            Integer pageNum, Integer pageSize) {
        if (enabled != null && enabled != 0 && enabled != 1) {
            throw paramError("启用状态只能为0或1");
        }
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(planMapper.selectList(scopedUserId, plotId, normalize(keyword), enabled));
    }

    /** 校验保存字段，保证数据库约束失败前返回可理解的业务错误。 */
    private void validateRequest(CameraCapturePlanSaveRequest request, boolean updating) {
        if (request == null) {
            throw paramError("摄像头采集计划不能为空");
        }
        if (updating && (request.getId() == null || request.getVersion() == null)) {
            throw paramError("修改计划时ID和版本号不能为空");
        }
        if (request.getDeviceId() == null) {
            throw paramError("摄像头不能为空");
        }
        if (!StringUtils.hasText(request.getPlanName()) || request.getPlanName().trim().length() > 100) {
            throw paramError("计划名称不能为空且不能超过100个字符");
        }
        if (request.getIntervalMinutes() == null || request.getIntervalMinutes() < 1
                || request.getIntervalMinutes() > 1440) {
            throw paramError("抓拍间隔必须在1到1440分钟之间");
        }
        int weekdays = request.getWeekdaysMask() == null ? ALL_WEEKDAYS : request.getWeekdaysMask();
        if (weekdays < 1 || weekdays > ALL_WEEKDAYS) {
            throw paramError("运行星期设置不正确");
        }
        if (request.getStartTime() == null || request.getEndTime() == null
                || !request.getEndTime().isAfter(request.getStartTime())) {
            throw paramError("每日结束时间必须晚于开始时间");
        }
        validateTimezone(request.getTimezone());
        if (request.getRemark() != null && request.getRemark().trim().length() > 255) {
            throw paramError("备注不能超过255个字符");
        }
    }

    /** 将白名单请求转换为实体，并计算软件侧下一次理论抓拍时间。 */
    private CameraCapturePlan toEntity(CameraCapturePlanSaveRequest request, CameraDevice camera, Long operatorId) {
        CameraCapturePlan plan = new CameraCapturePlan();
        plan.setDeviceId(camera.getId());
        plan.setUserId(camera.getUserId());
        plan.setPlotId(camera.getPlotId());
        plan.setPlanName(request.getPlanName().trim());
        plan.setIntervalMinutes(request.getIntervalMinutes());
        plan.setWeekdaysMask(request.getWeekdaysMask() == null ? ALL_WEEKDAYS : request.getWeekdaysMask());
        plan.setStartTime(request.getStartTime().withSecond(0).withNano(0));
        plan.setEndTime(request.getEndTime().withSecond(0).withNano(0));
        plan.setTimezone(StringUtils.hasText(request.getTimezone()) ? request.getTimezone().trim() : DEFAULT_TIMEZONE);
        plan.setEnabled(Boolean.FALSE.equals(request.getEnabled()) ? 0 : 1);
        plan.setRemark(normalize(request.getRemark()));
        plan.setCreateBy(operatorId);
        plan.setUpdateBy(operatorId);
        plan.setNextCaptureTime(calculateNextCapture(plan, LocalDateTime.now(ZoneId.of(plan.getTimezone()))));
        return plan;
    }

    /** 在未来七天内寻找第一个符合星期、时段和间隔的理论执行点。 */
    private LocalDateTime calculateNextCapture(CameraCapturePlan plan, LocalDateTime now) {
        if (!Integer.valueOf(1).equals(plan.getEnabled())) {
            return null;
        }
        for (int offset = 0; offset <= 7; offset++) {
            LocalDate date = now.toLocalDate().plusDays(offset);
            if (!isSelectedWeekday(plan.getWeekdaysMask(), date.getDayOfWeek())) {
                continue;
            }
            LocalDateTime start = LocalDateTime.of(date, plan.getStartTime());
            LocalDateTime end = LocalDateTime.of(date, plan.getEndTime());
            LocalDateTime reference = offset == 0 ? now : start.minusNanos(1);
            if (reference.isBefore(start)) {
                return start;
            }
            if (reference.isBefore(end)) {
                long steps = Duration.between(start, reference).toMinutes() / plan.getIntervalMinutes() + 1;
                LocalDateTime candidate = start.plusMinutes(steps * plan.getIntervalMinutes());
                if (!candidate.isAfter(end)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /** 判断指定星期是否包含在位掩码内。 */
    private boolean isSelectedWeekday(Integer mask, DayOfWeek dayOfWeek) {
        return (mask & (1 << (dayOfWeek.getValue() - 1))) != 0;
    }

    /** 获取摄像头并验证当前用户具备其所属农场的管理权限。 */
    private CameraDevice requireManageableCamera(Long cameraId) {
        CameraDevice camera = cameraMapper.selectById(cameraId);
        if (camera == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "摄像头不存在");
        }
        dataPermissionService.requireFarmManager(camera.getUserId());
        return camera;
    }

    /** 获取计划并验证数据归属权限。 */
    private CameraCapturePlan requirePlan(Long id) {
        if (id == null || id <= 0) {
            throw paramError("计划ID不正确");
        }
        CameraCapturePlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "摄像头采集计划不存在");
        }
        dataPermissionService.requireFarmManager(plan.getUserId());
        return plan;
    }

    /** 验证IANA时区名称。 */
    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(StringUtils.hasText(timezone) ? timezone.trim() : DEFAULT_TIMEZONE);
        } catch (ZoneRulesException exception) {
            throw paramError("时区格式不正确");
        }
    }

    /** 标准化可选文本。 */
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 统一创建参数错误。 */
    private BusinessException paramError(String message) {
        return new BusinessException(ResponseCode.PARAM_ERROR, message);
    }
}
