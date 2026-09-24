package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.Crop;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CropMapper;
import com.smart_plant.smart_plant.mapper.CropTypeMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.CropService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 作物信息业务实现类，处理字段校验和分类校验。
 */
@Service
@RequiredArgsConstructor
public class CropServiceImpl implements CropService {

    private final CropMapper cropMapper;

    private final CropTypeMapper cropTypeMapper;

    private final DataPermissionService dataPermissionService;

    /**
     * 新增作物信息，要求作物名称不能为空，作物类型存在。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Crop addCrop(Crop crop) {
        if (!dataPermissionService.isAdmin() && crop != null) {
            crop.setUserId(dataPermissionService.currentUser().getId());
        }
        validateCreateCrop(crop);
        normalizeDefaults(crop);
        checkTypeExists(crop.getTypeId());
        // 编码只由服务端根据数据库主键生成，客户端即使传值也不会生效。
        crop.setCropCode(null);
        cropMapper.insert(crop);
        if (crop.getId() == null) {
            throw new BusinessException(ResponseCode.FAIL, "作物编码生成失败");
        }
        crop.setCropCode(formatCropCode(crop.getId()));
        if (cropMapper.updateCropCode(crop.getId(), crop.getCropCode()) == 0) {
            throw new BusinessException(ResponseCode.FAIL, "作物编码生成失败");
        }
        return cropMapper.selectById(crop.getId());
    }

    /**
     * 编辑作物信息，按非空字段动态更新。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Crop updateCrop(Crop crop) {
        if (crop == null || crop.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "作物ID不能为空");
        }
        Crop oldCrop = cropMapper.selectById(crop.getId());
        if (oldCrop == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物信息不存在");
        }
        requireWritableCrop(oldCrop);
        if (!dataPermissionService.isAdmin()) {
            crop.setUserId(oldCrop.getUserId());
        }
        checkTypeExists(crop.getTypeId());
        validateStatus(crop.getStatus());
        validateNumericFields(crop);
        int rows = cropMapper.updateById(crop);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "作物信息修改失败");
        }
        return cropMapper.selectById(crop.getId());
    }

    /**
     * 删除作物信息。若已被种植批次、病虫害等业务引用，数据库外键会阻止删除。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCrop(Long id) {
        requireId(id);
        Crop crop = cropMapper.selectById(id);
        if (crop == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物信息不存在");
        }
        requireWritableCrop(crop);
        int rows = cropMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物信息不存在");
        }
    }

    /**
     * 批量删除作物信息。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCrops(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的作物信息");
        }
        for (Long id : ids) {
            requireId(id);
            Crop crop = cropMapper.selectById(id);
            if (crop == null) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "作物信息不存在");
            }
            requireWritableCrop(crop);
        }
        return cropMapper.deleteBatchByIds(ids);
    }

    /**
     * 修改作物状态，status=1 表示启用，status=0 表示禁用。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireId(id);
        validateStatus(status);
        Crop crop = cropMapper.selectById(id);
        if (crop == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物信息不存在");
        }
        requireWritableCrop(crop);
        int rows = cropMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物信息不存在");
        }
    }

    /**
     * 根据ID查询作物详情。
     */
    @Override
    public Crop getCropById(Long id) {
        requireId(id);
        Crop crop = cropMapper.selectById(id);
        if (crop == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物信息不存在");
        }
        requireVisibleCrop(crop);
        return crop;
    }

    /**
     * 分页查询作物列表，支持作物名称、作物类型和状态筛选。
     */
    @Override
    public PageInfo<Crop> listCrops(String cropName, Long typeId, Integer status,
                                    Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(cropMapper.selectList(cropName, typeId, status, scopedUserId));
    }

    /** 新增时的必填字段校验。 */
    private void validateCreateCrop(Crop crop) {
        if (crop == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "作物信息不能为空");
        }
        if (!StringUtils.hasText(crop.getCropName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "作物名称不能为空");
        }
        checkTypeExists(crop.getTypeId());
        validateStatus(crop.getStatus());
        validateNumericFields(crop);
    }

    /** 补齐默认字段。 */
    private void normalizeDefaults(Crop crop) {
        if (crop.getStatus() == null) {
            crop.setStatus(1);
        }
        crop.setCropName(crop.getCropName().trim());
    }

    /** 校验作物类型是否存在。 */
    private void checkTypeExists(Long typeId) {
        if (typeId == null) {
            return;
        }
        if (cropTypeMapper.selectById(typeId) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物类型不存在");
        }
    }

    /** 状态字段取值校验。 */
    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "作物状态只能为0或1");
        }
    }

    /** 数值字段范围校验。 */
    private void validateNumericFields(Crop crop) {
        if (crop.getGrowthDays() != null && crop.getGrowthDays() < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "推荐生长周期不能小于0");
        }
    }

    /** 公共ID非空校验。 */
    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "作物ID不能为空");
        }
    }

    private void requireVisibleCrop(Crop crop) {
        if (dataPermissionService.isAdmin()) {
            return;
        }
        Long currentUserId = dataPermissionService.currentUser().getId();
        if (crop.getId() == null || cropMapper.countVisibleByUserId(crop.getId(), currentUserId) == 0) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "无权限访问该作物信息");
        }
    }

    private void requireWritableCrop(Crop crop) {
        if (dataPermissionService.isAdmin()) {
            return;
        }
        Long currentUserId = dataPermissionService.currentUser().getId();
        if (crop.getId() == null || cropMapper.countOwnedByUserId(crop.getId(), currentUserId) == 0) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "只能维护自己创建的作物信息");
        }
    }

    private String formatCropCode(Long cropId) {
        return String.format(Locale.ROOT, "C%03d", cropId);
    }
}
