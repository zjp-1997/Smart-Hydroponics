package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ClientPlotDetailResponse;
import com.smart_plant.smart_plant.dto.ClientPlotListResponse;
import com.smart_plant.smart_plant.dto.ClientPlotUpdateRequest;
import com.smart_plant.smart_plant.dto.PlotStatisticsResponse;
import com.smart_plant.smart_plant.dto.PlotHarvestRequest;
import com.smart_plant.smart_plant.dto.PlotBoundaryPoint;
import com.smart_plant.smart_plant.entity.EnvironmentData;
import com.smart_plant.smart_plant.entity.Crop;
import com.smart_plant.smart_plant.entity.Farm;
import com.smart_plant.smart_plant.entity.FarmTask;
import com.smart_plant.smart_plant.entity.IotDevice;
import com.smart_plant.smart_plant.entity.PlantingBatch;
import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.entity.WaterQualityData;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.EnvironmentDataMapper;
import com.smart_plant.smart_plant.mapper.CropMapper;
import com.smart_plant.smart_plant.mapper.FarmMapper;
import com.smart_plant.smart_plant.mapper.FarmTaskMapper;
import com.smart_plant.smart_plant.mapper.IotDeviceMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.mapper.WaterQualityDataMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.PlantingBatchService;
import com.smart_plant.smart_plant.service.PlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlotServiceImpl implements PlotService {

    private static final String FARM_OWNER_ROLE_CODE = "farm_owner";

    private final PlotMapper plotMapper;
    private final CropMapper cropMapper;

    private final UserMapper userMapper;

    private final FarmMapper farmMapper;

    private final EnvironmentDataMapper environmentDataMapper;

    private final WaterQualityDataMapper waterQualityDataMapper;

    private final FarmTaskMapper farmTaskMapper;

    private final IotDeviceMapper iotDeviceMapper;

    private final DataPermissionService dataPermissionService;

    private final PlantingBatchService plantingBatchService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Plot addPlot(Plot plot) {
        if (!dataPermissionService.isAdmin() && plot != null) {
            plot.setUserId(dataPermissionService.currentUser().getId());
        }
        normalizeDefaults(plot);
        synchronizeCoordinate(plot);
        validateCreatePlot(plot);
        Farm farm = requireActiveFarm(plot.getFarmId());
        plot.setUserId(farm.getUserId());
        dataPermissionService.requireFarmManager(plot.getUserId());
        checkFarmOwnerUser(plot.getUserId());
        validateFarmArea(plot, farm, null);
        checkUniquePlotCode(plot);
        plotMapper.insert(plot);
        synchronizeCurrentPlantingBatch(plot, null);
        return plotMapper.selectById(plot.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlot(Long id) {
        requireId(id);
        dataPermissionService.requireFarmManager(getPlotById(id).getUserId());
        checkNoPlantingBatch(id);
        int rows = plotMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "地块不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePlots(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的地块");
        }
        for (Long id : ids) {
            requireId(id);
            dataPermissionService.requireFarmManager(getPlotById(id).getUserId());
            checkNoPlantingBatch(id);
        }
        return plotMapper.deleteBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Plot updatePlot(Plot plot) {
        if (plot == null || plot.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块ID不能为空");
        }
        Plot oldPlot = plotMapper.selectById(plot.getId());
        if (oldPlot == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "地块不存在");
        }
        dataPermissionService.requireFarmManager(oldPlot.getUserId());
        if (!dataPermissionService.isAdmin()) {
            plot.setUserId(oldPlot.getUserId());
        }
        if (plot.getFarmId() == null) {
            plot.setFarmId(oldPlot.getFarmId());
        }
        if (plot.getFarmId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择所属农场");
        }
        Farm farm = requireActiveFarm(plot.getFarmId());
        plot.setUserId(farm.getUserId());
        if (!plot.getUserId().equals(oldPlot.getUserId()) && plotMapper.countPlantingBatchByPlotId(plot.getId()) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块已存在种植批次，不能修改所属用户");
        }
        dataPermissionService.requireFarmManager(plot.getUserId());
        checkFarmOwnerUser(plot.getUserId());
        if (plot.getArea() == null) {
            plot.setArea(oldPlot.getArea());
        }
        if (!StringUtils.hasText(plot.getAreaUnit())) {
            plot.setAreaUnit(oldPlot.getAreaUnit());
        }
        synchronizeCoordinate(plot);
        validateUpdatePlot(plot);
        validateFarmArea(plot, farm, plot.getId());
        checkUniquePlotCode(plot);
        int rows = plotMapper.updateById(plot);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "地块修改失败");
        }
        synchronizeCurrentPlantingBatch(plot, oldPlot);
        return plotMapper.selectById(plot.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireId(id);
        dataPermissionService.requireFarmManager(getPlotById(id).getUserId());
        validateStatus(status);
        int rows = plotMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "地块不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlantingBatch harvestPlot(Long id, PlotHarvestRequest request) {
        requireId(id);
        Plot plot = plotMapper.selectById(id);
        if (plot == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "地块不存在");
        }
        dataPermissionService.requireFarmManager(plot.getUserId());
        if (plot.getCurrentBatchId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该地块当前没有种植中的批次");
        }
        if (!isHarvestReady(plot)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "当前批次尚未进入成熟期");
        }
        return plantingBatchService.harvestActiveBatch(id, request);
    }

    @Override
    public Plot getPlotById(Long id) {
        requireId(id);
        Plot plot = plotMapper.selectById(id);
        if (plot == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "地块不存在");
        }
        dataPermissionService.requireOwnedResource(plot.getUserId());
        return plot;
    }

    @Override
    public PageInfo<Plot> listPlots(String plotName, String plotCode, Long farmId, Long userId, Integer type,
                                    Integer status, Integer pageNum, Integer pageSize) {
        validateType(type);
        validateStatus(status);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = dataPermissionService.restrictUserId(userId);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(plotMapper.selectList(plotName, plotCode, farmId, scopedUserId, type, status));
    }

    @Override
    public PlotStatisticsResponse statisticsPlots() {
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PlotStatisticsResponse statistics = plotMapper.selectStatistics(scopedUserId);
        return statistics == null ? PlotStatisticsResponse.empty() : statistics;
    }

    @Override
    public List<ClientPlotListResponse> listCurrentClientPlots() {
        // 用户端全部地块列表以 token 中的当前用户为准，避免客户端传 userId 造成越权查询。
        Long currentUserId = dataPermissionService.currentClientOwnerId();
        return plotMapper.selectList(null, null, null, currentUserId, null, 1)
                .stream()
                .map(this::toClientPlotListResponse)
                .toList();
    }

    @Override
    public List<ClientPlotListResponse> listCurrentClientPlotsByFarmId(Long farmId) {
        if (farmId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农场ID不能为空");
        }
        Long currentUserId = dataPermissionService.currentClientOwnerId();
        Farm farm = farmMapper.selectById(farmId);
        if (farm == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "农场不存在");
        }
        // 先校验农场归属，再查询其下地块，保证用户只能看到自己农场下的地块。
        dataPermissionService.requireClientFarmReader(farm.getUserId());
        if (!currentUserId.equals(farm.getUserId())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "No permission to access this farm");
        }
        return plotMapper.selectList(null, null, farmId, currentUserId, null, 1)
                .stream()
                .map(this::toClientPlotListResponse)
                .toList();
    }

    @Override
    public ClientPlotDetailResponse getCurrentClientPlotDetail(Long id) {
        requireId(id);
        Long currentUserId = dataPermissionService.currentClientOwnerId();
        Plot plot = plotMapper.selectById(id);
        if (plot == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "地块不存在");
        }
        // 地块详情是用户端高敏接口，必须先校验地块归属，再查询关联监测、设备和农事任务。
        if (!currentUserId.equals(plot.getUserId())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "No permission to access this plot");
        }

        ClientPlotDetailResponse response = toClientPlotDetailResponse(plot);
        if (Boolean.TRUE.equals(response.getIdle())) {
            // 空闲地块不展示历史监测和农事任务；设备仅在此详情响应中标记为离线，不改写物理设备状态。
            response.setMonitorItems(toMonitorItems(null, null));
            response.setDevices(iotDeviceMapper.selectList(currentUserId, id, null, null, null, null, null, null, null)
                    .stream()
                    .map(device -> {
                        ClientPlotDetailResponse.DeviceStatus status = toDeviceStatus(device);
                        status.setOnlineStatus(0);
                        status.setState("离线");
                        status.setEnabled(false);
                        return status;
                    })
                    .toList());
            return response;
        }
        EnvironmentData environmentData = latestEnvironmentData(currentUserId, id);
        WaterQualityData waterQualityData = latestWaterQualityData(currentUserId, id);
        response.setEnvironmentMonitor(toEnvironmentMonitor(environmentData));
        response.setWaterQualityMonitor(toWaterQualityMonitor(waterQualityData));
        response.setMonitorItems(toMonitorItems(environmentData, waterQualityData));
        // 普通用户在地块详情中只看指派给自己的任务，农场主保留全部任务视图。
        Long viewerId = dataPermissionService.currentUser().getId();
        response.setFarmTasks(farmTaskMapper.selectClientTasksByPlotId(currentUserId, id)
                .stream()
                .filter(task -> currentUserId.equals(viewerId) || viewerId.equals(task.getExecutorId()))
                .map(this::toFarmTaskSummary)
                .toList());
        response.setDevices(iotDeviceMapper.selectList(currentUserId, id, null, null, null, null, null, null, null)
                .stream()
                .map(this::toDeviceStatus)
                .toList());
        return response;
    }

    @Override
    public List<Crop> listCurrentClientCrops() {
        // 包含系统公共作物及农场主可见的启用作物；其他角色无权获取编辑选项。
        Long ownerId = requireCurrentClientFarmOwner();
        return cropMapper.selectClientEditableCrops(ownerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentClientPlot(Long id, ClientPlotUpdateRequest request) {
        Long ownerId = requireCurrentClientFarmOwner();
        requireId(id);
        Plot current = plotMapper.selectById(id);
        if (current == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "地块不存在");
        }
        // 角色与资源归属都在服务端核验，不能依赖页面隐藏编辑按钮。
        if (!ownerId.equals(current.getUserId())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "无权修改该地块");
        }
        if (request == null || request.getCropId() == null || !StringUtils.hasText(request.getPlotName())
                || request.getPlantingArea() == null || request.getPlantedAt() == null
                || request.getExpectedHarvestAt() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请填写完整的地块信息");
        }
        if (request.getPlotName().trim().length() > 100
                || request.getPlantingArea().compareTo(BigDecimal.ZERO) <= 0
                || request.getPlantingArea().scale() > 2
                || request.getPlantingArea().precision() - request.getPlantingArea().scale() > 8) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块名称或种植面积不合法");
        }
        if (request.getExpectedHarvestAt().isBefore(request.getPlantedAt())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "预计采收时间不能早于种植时间");
        }
        // 作物必须属于农场主可见且已启用的选项，防止伪造其他用户的作物 ID。
        Crop crop = cropMapper.selectById(request.getCropId());
        if (crop == null || !Integer.valueOf(1).equals(crop.getStatus())
                || (crop.getUserId() != null && cropMapper.countVisibleByUserId(crop.getId(), ownerId) == 0)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择有效的作物");
        }
        Plot update = new Plot();
        update.setId(id);
        update.setPlotName(request.getPlotName().trim());
        update.setArea(request.getPlantingArea());
        update.setAreaUnit(current.getAreaUnit());
        update.setFarmId(current.getFarmId());
        validateFarmArea(update, requireActiveFarm(current.getFarmId()), id);
        if (plotMapper.updateById(update) != 1) {
            throw new BusinessException(ResponseCode.FAIL, "地块修改失败");
        }

        // 当前批次不存在时新建种植记录；存在时仅更新作物、面积和日期，其余批次信息保持原值。
        PlantingBatch batch = new PlantingBatch();
        batch.setId(current.getCurrentBatchId());
        batch.setPlotId(id);
        batch.setUserId(ownerId);
        batch.setCropId(crop.getId());
        batch.setPlantingArea(request.getPlantingArea());
        batch.setAreaUnit(current.getAreaUnit());
        batch.setPlantedAt(request.getPlantedAt());
        batch.setExpectedHarvestAt(request.getExpectedHarvestAt());
        if (current.getCurrentBatchId() == null) {
            plantingBatchService.addPlantingBatch(batch);
        } else {
            plantingBatchService.updatePlantingBatch(batch);
        }
    }

    private Long requireCurrentClientFarmOwner() {
        User user = dataPermissionService.currentUser();
        if (user == null || !FARM_OWNER_ROLE_CODE.equalsIgnoreCase(user.getRoleCode())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "只有农场主可以编辑地块");
        }
        return user.getId();
    }

    private void validateCreatePlot(Plot plot) {
        if (plot == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块信息不能为空");
        }
        if (plot.getFarmId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择所属农场");
        }
        if (!StringUtils.hasText(plot.getPlotName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块名称不能为空");
        }
        validateUpdatePlot(plot);
    }

    private void validateUpdatePlot(Plot plot) {
        validateType(plot.getType());
        validateStatus(plot.getStatus());
        validateNonNegative(plot.getArea(), "地块面积不能小于0");
        if (plot.getCropImage() != null && plot.getCropImage().length() > 500) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "作物图片地址长度不能超过500个字符");
        }
        validateLongitude(plot.getLongitude());
        validateLatitude(plot.getLatitude());
        validateBoundaryPoints(plot.getBoundaryPoints());
    }

    /**
     * 校验地块边界，阻止点数不足、重复点或退化成直线的无效多边形进入数据库。
     */
    private void validateBoundaryPoints(List<PlotBoundaryPoint> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        if (points.size() < 3) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块边界至少需要3个坐标点");
        }
        if (points.size() > 500) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块边界坐标点不能超过500个");
        }
        Set<String> uniquePoints = new HashSet<>();
        double twiceArea = 0;
        for (int index = 0; index < points.size(); index++) {
            PlotBoundaryPoint point = points.get(index);
            PlotBoundaryPoint next = points.get((index + 1) % points.size());
            if (point == null || point.getLongitude() == null || point.getLatitude() == null) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "地块边界坐标不能为空");
            }
            validateLongitude(point.getLongitude());
            validateLatitude(point.getLatitude());
            uniquePoints.add(point.getLongitude().stripTrailingZeros().toPlainString() + ","
                    + point.getLatitude().stripTrailingZeros().toPlainString());
            if (next != null && next.getLongitude() != null && next.getLatitude() != null) {
                twiceArea += point.getLongitude().doubleValue() * next.getLatitude().doubleValue()
                        - next.getLongitude().doubleValue() * point.getLatitude().doubleValue();
            }
        }
        if (uniquePoints.size() != points.size()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块边界不能包含重复坐标点");
        }
        if (Math.abs(twiceArea) < 1e-12) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块边界不能由重复点或共线点组成");
        }
        if (hasSelfIntersection(points)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块边界不能存在交叉线段");
        }
    }

    /** 检查不相邻边是否相交，避免保存无法正确填充的蝴蝶形多边形。 */
    private boolean hasSelfIntersection(List<PlotBoundaryPoint> points) {
        int size = points.size();
        for (int first = 0; first < size; first++) {
            int firstNext = (first + 1) % size;
            for (int second = first + 1; second < size; second++) {
                int secondNext = (second + 1) % size;
                if (first == second || firstNext == second || secondNext == first) {
                    continue;
                }
                if (segmentsIntersect(points.get(first), points.get(firstNext),
                        points.get(second), points.get(secondNext))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 使用叉积方向判断两条边是否相交，同时覆盖共线重叠的异常情况。 */
    private boolean segmentsIntersect(PlotBoundaryPoint a, PlotBoundaryPoint b,
                                      PlotBoundaryPoint c, PlotBoundaryPoint d) {
        double abC = cross(a, b, c);
        double abD = cross(a, b, d);
        double cdA = cross(c, d, a);
        double cdB = cross(c, d, b);
        double epsilon = 1e-12;
        if (((abC > epsilon && abD < -epsilon) || (abC < -epsilon && abD > epsilon))
                && ((cdA > epsilon && cdB < -epsilon) || (cdA < -epsilon && cdB > epsilon))) {
            return true;
        }
        return Math.abs(abC) <= epsilon && isPointOnSegment(a, b, c)
                || Math.abs(abD) <= epsilon && isPointOnSegment(a, b, d)
                || Math.abs(cdA) <= epsilon && isPointOnSegment(c, d, a)
                || Math.abs(cdB) <= epsilon && isPointOnSegment(c, d, b);
    }

    /** 计算向量 AB 与 AC 的二维叉积。 */
    private double cross(PlotBoundaryPoint a, PlotBoundaryPoint b, PlotBoundaryPoint c) {
        return (b.getLongitude().doubleValue() - a.getLongitude().doubleValue())
                * (c.getLatitude().doubleValue() - a.getLatitude().doubleValue())
                - (b.getLatitude().doubleValue() - a.getLatitude().doubleValue())
                * (c.getLongitude().doubleValue() - a.getLongitude().doubleValue());
    }

    /** 判断坐标点是否位于指定线段的包围范围内。 */
    private boolean isPointOnSegment(PlotBoundaryPoint a, PlotBoundaryPoint b, PlotBoundaryPoint point) {
        double longitude = point.getLongitude().doubleValue();
        double latitude = point.getLatitude().doubleValue();
        double epsilon = 1e-12;
        return longitude >= Math.min(a.getLongitude().doubleValue(), b.getLongitude().doubleValue()) - epsilon
                && longitude <= Math.max(a.getLongitude().doubleValue(), b.getLongitude().doubleValue()) + epsilon
                && latitude >= Math.min(a.getLatitude().doubleValue(), b.getLatitude().doubleValue()) - epsilon
                && latitude <= Math.max(a.getLatitude().doubleValue(), b.getLatitude().doubleValue()) + epsilon;
    }

    private void normalizeDefaults(Plot plot) {
        if (plot == null) {
            return;
        }
        if (StringUtils.hasText(plot.getPlotName())) {
            plot.setPlotName(plot.getPlotName().trim());
        }
        if (StringUtils.hasText(plot.getPlotCode())) {
            plot.setPlotCode(plot.getPlotCode().trim());
        }
        if (StringUtils.hasText(plot.getCropImage())) {
            plot.setCropImage(plot.getCropImage().trim());
        }
        if (!StringUtils.hasText(plot.getAreaUnit())) {
            plot.setAreaUnit("亩");
        }
        if (plot.getType() == null) {
            plot.setType(2);
        }
        if (plot.getStatus() == null) {
            plot.setStatus(1);
        }
        if (StringUtils.hasText(plot.getAddress())) {
            plot.setAddress(plot.getAddress().trim());
        }
    }

    private void synchronizeCurrentPlantingBatch(Plot plot, Plot oldPlot) {
        if (plot == null || plot.getId() == null) {
            return;
        }

        Long currentBatchId = oldPlot == null ? null : oldPlot.getCurrentBatchId();
        Integer requestedStatus = plot.getCurrentBatchStatus();
        if (Integer.valueOf(0).equals(requestedStatus)) {
            if (currentBatchId != null) {
                plantingBatchService.updateStatus(currentBatchId, 4);
            }
            return;
        }

        if (!hasCurrentBatchInput(plot)) {
            return;
        }
        if (plot.getCurrentCropId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择作物名称");
        }

        PlantingBatch batch = new PlantingBatch();
        batch.setId(currentBatchId);
        batch.setPlotId(plot.getId());
        batch.setUserId(plot.getUserId());
        batch.setCropId(plot.getCurrentCropId());
        batch.setPlantingArea(plot.getCurrentPlantingArea() == null ? plot.getArea() : plot.getCurrentPlantingArea());
        batch.setAreaUnit(defaultText(plot.getCurrentPlantingAreaUnit(), defaultText(plot.getAreaUnit(), "亩")));
        batch.setPlantedAt(plot.getCurrentPlantedAt());
        batch.setExpectedHarvestAt(plot.getCurrentExpectedHarvestAt());
        batch.setGrowthStageId(plot.getCurrentGrowthStageId());
        batch.setExpectedYieldAmount(plot.getCurrentExpectedYieldAmount());
        batch.setGrownDays(plot.getCurrentGrownDays());
        batch.setCropImage(plot.getCropImage());
        batch.setStatus(requestedStatus == null ? 1 : requestedStatus);
        batch.setYieldUnit(defaultText(plot.getCurrentYieldUnit(), "kg"));

        if (currentBatchId == null) {
            plantingBatchService.addPlantingBatch(batch);
        } else {
            plantingBatchService.updatePlantingBatch(batch);
        }
    }

    private boolean hasCurrentBatchInput(Plot plot) {
        return plot.getCurrentBatchStatus() != null
                || plot.getCurrentCropId() != null
                || plot.getCurrentPlantingArea() != null
                || plot.getCurrentPlantedAt() != null
                || plot.getCurrentExpectedHarvestAt() != null
                || plot.getCurrentGrowthStageId() != null
                || plot.getCurrentExpectedYieldAmount() != null
                || plot.getCurrentGrownDays() != null
                || StringUtils.hasText(plot.getCurrentPlantingAreaUnit())
                || StringUtils.hasText(plot.getCurrentYieldUnit());
    }

    private void synchronizeCoordinate(Plot plot) {
        if (plot == null) {
            return;
        }
        if (StringUtils.hasText(plot.getCoordinate())) {
            String normalized = plot.getCoordinate().trim().replace('，', ',');
            String[] parts = normalized.split(",");
            if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "经纬度格式必须为“经度,纬度”");
            }
            try {
                BigDecimal longitude = new BigDecimal(parts[0].trim());
                BigDecimal latitude = new BigDecimal(parts[1].trim());
                plot.setLongitude(longitude);
                plot.setLatitude(latitude);
                plot.setCoordinate(formatCoordinate(longitude, latitude));
            } catch (NumberFormatException e) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "经纬度必须为数字");
            }
            return;
        }

        if (plot.getLongitude() != null && plot.getLatitude() != null) {
            plot.setCoordinate(formatCoordinate(plot.getLongitude(), plot.getLatitude()));
            return;
        }

        // 新增地块只绘制边界时，以全部顶点的平均位置补充中心点，保证地图列表仍可定位该地块。
        if (plot.getBoundaryPoints() != null && plot.getBoundaryPoints().size() >= 3
                && plot.getBoundaryPoints().stream().allMatch(point -> point != null
                && point.getLongitude() != null && point.getLatitude() != null)) {
            BigDecimal pointCount = BigDecimal.valueOf(plot.getBoundaryPoints().size());
            BigDecimal longitude = plot.getBoundaryPoints().stream()
                    .map(PlotBoundaryPoint::getLongitude)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(pointCount, 6, java.math.RoundingMode.HALF_UP);
            BigDecimal latitude = plot.getBoundaryPoints().stream()
                    .map(PlotBoundaryPoint::getLatitude)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(pointCount, 6, java.math.RoundingMode.HALF_UP);
            plot.setLongitude(longitude);
            plot.setLatitude(latitude);
            plot.setCoordinate(formatCoordinate(longitude, latitude));
        }
    }

    private String formatCoordinate(BigDecimal longitude, BigDecimal latitude) {
        return longitude.stripTrailingZeros().toPlainString() + "," + latitude.stripTrailingZeros().toPlainString();
    }

    private ClientPlotListResponse toClientPlotListResponse(Plot plot) {
        ClientPlotListResponse response = new ClientPlotListResponse();
        response.setId(plot.getId());
        response.setFarmId(plot.getFarmId());
        response.setFarmName(plot.getFarmName());
        response.setPlotName(defaultText(plot.getPlotName(), "-"));
        response.setPlotCode(defaultText(plot.getPlotCode(), "-"));
        response.setCropImage(plot.getCurrentCropImage());
        response.setCropName(defaultText(plot.getCurrentCropName(), "暂无作物"));
        response.setPlantingDays(resolvePlantingDays(plot));
        response.setGrowthStageName(defaultText(plot.getCurrentGrowthStageName(), "暂无阶段"));
        applyClientPlantingStatus(response, plot);
        response.setPlotAreaValue(plot.getArea());
        response.setPlotAreaUnit(defaultText(plot.getAreaUnit(), "亩"));
        response.setPlotArea(formatAmount(plot.getArea(), response.getPlotAreaUnit()));
        response.setPlantingTime(plot.getCurrentPlantedAt());
        response.setHarvestTime(resolveHarvestTime(plot));
        response.setEstimatedYieldAmount(plot.getCurrentExpectedYieldAmount());
        response.setYieldUnit(defaultText(plot.getCurrentYieldUnit(), "kg"));
        response.setEstimatedYield(formatAmount(plot.getCurrentExpectedYieldAmount(), response.getYieldUnit()));
        return response;
    }

    /**
     * 根据当前有效种植批次和预采日期生成用户端状态，避免多个前端各自推断导致口径不一致。
     */
    private void applyClientPlantingStatus(ClientPlotListResponse response, Plot plot) {
        boolean isPlanting = plot.getCurrentBatchId() != null
                && Integer.valueOf(1).equals(plot.getCurrentBatchStatus());
        if (!isPlanting) {
            response.setPlantingStatus("IDLE");
            response.setPlantingStatusName("空闲中");
            return;
        }
        if (isHarvestReady(plot)) {
            response.setPlantingStatus("HARVEST_READY");
            response.setPlantingStatusName("待采收");
            return;
        }
        response.setPlantingStatus("PLANTING");
        response.setPlantingStatusName("种植中");
    }

    private ClientPlotDetailResponse toClientPlotDetailResponse(Plot plot) {
        ClientPlotDetailResponse response = new ClientPlotDetailResponse();
        response.setId(plot.getId());
        response.setPlotName(defaultText(plot.getPlotName(), "-"));
        response.setPlotAreaValue(plot.getArea());
        response.setPlotAreaUnit(defaultText(plot.getAreaUnit(), "亩"));
        response.setPlotArea(formatAmount(plot.getArea(), response.getPlotAreaUnit()));
        // 地块名称和面积始终可见；没有有效种植批次时，其他种植字段保持空值或“-”。
        boolean idle = plot.getCurrentBatchId() == null || !Integer.valueOf(1).equals(plot.getCurrentBatchStatus());
        response.setIdle(idle);
        if (idle) {
            response.setCropName("-");
            response.setGrowthStageName("-");
            response.setHarvestable(false);
            return response;
        }
        response.setPlotImage(plot.getCurrentCropImage());
        response.setCropName(defaultText(plot.getCurrentCropName(), "-"));
        response.setCropId(plot.getCurrentCropId());
        response.setPlantingTime(plot.getCurrentPlantedAt());
        response.setExpectedHarvestAt(plot.getCurrentExpectedHarvestAt());
        response.setHarvestTime(resolveHarvestTime(plot));
        response.setPlantingDays(resolvePlantingDays(plot));
        response.setGrowthStageName(defaultText(plot.getCurrentGrowthStageName(), "暂无阶段"));
        response.setGrowthStageOrder(plot.getCurrentGrowthStageOrder());
        response.setHarvestable(plot.getCurrentBatchId() != null
                && Integer.valueOf(1).equals(plot.getCurrentBatchStatus())
                && isHarvestReady(plot));
        return response;
    }

    private boolean isHarvestReady(Plot plot) {
        return "成熟期".equals(plot.getCurrentGrowthStageName())
                || plot.getCurrentExpectedHarvestAt() != null
                && !plot.getCurrentExpectedHarvestAt().isAfter(LocalDate.now());
    }

    private EnvironmentData latestEnvironmentData(Long userId, Long plotId) {
        // selectList 已按采集时间倒序排序，取第一条即可作为详情页最新环境快照。
        return environmentDataMapper.selectList(userId, plotId, null, null, null, null, null)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private WaterQualityData latestWaterQualityData(Long userId, Long plotId) {
        // 水质数据同样按采集时间倒序返回，优先用于当前 UI 的水温、EC、PH、溶解氧四个指标。
        return waterQualityDataMapper.selectList(userId, plotId, null, null, null, null, null)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private ClientPlotDetailResponse.EnvironmentMonitor toEnvironmentMonitor(EnvironmentData data) {
        if (data == null) {
            return null;
        }
        return new ClientPlotDetailResponse.EnvironmentMonitor(
                data.getAirTemperature(),
                data.getAirHumidity(),
                data.getWindSpeed(),
                data.getAirPressure(),
                data.getCo2Concentration(),
                data.getPm25(),
                data.getDataStatus(),
                data.getAbnormalDetail(),
                data.getCollectTime()
        );
    }

    private ClientPlotDetailResponse.WaterQualityMonitor toWaterQualityMonitor(WaterQualityData data) {
        if (data == null) {
            return null;
        }
        return new ClientPlotDetailResponse.WaterQualityMonitor(
                data.getWaterTemperature(),
                data.getPh(),
                data.getEcValue(),
                data.getDissolvedOxygen(),
                data.getDataStatus(),
                data.getAbnormalDetail(),
                data.getCollectTime()
        );
    }

    private List<ClientPlotDetailResponse.MonitorItem> toMonitorItems(EnvironmentData environmentData,
                                                                     WaterQualityData waterQualityData) {
        if (waterQualityData != null) {
            return List.of(
                    new ClientPlotDetailResponse.MonitorItem("水温", formatMetric(waterQualityData.getWaterTemperature(), "℃")),
                    new ClientPlotDetailResponse.MonitorItem("电导率", formatMetric(waterQualityData.getEcValue(), " μS/cm")),
                    new ClientPlotDetailResponse.MonitorItem("PH值", formatMetric(waterQualityData.getPh(), "")),
                    new ClientPlotDetailResponse.MonitorItem("溶解氧", formatMetric(waterQualityData.getDissolvedOxygen(), " mg/L"))
            );
        }
        if (environmentData != null) {
            return List.of(
                    new ClientPlotDetailResponse.MonitorItem("温度", formatMetric(environmentData.getAirTemperature(), "℃")),
                    new ClientPlotDetailResponse.MonitorItem("湿度", formatMetric(environmentData.getAirHumidity(), "%")),
                    new ClientPlotDetailResponse.MonitorItem("风速", formatMetric(environmentData.getWindSpeed(), " m/s")),
                    new ClientPlotDetailResponse.MonitorItem("气压", formatMetric(environmentData.getAirPressure(), " hPa"))
            );
        }
        return List.of(
                new ClientPlotDetailResponse.MonitorItem("水温", "-"),
                new ClientPlotDetailResponse.MonitorItem("电导率", "-"),
                new ClientPlotDetailResponse.MonitorItem("PH值", "-"),
                new ClientPlotDetailResponse.MonitorItem("溶解氧", "-")
        );
    }

    /** 将后台任务实体收敛为地块详情所需的最小展示字段，避免暴露无关管理数据。 */
    private ClientPlotDetailResponse.FarmTaskSummary toFarmTaskSummary(FarmTask task) {
        ClientPlotDetailResponse.FarmTaskSummary summary = new ClientPlotDetailResponse.FarmTaskSummary();
        summary.setId(task.getId());
        summary.setTaskTitle(defaultText(task.getTaskTitle(), "农事任务"));
        summary.setStatus(task.getStatus());
        summary.setStatusName(resolveFarmTaskStatus(task.getStatus()));
        summary.setDeadlineTime(task.getDeadlineTime());
        return summary;
    }

    /** 统一任务状态文案，确保地块详情与任务管理页面语义一致。 */
    private String resolveFarmTaskStatus(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 1 -> "未开始";
            case 2 -> "进行中";
            case 3 -> "已完成";
            case 4 -> "已逾期";
            case 5 -> "已取消";
            default -> "未知";
        };
    }

    private ClientPlotDetailResponse.DeviceStatus toDeviceStatus(IotDevice device) {
        ClientPlotDetailResponse.DeviceStatus status = new ClientPlotDetailResponse.DeviceStatus();
        status.setId(device.getId());
        status.setName(defaultText(device.getName(), "未知设备"));
        status.setTypeCode(device.getTypeCode());
        status.setOnlineStatus(device.getOnlineStatus());
        status.setHealthStatus(device.getHealthStatus());
        status.setControlStatus(device.getControlStatus());
        status.setState(resolveDeviceState(device));
        status.setEnabled(Integer.valueOf(1).equals(device.getControlStatus()));
        return status;
    }

    private Integer resolvePlantingDays(Plot plot) {
        if (plot.getCurrentGrownDays() != null) {
            return plot.getCurrentGrownDays();
        }
        LocalDate plantedAt = plot.getCurrentPlantedAt();
        if (plantedAt == null) {
            return 0;
        }
        // 数据库没有维护 grown_days 时按种植日期实时计算，至少返回 0，避免未来日期导致负数展示。
        return Math.max(0, (int) ChronoUnit.DAYS.between(plantedAt, LocalDate.now()));
    }

    private LocalDate resolveHarvestTime(Plot plot) {
        return plot.getCurrentActualHarvestAt() != null
                ? plot.getCurrentActualHarvestAt()
                : plot.getCurrentExpectedHarvestAt();
    }

    private String formatAmount(BigDecimal amount, String unit) {
        if (amount == null) {
            return "-";
        }
        return amount.stripTrailingZeros().toPlainString() + defaultText(unit, "");
    }

    private String formatMetric(BigDecimal amount, String unit) {
        if (amount == null) {
            return "-";
        }
        return amount.stripTrailingZeros().toPlainString() + defaultText(unit, "");
    }

    private String resolveDeviceState(IotDevice device) {
        if (device.getHealthStatus() != null && device.getHealthStatus() != 0) {
            return "故障";
        }
        if (Integer.valueOf(1).equals(device.getOnlineStatus())) {
            return "在线";
        }
        return "离线";
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
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

    private Farm requireActiveFarm(Long farmId) {
        if (farmId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择所属农场");
        }
        Farm farm = farmMapper.selectById(farmId);
        if (farm == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "所属农场不存在");
        }
        if (farm.getStatus() != null && farm.getStatus() == 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属农场已停用");
        }
        return farm;
    }

    private void validateFarmArea(Plot plot, Farm farm, Long excludePlotId) {
        if (plot.getArea() == null || farm.getTotalArea() == null) {
            return;
        }
        BigDecimal usedArea = plotMapper.selectList(null, null, farm.getId(), null, null, null).stream()
                .filter(item -> excludePlotId == null || !excludePlotId.equals(item.getId()))
                .map(item -> toSquareMeters(item.getArea(), item.getAreaUnit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalArea = usedArea.add(toSquareMeters(plot.getArea(), plot.getAreaUnit()));
        if (totalArea.compareTo(toSquareMeters(farm.getTotalArea(), farm.getAreaUnit())) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块总面积不能超过农场面积");
        }
    }

    private BigDecimal toSquareMeters(BigDecimal area, String unit) {
        if (area == null) {
            return BigDecimal.ZERO;
        }
        String normalizedUnit = StringUtils.hasText(unit) ? unit.trim() : "亩";
        return switch (normalizedUnit) {
            case "平方米", "㎡", "m²" -> area;
            case "公顷", "ha" -> area.multiply(new BigDecimal("10000"));
            case "亩" -> area.multiply(new BigDecimal("666.6666667"));
            default -> throw new BusinessException(ResponseCode.PARAM_ERROR, "面积单位仅支持亩、平方米或公顷");
        };
    }

    private void checkUniquePlotCode(Plot plot) {
        if (StringUtils.hasText(plot.getPlotCode())
                && plotMapper.countByUserIdAndPlotCode(plot.getUserId(), plot.getPlotCode(), plot.getId()) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该用户下地块编号已存在");
        }
    }

    private void checkNoPlantingBatch(Long id) {
        if (plotMapper.countPlantingBatchByPlotId(id) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块已存在种植批次，不能删除");
        }
    }

    private void validateType(Integer type) {
        if (type != null && type != 1 && type != 2 && type != 3) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块类型只能为1、2或3");
        }
    }

    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块状态只能为0或1");
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
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块ID不能为空");
        }
    }
}
