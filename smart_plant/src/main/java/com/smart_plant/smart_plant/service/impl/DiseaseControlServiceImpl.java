package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.DiseaseControl;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.DiseaseControlMapper;
import com.smart_plant.smart_plant.mapper.DiseasePestMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DiseaseControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 病虫害防治措施业务实现类。
 *
 * <p>该实现类负责参数校验、默认值补齐、外键存在性校验和分页查询。</p>
 */
@Service
@RequiredArgsConstructor
public class DiseaseControlServiceImpl implements DiseaseControlService {

    /** 防治措施表 Mapper，用于 disease_control 表读写。 */
    private final DiseaseControlMapper diseaseControlMapper;

    /** 病虫害基础信息 Mapper，用于校验 disease_id 是否存在。 */
    private final DiseasePestMapper diseasePestMapper;

    /** 新增防治措施，并返回数据库中的完整记录。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiseaseControl addDiseaseControl(DiseaseControl diseaseControl) {
        validateCreateDiseaseControl(diseaseControl);
        normalizeDefaults(diseaseControl);
        checkDiseaseExists(diseaseControl.getDiseaseId());
        diseaseControlMapper.insert(diseaseControl);
        return diseaseControlMapper.selectById(diseaseControl.getId());
    }

    /** 删除单条防治措施，若记录不存在则返回业务错误。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDiseaseControl(Long id) {
        requireId(id);
        int rows = diseaseControlMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "防治措施不存在");
        }
    }

    /** 批量删除防治措施，空集合直接视为参数错误。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDiseaseControls(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的防治措施");
        }
        return diseaseControlMapper.deleteBatchByIds(ids);
    }

    /** 修改防治措施，并返回更新后的完整记录。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiseaseControl updateDiseaseControl(DiseaseControl diseaseControl) {
        if (diseaseControl == null || diseaseControl.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "防治措施ID不能为空");
        }
        if (diseaseControlMapper.selectById(diseaseControl.getId()) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "防治措施不存在");
        }
        if (diseaseControl.getDiseaseId() != null) {
            checkDiseaseExists(diseaseControl.getDiseaseId());
        }
        validateUpdateDiseaseControl(diseaseControl);
        int rows = diseaseControlMapper.updateById(diseaseControl);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "防治措施修改失败");
        }
        return diseaseControlMapper.selectById(diseaseControl.getId());
    }

    /** 单独修改状态，方便列表页启用或停用记录。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireId(id);
        validateStatus(status);
        int rows = diseaseControlMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "防治措施不存在");
        }
    }

    /** 根据ID查询防治措施详情。 */
    @Override
    public DiseaseControl getDiseaseControlById(Long id) {
        requireId(id);
        DiseaseControl diseaseControl = diseaseControlMapper.selectById(id);
        if (diseaseControl == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "防治措施不存在");
        }
        return diseaseControl;
    }

    /** 分页查询防治措施列表，并复用 PageHelper 生成分页信息。 */
    @Override
    public PageInfo<DiseaseControl> listDiseaseControls(Long diseaseId, String diseaseName, Integer controlType,
                                                        Integer controlCategory, Integer status,
                                                        Integer pageNum, Integer pageSize) {
        validateControlType(controlType);
        validateControlCategory(controlCategory);
        validateStatus(status);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(diseaseControlMapper.selectList(
                diseaseId, diseaseName, controlType, controlCategory, status));
    }

    /** 校验新增时的必填字段。 */
    private void validateCreateDiseaseControl(DiseaseControl diseaseControl) {
        if (diseaseControl == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "防治措施信息不能为空");
        }
        if (diseaseControl.getDiseaseId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "病虫害ID不能为空");
        }
        if (diseaseControl.getControlType() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "防治类型不能为空");
        }
        if (diseaseControl.getControlCategory() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "防治手段不能为空");
        }
        if (!StringUtils.hasText(diseaseControl.getMethod())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "措施内容不能为空");
        }
        validateUpdateDiseaseControl(diseaseControl);
    }

    /** 校验新增和修改共用的字段范围。 */
    private void validateUpdateDiseaseControl(DiseaseControl diseaseControl) {
        validateControlType(diseaseControl.getControlType());
        validateControlCategory(diseaseControl.getControlCategory());
        validateStatus(diseaseControl.getStatus());
        if (diseaseControl.getSafetyIntervalDays() != null && diseaseControl.getSafetyIntervalDays() < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "安全间隔期不能小于0天");
        }
        if (diseaseControl.getSortOrder() != null && diseaseControl.getSortOrder() < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "展示顺序不能小于0");
        }
    }

    /** 为新增记录补齐数据库允许为空但业务常用的默认值。 */
    private void normalizeDefaults(DiseaseControl diseaseControl) {
        if (diseaseControl.getStatus() == null) {
            diseaseControl.setStatus(1);
        }
        if (diseaseControl.getSortOrder() == null) {
            diseaseControl.setSortOrder(0);
        }
        if (StringUtils.hasText(diseaseControl.getMethod())) {
            diseaseControl.setMethod(diseaseControl.getMethod().trim());
        }
    }

    /** 校验病虫害基础信息是否存在，避免产生无效外键。 */
    private void checkDiseaseExists(Long diseaseId) {
        if (diseasePestMapper.selectById(diseaseId) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "关联病虫害不存在");
        }
    }

    /** 校验防治类型枚举值。 */
    private void validateControlType(Integer controlType) {
        if (controlType != null && controlType != 1 && controlType != 2) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "防治类型只能为1或2");
        }
    }

    /** 校验农业、物理、化学和生物四类防治手段枚举。 */
    private void validateControlCategory(Integer controlCategory) {
        if (controlCategory != null && controlCategory != 1
                && controlCategory != 2 && controlCategory != 3 && controlCategory != 4) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "防治手段只能为1、2、3或4");
        }
    }

    /** 校验状态枚举值。 */
    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "状态只能为0或1");
        }
    }

    /** 公共ID非空校验。 */
    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "防治措施ID不能为空");
        }
    }
}
