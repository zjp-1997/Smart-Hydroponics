package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.DeviceType;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.DeviceTypeMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DeviceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 设备类型业务实现类。
 *
 * <p>该类负责设备类型的字段校验、默认值补齐、唯一性校验、引用删除保护和分页查询。</p>
 */
@Service
@RequiredArgsConstructor
public class DeviceTypeServiceImpl implements DeviceTypeService {

    /** 设备分类：传感器。 */
    private static final int CATEGORY_SENSOR = 1;

    /** 设备分类：执行器。 */
    private static final int CATEGORY_ACTUATOR = 2;

    /** 状态：禁用。 */
    private static final int STATUS_DISABLED = 0;

    /** 状态：启用。 */
    private static final int STATUS_ENABLED = 1;

    /** 设备类型 Mapper，负责 device_type 表的读写。 */
    private final DeviceTypeMapper deviceTypeMapper;

    /** 新增设备类型时先校验必填字段，再补齐默认状态并检查类型编码唯一性。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceType addDeviceType(DeviceType deviceType) {
        validateCreate(deviceType);
        normalizeDefaults(deviceType);
        checkUniqueTypeCode(deviceType);
        deviceTypeMapper.insert(deviceType);
        return deviceTypeMapper.selectById(deviceType.getId());
    }

    /** 删除单个设备类型前先确认记录存在且未被设备表引用。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDeviceType(Long id) {
        requireId(id);
        checkExists(id);
        checkNoDeviceReferences(id);
        int rows = deviceTypeMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备类型不存在");
        }
    }

    /** 批量删除设备类型时逐条校验，避免删除已被设备引用的类型。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDeviceTypes(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的设备类型");
        }
        for (Long id : ids) {
            requireId(id);
            checkExists(id);
            checkNoDeviceReferences(id);
        }
        return deviceTypeMapper.deleteBatchByIds(ids);
    }

    /** 修改设备类型时支持局部更新，未传字段沿用数据库旧值。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceType updateDeviceType(DeviceType deviceType) {
        if (deviceType == null || deviceType.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备类型ID不能为空");
        }
        DeviceType oldType = deviceTypeMapper.selectById(deviceType.getId());
        if (oldType == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备类型不存在");
        }
        normalizeUpdateFields(deviceType, oldType);
        validateCommon(deviceType);
        checkUniqueTypeCode(deviceType);
        int rows = deviceTypeMapper.updateById(deviceType);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "设备类型修改失败");
        }
        return deviceTypeMapper.selectById(deviceType.getId());
    }

    /** 单独修改设备类型状态，便于前端做启用和禁用操作。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireId(id);
        validateStatusRequired(status);
        if (deviceTypeMapper.updateStatus(id, status) == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备类型不存在");
        }
    }

    /** 查询设备类型详情，统一处理ID为空和记录不存在的错误。 */
    @Override
    public DeviceType getDeviceTypeById(Long id) {
        requireId(id);
        DeviceType deviceType = deviceTypeMapper.selectById(id);
        if (deviceType == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备类型不存在");
        }
        return deviceType;
    }

    /** 分页查询设备类型列表，并复用 PageHelper 生成分页元数据。 */
    @Override
    public PageInfo<DeviceType> listDeviceTypes(String typeCode, String typeName, Integer category,
                                                Integer status, Integer pageNum, Integer pageSize) {
        validateCategory(category);
        validateStatus(status);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(deviceTypeMapper.selectList(
                normalizeOptionalText(typeCode),
                normalizeOptionalText(typeName),
                category,
                status));
    }

    /** 校验新增设备类型的必填字段。 */
    private void validateCreate(DeviceType deviceType) {
        if (deviceType == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备类型信息不能为空");
        }
        if (!StringUtils.hasText(deviceType.getTypeCode())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "类型编码不能为空");
        }
        if (!StringUtils.hasText(deviceType.getTypeName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "类型名称不能为空");
        }
        if (deviceType.getCategory() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备分类不能为空");
        }
    }

    /** 新增时补齐默认状态，并清理文本两侧空白。 */
    private void normalizeDefaults(DeviceType deviceType) {
        deviceType.setTypeCode(normalizeRequiredText(deviceType.getTypeCode(), "类型编码不能为空").toUpperCase());
        deviceType.setTypeName(normalizeRequiredText(deviceType.getTypeName(), "类型名称不能为空"));
        deviceType.setDescription(normalizeOptionalText(deviceType.getDescription()));
        deviceType.setStatus(deviceType.getStatus() == null ? STATUS_ENABLED : deviceType.getStatus());
        validateCommon(deviceType);
    }

    /** 修改时将未传字段补为旧值，支持前端局部提交。 */
    private void normalizeUpdateFields(DeviceType deviceType, DeviceType oldType) {
        deviceType.setTypeCode(StringUtils.hasText(deviceType.getTypeCode())
                ? deviceType.getTypeCode().trim().toUpperCase() : oldType.getTypeCode());
        deviceType.setTypeName(StringUtils.hasText(deviceType.getTypeName())
                ? deviceType.getTypeName().trim() : oldType.getTypeName());
        deviceType.setCategory(deviceType.getCategory() == null ? oldType.getCategory() : deviceType.getCategory());
        deviceType.setStatus(deviceType.getStatus() == null ? oldType.getStatus() : deviceType.getStatus());
        deviceType.setDescription(deviceType.getDescription() == null
                ? oldType.getDescription() : normalizeOptionalText(deviceType.getDescription()));
    }

    /** 校验新增和修改共用字段。 */
    private void validateCommon(DeviceType deviceType) {
        validateCategory(deviceType.getCategory());
        validateStatusRequired(deviceType.getStatus());
    }

    /** 校验设备分类枚举。 */
    private void validateCategory(Integer category) {
        if (category != null && category != CATEGORY_SENSOR && category != CATEGORY_ACTUATOR) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备分类只能为1或2");
        }
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
    private void checkUniqueTypeCode(DeviceType deviceType) {
        if (deviceTypeMapper.countByTypeCode(deviceType.getTypeCode(), deviceType.getId()) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "类型编码已存在");
        }
    }

    /** 确认设备类型存在，供删除前置校验使用。 */
    private void checkExists(Long id) {
        if (deviceTypeMapper.selectById(id) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备类型不存在");
        }
    }

    /** 删除前检查设备表引用，避免删除正在被 iot_device 使用的类型。 */
    private void checkNoDeviceReferences(Long id) {
        if (deviceTypeMapper.countDevicesByTypeId(id) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备类型已被设备引用，不能删除");
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
            throw new BusinessException(ResponseCode.PARAM_ERROR, "设备类型ID不能为空");
        }
    }
}
