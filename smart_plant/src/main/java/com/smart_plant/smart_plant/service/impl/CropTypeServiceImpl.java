package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.CropType;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CropTypeMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.CropTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 作物分类业务实现类，处理字段校验、父分类校验和唯一性校验。
 */
@Service
@RequiredArgsConstructor
public class CropTypeServiceImpl implements CropTypeService {

    private final CropTypeMapper cropTypeMapper;

    /**
     * 新增作物分类，要求同一父分类下分类名称不能重复。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CropType addCropType(CropType cropType) {
        validateCreateCropType(cropType);
        normalizeDefaults(cropType);
        checkParentExists(cropType.getParentId(), null);
        checkTypeNameUnique(cropType);
        cropTypeMapper.insert(cropType);
        return cropTypeMapper.selectById(cropType.getId());
    }

    /**
     * 编辑作物分类，按非空字段动态更新。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CropType updateCropType(CropType cropType) {
        if (cropType == null || cropType.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "作物分类ID不能为空");
        }
        CropType oldCropType = cropTypeMapper.selectById(cropType.getId());
        if (oldCropType == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物分类不存在");
        }
        checkParentExists(cropType.getParentId(), cropType.getId());
        checkTypeNameUnique(cropType);
        validateStatus(cropType.getStatus());
        int rows = cropTypeMapper.updateById(cropType);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "作物分类修改失败");
        }
        return cropTypeMapper.selectById(cropType.getId());
    }

    /**
     * 删除作物分类。数据库外键会将关联作物的 type_id 置空。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCropType(Long id) {
        requireId(id);
        int rows = cropTypeMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物分类不存在");
        }
    }

    /**
     * 批量删除作物分类。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCropTypes(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的作物分类");
        }
        return cropTypeMapper.deleteBatchByIds(ids);
    }

    /**
     * 修改作物分类状态，status=1 表示启用，status=0 表示禁用。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireId(id);
        validateStatus(status);
        int rows = cropTypeMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物分类不存在");
        }
    }

    /**
     * 根据ID查询作物分类详情。
     */
    @Override
    public CropType getCropTypeById(Long id) {
        requireId(id);
        CropType cropType = cropTypeMapper.selectById(id);
        if (cropType == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "作物分类不存在");
        }
        return cropType;
    }

    /**
     * 分页查询作物分类列表，支持分类名称、父分类和状态筛选。
     */
    @Override
    public PageInfo<CropType> listCropTypes(String typeName, Long parentId, Integer status,
                                            Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(cropTypeMapper.selectList(typeName, parentId, status));
    }

    /** 新增时的必填字段校验。 */
    private void validateCreateCropType(CropType cropType) {
        if (cropType == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "作物分类信息不能为空");
        }
        if (!StringUtils.hasText(cropType.getTypeName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "分类名称不能为空");
        }
        validateStatus(cropType.getStatus());
    }

    /** 补齐默认状态。 */
    private void normalizeDefaults(CropType cropType) {
        if (cropType.getStatus() == null) {
            cropType.setStatus(1);
        }
    }

    /** 校验父分类是否存在，并避免将自己设置为自己的父分类。 */
    private void checkParentExists(Long parentId, Long currentId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(currentId)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "父分类不能选择自身");
        }
        if (cropTypeMapper.selectById(parentId) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "父分类不存在");
        }
    }

    /** 校验同一父分类下分类名称唯一。 */
    private void checkTypeNameUnique(CropType cropType) {
        if (!StringUtils.hasText(cropType.getTypeName())) {
            return;
        }
        int count = cropTypeMapper.countByTypeNameAndParentId(
                cropType.getTypeName().trim(),
                cropType.getParentId(),
                cropType.getId()
        );
        if (count > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "同一父分类下分类名称已存在");
        }
        cropType.setTypeName(cropType.getTypeName().trim());
    }

    /** 状态字段取值校验。 */
    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "作物分类状态只能为0或1");
        }
    }

    /** 公共ID非空校验。 */
    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "作物分类ID不能为空");
        }
    }
}
