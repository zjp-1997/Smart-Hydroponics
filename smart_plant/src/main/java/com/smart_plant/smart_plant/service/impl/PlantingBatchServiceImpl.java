package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.PlantingBatch;
import com.smart_plant.smart_plant.dto.PlotHarvestRequest;
import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CropMapper;
import com.smart_plant.smart_plant.mapper.GrowthStageMapper;
import com.smart_plant.smart_plant.mapper.PlantingBatchMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.PlantingBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlantingBatchServiceImpl implements PlantingBatchService {

    private final PlantingBatchMapper plantingBatchMapper;

    private final PlotMapper plotMapper;

    private final CropMapper cropMapper;

    private final UserMapper userMapper;

    private final GrowthStageMapper growthStageMapper;

    private final DataPermissionService dataPermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlantingBatch addPlantingBatch(PlantingBatch plantingBatch) {
        if (!dataPermissionService.isAdmin() && plantingBatch != null) {
            plantingBatch.setUserId(dataPermissionService.currentUser().getId());
        }
        validateCreatePlantingBatch(plantingBatch);
        dataPermissionService.requireAgriculturalOperator(plantingBatch.getUserId());
        normalizeDefaults(plantingBatch);
        validateHarvestCompletion(plantingBatch);
        Plot plot = checkReferences(plantingBatch);
        fillPlantingAreaFromPlot(plantingBatch, plot);
        checkSingleActiveBatch(plantingBatch);
        checkUniqueBatchNo(plantingBatch);
        plantingBatchMapper.insert(plantingBatch);
        return plantingBatchMapper.selectById(plantingBatch.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlantingBatch updatePlantingBatch(PlantingBatch plantingBatch) {
        if (plantingBatch == null || plantingBatch.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "种植批次ID不能为空");
        }
        PlantingBatch oldBatch = plantingBatchMapper.selectById(plantingBatch.getId());
        if (oldBatch == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "种植批次不存在");
        }
        dataPermissionService.requireAgriculturalOperator(oldBatch.getUserId());
        if (!dataPermissionService.isAdmin()) {
            plantingBatch.setUserId(oldBatch.getUserId());
        }
        if (plantingBatch.getUserId() == null) {
            plantingBatch.setUserId(oldBatch.getUserId());
        }
        mergeUpdateFields(plantingBatch, oldBatch);
        validateUpdatePlantingBatch(plantingBatch);
        validateHarvestCompletion(plantingBatch);
        Plot plot = checkReferences(plantingBatch);
        fillPlantingAreaFromPlot(plantingBatch, plot);
        checkSingleActiveBatch(plantingBatch);
        checkUniqueBatchNo(plantingBatch);
        int rows = plantingBatchMapper.updateById(plantingBatch);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "种植批次修改失败");
        }
        return plantingBatchMapper.selectById(plantingBatch.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlantingBatch(Long id) {
        requireId(id);
        dataPermissionService.requireAgriculturalOperator(getPlantingBatchById(id).getUserId());
        int rows = plantingBatchMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "种植批次不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePlantingBatches(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的种植批次");
        }
        for (Long id : ids) {
            requireId(id);
            dataPermissionService.requireAgriculturalOperator(getPlantingBatchById(id).getUserId());
        }
        return plantingBatchMapper.deleteBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireId(id);
        PlantingBatch plantingBatch = getPlantingBatchById(id);
        dataPermissionService.requireAgriculturalOperator(plantingBatch.getUserId());
        validateStatus(status);
        plantingBatch.setStatus(status);
        validateHarvestCompletion(plantingBatch);
        checkSingleActiveBatch(plantingBatch);
        int rows = plantingBatchMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "种植批次不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlantingBatch harvestActiveBatch(Long plotId, PlotHarvestRequest request) {
        if (plotId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块ID不能为空");
        }
        validateHarvestRequest(request);
        PlantingBatch batch = plantingBatchMapper.selectActiveByPlotIdForUpdate(plotId);
        if (batch == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该地块当前没有种植中的批次");
        }
        dataPermissionService.requireAgriculturalOperator(batch.getUserId());
        if (batch.getPlantedAt() != null && request.getActualHarvestAt().isBefore(batch.getPlantedAt())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "实际采收日期不能早于种植日期");
        }
        if (request.getActualHarvestAt().isAfter(LocalDate.now())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "实际采收日期不能晚于当前日期");
        }
        String yieldUnit = StringUtils.hasText(request.getYieldUnit()) ? request.getYieldUnit().trim()
                : StringUtils.hasText(batch.getYieldUnit()) ? batch.getYieldUnit().trim() : "kg";
        int rows = plantingBatchMapper.completeHarvest(batch.getId(), request.getHarvester().trim(),
                request.getYieldAmount(), yieldUnit, request.getActualHarvestAt());
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "采收失败，请刷新后重试");
        }
        return plantingBatchMapper.selectById(batch.getId());
    }

    @Override
    public PlantingBatch getPlantingBatchById(Long id) {
        requireId(id);
        PlantingBatch plantingBatch = plantingBatchMapper.selectById(id);
        if (plantingBatch == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "种植批次不存在");
        }
        dataPermissionService.requireOwnedResource(plantingBatch.getUserId());
        return plantingBatch;
    }

    @Override
    public PageInfo<PlantingBatch> listPlantingBatches(Long plotId, Long cropId, Long userId,
                                                       String batchNo, Integer status,
                                                       Integer pageNum, Integer pageSize) {
        validateStatus(status);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = dataPermissionService.restrictUserId(userId);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(plantingBatchMapper.selectList(plotId, cropId, scopedUserId, batchNo, status));
    }

    private void validateCreatePlantingBatch(PlantingBatch plantingBatch) {
        if (plantingBatch == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "种植批次信息不能为空");
        }
        if (plantingBatch.getPlotId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属地块不能为空");
        }
        if (plantingBatch.getCropId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "作物不能为空");
        }
        if (plantingBatch.getUserId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属用户不能为空");
        }
        validateUpdatePlantingBatch(plantingBatch);
    }

    private void validateUpdatePlantingBatch(PlantingBatch plantingBatch) {
        validateStatus(plantingBatch.getStatus());
        validateGrowthStage(plantingBatch.getGrowthStage());
        validateNonNegative(plantingBatch.getPlantingArea(), "种植面积不能小于0");
        validateNonNegative(plantingBatch.getExpectedYieldAmount(), "预计产量不能小于0");
        validateNonNegative(plantingBatch.getYieldAmount(), "实际产量不能小于0");
        if (plantingBatch.getGrownDays() != null && plantingBatch.getGrownDays() < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "已生长天数不能小于0");
        }
        if (plantingBatch.getPlantedAt() != null
                && plantingBatch.getExpectedHarvestAt() != null
                && plantingBatch.getExpectedHarvestAt().isBefore(plantingBatch.getPlantedAt())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "预计采收日期不能早于种植日期");
        }
    }

    private void normalizeDefaults(PlantingBatch plantingBatch) {
        if (!StringUtils.hasText(plantingBatch.getAreaUnit())) {
            plantingBatch.setAreaUnit("亩");
        }
        if (!StringUtils.hasText(plantingBatch.getYieldUnit())) {
            plantingBatch.setYieldUnit("kg");
        }
        if (plantingBatch.getGrowthStage() == null) {
            plantingBatch.setGrowthStage(1);
        }
        if (plantingBatch.getStatus() == null) {
            plantingBatch.setStatus(1);
        }
        if (StringUtils.hasText(plantingBatch.getBatchNo())) {
            plantingBatch.setBatchNo(plantingBatch.getBatchNo().trim());
        }
    }

    private Plot checkReferences(PlantingBatch plantingBatch) {
        Plot plot = null;
        if (plantingBatch.getPlotId() != null) {
            plot = plotMapper.selectById(plantingBatch.getPlotId());
            if (plot == null) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "所属地块不存在");
            }
        }
        if (plantingBatch.getCropId() != null && cropMapper.selectById(plantingBatch.getCropId()) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物不存在");
        }
        if (plantingBatch.getUserId() != null && userMapper.selectById(plantingBatch.getUserId()) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "所属用户不存在");
        }
        if (plot != null
                && plantingBatch.getUserId() != null
                && !plantingBatch.getUserId().equals(plot.getUserId())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属用户必须与地块归属用户一致");
        }
        if (plantingBatch.getGrowthStageId() != null
                && growthStageMapper.selectById(plantingBatch.getGrowthStageId()) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物生长期不存在");
        }
        return plot;
    }

    private void mergeUpdateFields(PlantingBatch plantingBatch, PlantingBatch oldBatch) {
        if (plantingBatch.getPlotId() == null) plantingBatch.setPlotId(oldBatch.getPlotId());
        if (plantingBatch.getCropId() == null) plantingBatch.setCropId(oldBatch.getCropId());
        if (plantingBatch.getStatus() == null) plantingBatch.setStatus(oldBatch.getStatus());
        if (plantingBatch.getPlantingArea() == null) plantingBatch.setPlantingArea(oldBatch.getPlantingArea());
        if (!StringUtils.hasText(plantingBatch.getAreaUnit())) plantingBatch.setAreaUnit(oldBatch.getAreaUnit());
        if (plantingBatch.getActualHarvestAt() == null) plantingBatch.setActualHarvestAt(oldBatch.getActualHarvestAt());
        if (plantingBatch.getYieldAmount() == null) plantingBatch.setYieldAmount(oldBatch.getYieldAmount());
    }

    private void fillPlantingAreaFromPlot(PlantingBatch plantingBatch, Plot plot) {
        if (plot != null && (Integer.valueOf(1).equals(plantingBatch.getStatus())
                || plantingBatch.getPlantingArea() == null)) {
            plantingBatch.setPlantingArea(plot.getArea());
            plantingBatch.setAreaUnit(StringUtils.hasText(plot.getAreaUnit()) ? plot.getAreaUnit() : "亩");
        }
    }

    private void checkSingleActiveBatch(PlantingBatch plantingBatch) {
        if (Integer.valueOf(1).equals(plantingBatch.getStatus())
                && plantingBatchMapper.countActiveByPlotId(plantingBatch.getPlotId(), plantingBatch.getId()) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "一个地块只能存在一个种植中批次");
        }
    }

    private void validateHarvestCompletion(PlantingBatch plantingBatch) {
        if (!Integer.valueOf(2).equals(plantingBatch.getStatus())) {
            return;
        }
        if (plantingBatch.getActualHarvestAt() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "已采收批次必须填写实际采收日期");
        }
        if (plantingBatch.getYieldAmount() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "已采收批次必须填写实际产量");
        }
    }

    private void validateHarvestRequest(PlotHarvestRequest request) {
        if (request == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "采收信息不能为空");
        }
        if (!StringUtils.hasText(request.getHarvester())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "采收人不能为空");
        }
        if (request.getHarvester().trim().length() > 50) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "采收人长度不能超过50个字符");
        }
        if (request.getYieldAmount() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "实际产量不能为空");
        }
        validateNonNegative(request.getYieldAmount(), "实际产量不能小于0");
        if (StringUtils.hasText(request.getYieldUnit()) && request.getYieldUnit().trim().length() > 20) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "产量单位长度不能超过20个字符");
        }
        if (request.getActualHarvestAt() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "实际采收日期不能为空");
        }
    }

    private void checkUniqueBatchNo(PlantingBatch plantingBatch) {
        if (StringUtils.hasText(plantingBatch.getBatchNo())
                && plantingBatchMapper.countByUserIdAndBatchNo(
                plantingBatch.getUserId(), plantingBatch.getBatchNo(), plantingBatch.getId()) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该用户下批次编号已存在");
        }
    }

    private void validateStatus(Integer status) {
        if (status != null && status != 1 && status != 2 && status != 3 && status != 4) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "种植批次状态只能为1、2、3或4");
        }
    }

    private void validateGrowthStage(Integer growthStage) {
        if (growthStage != null && growthStage != 1 && growthStage != 2 && growthStage != 3 && growthStage != 4) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "生长阶段只能为1、2、3或4");
        }
    }

    private void validateNonNegative(BigDecimal value, String message) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "种植批次ID不能为空");
        }
    }
}
