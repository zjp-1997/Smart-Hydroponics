package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.DiseasePest;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CropTypeMapper;
import com.smart_plant.smart_plant.mapper.DiseaseControlMapper;
import com.smart_plant.smart_plant.mapper.DiseasePestMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DiseasePestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 病虫害基础信息业务实现类。
 *
 * <p>该实现类负责字段校验、作物类型存在性校验、同作物类型同名校验以及详情防治措施组装。</p>
 */
@Service
@RequiredArgsConstructor
public class DiseasePestServiceImpl implements DiseasePestService {

    /** 病虫害基础信息 Mapper，用于 disease_pest 表读写。 */
    private final DiseasePestMapper diseasePestMapper;

    /** 防治措施 Mapper，用于病虫害详情中组装预防/治疗措施。 */
    private final DiseaseControlMapper diseaseControlMapper;

    /** 作物类型 Mapper，用于校验 crop_type_id 是否存在。 */
    private final CropTypeMapper cropTypeMapper;

    /** 新增病虫害基础信息，并返回数据库中的完整记录。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiseasePest addDiseasePest(DiseasePest diseasePest) {
        validateCreateDiseasePest(diseasePest);
        normalizeDefaults(diseasePest);
        checkCropTypeExists(diseasePest.getCropTypeId());
        checkUniqueName(diseasePest);
        diseasePestMapper.insert(diseasePest);
        return diseasePestMapper.selectById(diseasePest.getId());
    }

    /** 删除单条病虫害基础信息，防治措施由数据库外键级联删除。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDiseasePest(Long id) {
        requireId(id);
        int rows = diseasePestMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "病虫害信息不存在");
        }
    }

    /** 批量删除病虫害基础信息。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDiseasePests(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的病虫害信息");
        }
        return diseasePestMapper.deleteBatchByIds(ids);
    }

    /** 修改病虫害基础信息，并返回更新后的完整记录。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiseasePest updateDiseasePest(DiseasePest diseasePest) {
        if (diseasePest == null || diseasePest.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "病虫害ID不能为空");
        }
        DiseasePest oldDiseasePest = diseasePestMapper.selectById(diseasePest.getId());
        if (oldDiseasePest == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "病虫害信息不存在");
        }
        if (diseasePest.getCropTypeId() != null) {
            checkCropTypeExists(diseasePest.getCropTypeId());
        } else {
            diseasePest.setCropTypeId(oldDiseasePest.getCropTypeId());
        }
        if (!StringUtils.hasText(diseasePest.getName())) {
            diseasePest.setName(oldDiseasePest.getName());
        } else {
            diseasePest.setName(diseasePest.getName().trim());
        }
        validateUpdateDiseasePest(diseasePest);
        checkUniqueName(diseasePest);
        int rows = diseasePestMapper.updateById(diseasePest);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "病虫害信息修改失败");
        }
        return diseasePestMapper.selectById(diseasePest.getId());
    }

    /** 单独修改状态，方便列表页启用或停用记录。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireId(id);
        validateStatus(status);
        int rows = diseasePestMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "病虫害信息不存在");
        }
    }

    /** 查询病虫害详情，并附带该病虫害下的预防和治疗措施。 */
    @Override
    public DiseasePest getDiseasePestById(Long id) {
        requireId(id);
        DiseasePest diseasePest = diseasePestMapper.selectById(id);
        if (diseasePest == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "病虫害信息不存在");
        }
        diseasePest.setControls(diseaseControlMapper.selectList(id, null, null, null, null));
        return diseasePest;
    }

    /**
     * 查询 farm 用户端可见的病虫害详情。
     * 停用的知识库条目按不存在处理，防止后台下架内容继续被客户端访问；
     * 防治措施同样只保留启用记录，避免把尚未发布的方案展示给用户。
     */
    @Override
    public DiseasePest getEnabledDiseasePestById(Long id) {
        DiseasePest diseasePest = getDiseasePestById(id);
        if (!Integer.valueOf(1).equals(diseasePest.getStatus())) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "病虫害信息不存在或已停用");
        }
        diseasePest.setControls(diseasePest.getControls().stream()
                .filter(control -> Integer.valueOf(1).equals(control.getStatus()))
                .toList());
        return diseasePest;
    }

    /** 分页查询病虫害基础信息列表，并复用 PageHelper 生成分页信息。 */
    @Override
    public PageInfo<DiseasePest> listDiseasePests(String name, Long cropTypeId, Integer type,
                                                  Integer status, Integer pageNum, Integer pageSize) {
        validateType(type);
        validateStatus(status);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(diseasePestMapper.selectList(name, cropTypeId, type, status));
    }

    /** 校验新增时的必填字段。 */
    private void validateCreateDiseasePest(DiseasePest diseasePest) {
        if (diseasePest == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "病虫害信息不能为空");
        }
        if (!StringUtils.hasText(diseasePest.getName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "病虫害名称不能为空");
        }
        if (diseasePest.getType() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "病虫害类型不能为空");
        }
        validateUpdateDiseasePest(diseasePest);
    }

    /** 校验新增和修改共用的字段范围。 */
    private void validateUpdateDiseasePest(DiseasePest diseasePest) {
        validateType(diseasePest.getType());
        validateStatus(diseasePest.getStatus());
        validateSortOrder(diseasePest.getSortOrder());
        normalizeImageUrls(diseasePest);
        validateLength(diseasePest.getAffectedCrops(), 255, "危害作物");
        validateLength(diseasePest.getOccurrencePeriod(), 100, "发生时期");
    }

    /** 为新增记录补齐默认值，并清理名称两侧空白。 */
    private void normalizeDefaults(DiseasePest diseasePest) {
        diseasePest.setName(diseasePest.getName().trim());
        if (diseasePest.getStatus() == null) {
            diseasePest.setStatus(1);
        }
        if (diseasePest.getSortOrder() == null) {
            diseasePest.setSortOrder(0);
        }
    }

    /** 校验关联作物类型是否存在，crop_type_id 为空时表示通用病虫害。 */
    private void checkCropTypeExists(Long cropTypeId) {
        if (cropTypeId != null && cropTypeMapper.selectById(cropTypeId) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "关联作物类型不存在");
        }
    }

    /** 校验同一作物类型下是否已有同名病虫害。 */
    private void checkUniqueName(DiseasePest diseasePest) {
        if (StringUtils.hasText(diseasePest.getName())
                && diseasePestMapper.countByCropTypeIdAndName(
                diseasePest.getCropTypeId(), diseasePest.getName().trim(), diseasePest.getId()) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "同一作物类型下病虫害名称已存在");
        }
    }

    /** 校验病虫害类型枚举值。 */
    private void validateType(Integer type) {
        if (type != null && type != 1 && type != 2 && type != 3) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "病虫害类型只能为1、2或3");
        }
    }

    /** 校验状态枚举值。 */
    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "状态只能为0或1");
        }
    }

    /** 校验排序值，避免负数使记录意外插入列表最前方。 */
    private void validateSortOrder(Integer sortOrder) {
        if (sortOrder != null && sortOrder < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "展示顺序不能小于0");
        }
    }

    /** 清理详情图片数组中的空值和两侧空白，保证 JSON 数据可直接被前端消费。 */
    private void normalizeImageUrls(DiseasePest diseasePest) {
        if (diseasePest.getImageUrls() == null) {
            return;
        }
        diseasePest.setImageUrls(diseasePest.getImageUrls().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList());
        if (diseasePest.getImageUrls().size() > 8) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "详情图片最多上传8张");
        }
        diseasePest.getImageUrls().forEach(url -> validateLength(url, 500, "详情图片地址"));
    }

    /** 对数据库定长字段执行统一边界校验，防止绕过前端直接提交超长文本。 */
    private void validateLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(ResponseCode.PARAM_ERROR,
                    fieldName + "不能超过" + maxLength + "个字符");
        }
    }

    /** 公共ID非空校验。 */
    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "病虫害ID不能为空");
        }
    }
}
