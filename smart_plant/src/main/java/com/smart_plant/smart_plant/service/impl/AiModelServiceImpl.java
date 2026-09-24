package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AiModel;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.AiModelMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.AiModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI模型配置业务实现。
 *
 * <p>服务层负责输入校验、敏感字段脱敏和分页参数兜底，Mapper 只负责数据库访问。</p>
 */
@Service
@RequiredArgsConstructor
public class AiModelServiceImpl implements AiModelService {

    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;

    private final AiModelMapper aiModelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiModel addModel(AiModel model) {
        validateCreate(model);
        normalizeForSave(model);
        ensureUniqueModelName(model.getModelName(), null);
        aiModelMapper.insert(model);
        return maskSensitiveFields(aiModelMapper.selectById(model.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiModel updateModel(AiModel model) {
        if (model == null || model.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "模型ID不能为空");
        }
        AiModel oldModel = aiModelMapper.selectById(model.getId());
        if (oldModel == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "模型配置不存在");
        }
        normalizeUpdateFields(model, oldModel);
        validateCommon(model);
        ensureUniqueModelName(model.getModelName(), model.getId());
        int rows = aiModelMapper.updateById(model);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "模型配置修改失败");
        }
        return maskSensitiveFields(aiModelMapper.selectById(model.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long id) {
        requireId(id);
        AiModel oldModel = aiModelMapper.selectById(id);
        if (oldModel == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "模型配置不存在");
        }
        int rows = aiModelMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "模型配置删除失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteModels(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的模型配置");
        }
        for (Long id : ids) {
            requireId(id);
        }
        return aiModelMapper.deleteBatchByIds(ids);
    }

    @Override
    public AiModel getModelById(Long id) {
        requireId(id);
        AiModel model = aiModelMapper.selectById(id);
        if (model == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "模型配置不存在");
        }
        return maskSensitiveFields(model);
    }

    @Override
    public PageInfo<AiModel> listModels(String modelName, String model, Integer status, Integer pageNum, Integer pageSize) {
        validateStatus(status);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        PageHelper.startPage(currentPage, currentSize);
        PageInfo<AiModel> pageInfo = new PageInfo<>(aiModelMapper.selectList(
                normalizeOptionalText(modelName),
                normalizeOptionalText(model),
                status));
        pageInfo.getList().replaceAll(this::maskSensitiveFields);
        return pageInfo;
    }

    @Override
    public Map<String, Object> statisticsModels(String modelName, String model, Integer status) {
        validateStatus(status);
        Map<String, Object> statistics = aiModelMapper.selectStatistics(
                normalizeOptionalText(modelName),
                normalizeOptionalText(model),
                status);
        return statistics == null ? new HashMap<>() : statistics;
    }

    private void validateCreate(AiModel model) {
        if (model == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "模型配置不能为空");
        }
        normalizeForSave(model);
        if (!StringUtils.hasText(model.getApiKey())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "API Key不能为空");
        }
        validateCommon(model);
    }

    private void normalizeForSave(AiModel model) {
        model.setModelName(normalizeRequiredText(model.getModelName(), "模型名称不能为空"));
        model.setBaseUrl(normalizeRequiredText(model.getBaseUrl(), "模型地址不能为空"));
        model.setApiKey(normalizeOptionalText(model.getApiKey()));
        model.setModel(normalizeRequiredText(model.getModel(), "模型标识不能为空"));
        model.setRemark(normalizeOptionalText(model.getRemark()));
        model.setStatus(model.getStatus() == null ? STATUS_ENABLED : model.getStatus());
    }

    private void normalizeUpdateFields(AiModel model, AiModel oldModel) {
        model.setModelName(model.getModelName() == null ? oldModel.getModelName() : normalizeOptionalText(model.getModelName()));
        model.setBaseUrl(model.getBaseUrl() == null ? oldModel.getBaseUrl() : normalizeOptionalText(model.getBaseUrl()));
        model.setApiKey(normalizeOptionalText(model.getApiKey()));
        model.setModel(model.getModel() == null ? oldModel.getModel() : normalizeOptionalText(model.getModel()));
        model.setStatus(model.getStatus() == null ? oldModel.getStatus() : model.getStatus());
        model.setRemark(model.getRemark() == null ? oldModel.getRemark() : normalizeOptionalText(model.getRemark()));
    }

    private void validateCommon(AiModel model) {
        if (!StringUtils.hasText(model.getModelName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "模型名称不能为空");
        }
        if (!StringUtils.hasText(model.getBaseUrl())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "模型地址不能为空");
        }
        if (!StringUtils.hasText(model.getModel())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "模型标识不能为空");
        }
        validateStatus(model.getStatus());
        validateHttpUrl(model.getBaseUrl());
        validateLength(model.getModelName(), 100, "模型名称不能超过100个字符");
        validateLength(model.getBaseUrl(), 500, "模型地址不能超过500个字符");
        validateLength(model.getApiKey(), 500, "API Key不能超过500个字符");
        validateLength(model.getModel(), 100, "模型标识不能超过100个字符");
        validateLength(model.getRemark(), 500, "备注不能超过500个字符");
    }

    private void validateHttpUrl(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException();
            }
        } catch (Exception exception) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "模型地址必须是合法的HTTP或HTTPS地址");
        }
    }

    private void validateStatus(Integer status) {
        if (status != null && status != STATUS_ENABLED && status != STATUS_DISABLED) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "状态只能为0或1");
        }
    }

    private void ensureUniqueModelName(String modelName, Long excludeId) {
        if (aiModelMapper.countByModelName(modelName, excludeId) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "模型名称已存在");
        }
    }

    private AiModel maskSensitiveFields(AiModel model) {
        if (model != null && StringUtils.hasText(model.getApiKey())) {
            model.setApiKey(maskApiKey(model.getApiKey()));
        }
        return model;
    }

    private String maskApiKey(String apiKey) {
        if (apiKey.length() <= 8) {
            return "******";
        }
        return apiKey.substring(0, 4) + "******" + apiKey.substring(apiKey.length() - 4);
    }

    private String normalizeRequiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "模型ID不能为空");
        }
    }
}
