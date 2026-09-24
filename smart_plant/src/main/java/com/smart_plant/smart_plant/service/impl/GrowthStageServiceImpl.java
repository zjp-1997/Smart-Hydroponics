package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.GrowthStage;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CropMapper;
import com.smart_plant.smart_plant.mapper.GrowthStageMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.GrowthStageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 作物生长期业务实现类，处理字段校验、作物校验和阶段顺序唯一性校验。
 */
@Service
@RequiredArgsConstructor
public class GrowthStageServiceImpl implements GrowthStageService {

    private final GrowthStageMapper growthStageMapper;

    private final CropMapper cropMapper;

    /**
     * 新增作物生长期，要求作物存在且同一作物下阶段顺序不能重复。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrowthStage addGrowthStage(GrowthStage growthStage) {
        validateCreateGrowthStage(growthStage);
        normalizeDefaults(growthStage);
        checkCropExists(growthStage.getCropId());
        checkStageOrderUnique(growthStage.getCropId(), growthStage.getStageOrder(), null);
        growthStageMapper.insert(growthStage);
        return growthStageMapper.selectById(growthStage.getId());
    }

    /**
     * 编辑作物生长期，按非空字段动态更新。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrowthStage updateGrowthStage(GrowthStage growthStage) {
        if (growthStage == null || growthStage.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "生长期阶段ID不能为空");
        }
        GrowthStage oldGrowthStage = growthStageMapper.selectById(growthStage.getId());
        if (oldGrowthStage == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物生长期不存在");
        }
        if (growthStage.getCropId() != null) {
            checkCropExists(growthStage.getCropId());
        }
        validateStageFields(growthStage);
        validateStatus(growthStage.getStatus());
        Long effectiveCropId = growthStage.getCropId() == null ? oldGrowthStage.getCropId() : growthStage.getCropId();
        Integer effectiveStageOrder = growthStage.getStageOrder() == null
                ? oldGrowthStage.getStageOrder()
                : growthStage.getStageOrder();
        checkStageOrderUnique(effectiveCropId, effectiveStageOrder, growthStage.getId());
        int rows = growthStageMapper.updateById(growthStage);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "作物生长期修改失败");
        }
        return growthStageMapper.selectById(growthStage.getId());
    }

    /**
     * 删除作物生长期。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGrowthStage(Long id) {
        requireId(id);
        int rows = growthStageMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物生长期不存在");
        }
    }

    /**
     * 批量删除作物生长期。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteGrowthStages(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的作物生长期");
        }
        return growthStageMapper.deleteBatchByIds(ids);
    }

    /**
     * 修改生长期状态，status=1 表示启用，status=0 表示禁用。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireId(id);
        validateStatus(status);
        int rows = growthStageMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物生长期不存在");
        }
    }

    /**
     * 根据ID查询作物生长期详情。
     */
    @Override
    public GrowthStage getGrowthStageById(Long id) {
        requireId(id);
        GrowthStage growthStage = growthStageMapper.selectById(id);
        if (growthStage == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物生长期不存在");
        }
        return growthStage;
    }

    /**
     * 分页查询作物生长期列表，支持作物、阶段名称和状态筛选。
     */
    @Override
    public PageInfo<GrowthStage> listGrowthStages(Long cropId, String stageName, Integer status,
                                                  Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(growthStageMapper.selectList(cropId, stageName, status));
    }

