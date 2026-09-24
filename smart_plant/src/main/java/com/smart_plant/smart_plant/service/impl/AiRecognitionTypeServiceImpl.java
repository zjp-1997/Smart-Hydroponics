package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AiRecognitionType;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.AiRecognitionTypeMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.AiRecognitionTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * AI识别类型业务实现类。
 *
 * <p>该类负责AI识别类型的字段校验、默认值补齐、唯一性校验、删除保护、分页查询和统计。</p>
 */
@Service
@RequiredArgsConstructor
public class AiRecognitionTypeServiceImpl implements AiRecognitionTypeService {

    /** 状态：禁用。 */
    private static final int STATUS_DISABLED = 0;

    /** 状态：启用。 */
    private static final int STATUS_ENABLED = 1;

    /** AI识别类型 Mapper，负责 ai_recognition_type 表的读写。 */
    private final AiRecognitionTypeMapper aiRecognitionTypeMapper;

    /** 新增AI识别类型时先校验必填字段，再补齐默认状态并检查类型编码唯一性。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiRecognitionType addAiRecognitionType(AiRecognitionType aiRecognitionType) {
        validateCreate(aiRecognitionType);
        normalizeDefaults(aiRecognitionType);
        checkUniqueTypeCode(aiRecognitionType);
        aiRecognitionTypeMapper.insert(aiRecognitionType);
        return aiRecognitionTypeMapper.selectById(aiRecognitionType.getId());
    }

    /** 修改AI识别类型时支持局部更新，未传字段沿用数据库旧值。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiRecognitionType updateAiRecognitionType(AiRecognitionType aiRecognitionType) {
        if (aiRecognitionType == null || aiRecognitionType.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "AI识别类型ID不能为空");
        }
        AiRecognitionType oldType = aiRecognitionTypeMapper.selectById(aiRecognitionType.getId());
        if (oldType == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "AI识别类型不存在");
        }
        normalizeUpdateFields(aiRecognitionType, oldType);
        validateCommon(aiRecognitionType);
        checkUniqueTypeCode(aiRecognitionType);
        int rows = aiRecognitionTypeMapper.updateById(aiRecognitionType);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "AI识别类型修改失败");
        }
        return aiRecognitionTypeMapper.selectById(aiRecognitionType.getId());
    }

    /** 删除单个AI识别类型前先确认记录存在且未被识别结果引用。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAiRecognitionType(Long id) {
        requireId(id);
        checkExists(id);
        checkNoRecognitionResultReferences(id);
        int rows = aiRecognitionTypeMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "AI识别类型不存在");
        }
    }

    /** 批量删除AI识别类型时逐条校验，避免删除已被识别结果引用的类型。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAiRecognitionTypes(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的AI识别类型");
        }
        for (Long id : ids) {
            requireId(id);
            checkExists(id);
            checkNoRecognitionResultReferences(id);
        }
        return aiRecognitionTypeMapper.deleteBatchByIds(ids);
    }

    /** 单独修改AI识别类型状态，便于前端做启用和禁用操作。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireId(id);
        validateStatusRequired(status);
        if (aiRecognitionTypeMapper.updateStatus(id, status) == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "AI识别类型不存在");
        }
    }

    /** 查询AI识别类型详情，统一处理ID为空和记录不存在的错误。 */
    @Override
    public AiRecognitionType getAiRecognitionTypeById(Long id) {
        requireId(id);
        AiRecognitionType aiRecognitionType = aiRecognitionTypeMapper.selectById(id);
        if (aiRecognitionType == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "AI识别类型不存在");
        }
        return aiRecognitionType;
    }

    /** 分页查询AI识别类型列表，并复用 PageHelper 生成分页元数据。 */
    @Override
    public PageInfo<AiRecognitionType> listAiRecognitionTypes(String typeCode, String typeName, Integer status,
                                                              Integer pageNum, Integer pageSize) {
        validateStatus(status);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(aiRecognitionTypeMapper.selectList(
                normalizeOptionalText(typeCode),
                normalizeOptionalText(typeName),
                status));
    }

    /** 统计AI识别类型总数、启用数、禁用数和识别结果引用数。 */
    @Override
    public Map<String, Object> statisticsAiRecognitionTypes(String typeCode, String typeName, Integer status) {
        validateStatus(status);
        return aiRecognitionTypeMapper.selectStatistics(
                normalizeOptionalText(typeCode),
                normalizeOptionalText(typeName),
                status);
    }

    /** 校验新增AI识别类型的必填字段。 */
    private void validateCreate(AiRecognitionType aiRecognitionType) {
        if (aiRecognitionType == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "AI识别类型信息不能为空");
        }
        if (!StringUtils.hasText(aiRecognitionType.getTypeCode())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "类型编码不能为空");
        }
        if (!StringUtils.hasText(aiRecognitionType.getTypeName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "类型名称不能为空");
        }
    }

    /** 新增时补齐默认状态，并清理文本两侧空白。 */
    private void normalizeDefaults(AiRecognitionType aiRecognitionType) {
        aiRecognitionType.setTypeCode(normalizeRequiredText(
                aiRecognitionType.getTypeCode(), "类型编码不能为空").toUpperCase());
        aiRecognitionType.setTypeName(normalizeRequiredText(aiRecognitionType.getTypeName(), "类型名称不能为空"));
        aiRecognitionType.setDescription(normalizeOptionalText(aiRecognitionType.getDescription()));
        aiRecognitionType.setStatus(aiRecognitionType.getStatus() == null ? STATUS_ENABLED : aiRecognitionType.getStatus());
        validateCommon(aiRecognitionType);
    }

    /** 修改时将未传字段补为旧值，支持前端局部提交。 */
    private void normalizeUpdateFields(AiRecognitionType aiRecognitionType, AiRecognitionType oldType) {
        aiRecognitionType.setTypeCode(StringUtils.hasText(aiRecognitionType.getTypeCode())
                ? aiRecognitionType.getTypeCode().trim().toUpperCase() : oldType.getTypeCode());
        aiRecognitionType.setTypeName(StringUtils.hasText(aiRecognitionType.getTypeName())
                ? aiRecognitionType.getTypeName().trim() : oldType.getTypeName());
        aiRecognitionType.setStatus(aiRecognitionType.getStatus() == null ? oldType.getStatus() : aiRecognitionType.getStatus());
        aiRecognitionType.setDescription(aiRecognitionType.getDescription() == null
                ? oldType.getDescription() : normalizeOptionalText(aiRecognitionType.getDescription()));
    }

    /** 校验新增和修改共用字段。 */
    private void validateCommon(AiRecognitionType aiRecognitionType) {
        validateStatusRequired(aiRecognitionType.getStatus());
    }

    /** 校验必填状态字段，空值会抛出业务异常。 */
    private void validateStatusRequired(Integer status) {
        if (status == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "状态不能为空");
        }
        validateStatus(status);
    }

    /** 校验状态枚举。 */
    private void validateStatus(Integer status) {
        if (status != null && status != STATUS_DISABLED && status != STATUS_ENABLED) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "状态只能为0或1");
        }
    }

    /** 校验类型编码在全表唯一。 */
    private void checkUniqueTypeCode(AiRecognitionType aiRecognitionType) {
        if (aiRecognitionTypeMapper.countByTypeCode(aiRecognitionType.getTypeCode(), aiRecognitionType.getId()) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "类型编码已存在");
        }
    }

    /** 确认AI识别类型存在，供删除前置校验使用。 */
    private void checkExists(Long id) {
        if (aiRecognitionTypeMapper.selectById(id) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "AI识别类型不存在");
        }
    }

    /** 删除前检查识别结果表引用，避免删除正在被 ai_recognition_result 使用的类型。 */
    private void checkNoRecognitionResultReferences(Long id) {
        if (aiRecognitionTypeMapper.countRecognitionResultsByTypeId(id) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "AI识别类型已被识别结果引用，不能删除");
        }
    }

    /** 必填字符串标准化，空字符串会抛出业务异常。 */
    private String normalizeRequiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
        return value.trim();
    }

    /** 可选字符串标准化，空字符串统一转成 null，方便动态 SQL 忽略空值。 */
    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 公共ID非空校验。 */
    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "AI识别类型ID不能为空");
        }
    }
}