    /** 新增时的必填字段校验。 */
    private void validateCreateGrowthStage(GrowthStage growthStage) {
        if (growthStage == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "作物生长期信息不能为空");
        }
        if (growthStage.getCropId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "作物ID不能为空");
        }
        if (!StringUtils.hasText(growthStage.getStageName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "阶段名称不能为空");
        }
        if (growthStage.getStageOrder() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "阶段顺序不能为空");
        }
        validateStageFields(growthStage);
        validateStatus(growthStage.getStatus());
    }

    /** 补齐默认字段并清理文本。 */
    private void normalizeDefaults(GrowthStage growthStage) {
        if (growthStage.getStatus() == null) {
            growthStage.setStatus(1);
        }
        growthStage.setStageName(growthStage.getStageName().trim());
        if (StringUtils.hasText(growthStage.getStageCode())) {
            growthStage.setStageCode(growthStage.getStageCode().trim());
        }
        if (growthStage.getDuration() == null
                && growthStage.getStartDay() != null
                && growthStage.getEndDay() != null
                && growthStage.getEndDay() >= growthStage.getStartDay()) {
            growthStage.setDuration(growthStage.getEndDay() - growthStage.getStartDay() + 1);
        }
    }

    /** 校验作物是否存在。 */
    private void checkCropExists(Long cropId) {
        if (cropMapper.selectById(cropId) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物信息不存在");
        }
    }

    /** 校验同一作物下阶段顺序唯一。 */
    private void checkStageOrderUnique(Long cropId, Integer stageOrder, Long excludeId) {
        if (cropId == null || stageOrder == null) {
            return;
        }
        int count = growthStageMapper.countByCropIdAndStageOrder(cropId, stageOrder, excludeId);
        if (count > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "同一作物下阶段顺序已存在");
        }
    }

    /** 校验阶段字段范围。 */
    private void validateStageFields(GrowthStage growthStage) {
        if (growthStage.getStageOrder() != null && growthStage.getStageOrder() < 1) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "阶段顺序必须大于0");
        }
        validateNonNegative(growthStage.getStartDay(), "开始天数不能小于0");
        validateNonNegative(growthStage.getEndDay(), "结束天数不能小于0");
        validateNonNegative(growthStage.getDuration(), "持续天数不能小于0");
        validateNonNegative(growthStage.getWaterIntervalDays(), "建议浇水间隔天数不能小于0");
        validateNonNegative(growthStage.getFertilizerIntervalDays(), "建议施肥间隔天数不能小于0");
        if (growthStage.getStartDay() != null
                && growthStage.getEndDay() != null
                && growthStage.getEndDay() < growthStage.getStartDay()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "结束天数不能小于开始天数");
        }
        validateDecimalRange(growthStage.getLightHours(), BigDecimal.ZERO, new BigDecimal("24.0"), "光照时长必须在0到24小时之间");
        validateRangePair(growthStage.getTempMin(), growthStage.getTempMax(), "最高温度不能小于最低温度");
        validateDecimalRange(growthStage.getHumidityMin(), BigDecimal.ZERO, new BigDecimal("100.00"), "最低湿度必须在0到100之间");
        validateDecimalRange(growthStage.getHumidityMax(), BigDecimal.ZERO, new BigDecimal("100.00"), "最高湿度必须在0到100之间");
        validateRangePair(growthStage.getHumidityMin(), growthStage.getHumidityMax(), "最高湿度不能小于最低湿度");
        validateDecimalRange(growthStage.getPhMin(), BigDecimal.ZERO, new BigDecimal("14.0"), "最低PH必须在0到14之间");
        validateDecimalRange(growthStage.getPhMax(), BigDecimal.ZERO, new BigDecimal("14.0"), "最高PH必须在0到14之间");
        validateRangePair(growthStage.getPhMin(), growthStage.getPhMax(), "最高PH不能小于最低PH");
        validateDecimalRange(growthStage.getEcMin(), BigDecimal.ZERO, null, "最低EC值不能小于0");
        validateDecimalRange(growthStage.getEcMax(), BigDecimal.ZERO, null, "最高EC值不能小于0");
        validateRangePair(growthStage.getEcMin(), growthStage.getEcMax(), "最高EC值不能小于最低EC值");
    }

    /** 校验整数非负。 */
    private void validateNonNegative(Integer value, String message) {
        if (value != null && value < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
    }

    /** 校验小数范围。 */
    private void validateDecimalRange(BigDecimal value, BigDecimal min, BigDecimal max, String message) {
        if (value == null) {
            return;
        }
        if (min != null && value.compareTo(min) < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
        if (max != null && value.compareTo(max) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
    }

    /** 校验范围字段的最大值不能小于最小值。 */
    private void validateRangePair(BigDecimal min, BigDecimal max, String message) {
        if (min != null && max != null && max.compareTo(min) < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
    }

    /** 状态字段取值校验。 */
    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "作物生长期状态只能为0或1");
        }
    }

    /** 公共ID非空校验。 */
    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "生长期阶段ID不能为空");
        }
    }
}
