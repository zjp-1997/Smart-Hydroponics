package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.EnvironmentData;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.EnvironmentDataMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.EnvironmentDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 环境监测数据业务实现类。
 *
 * <p>由于环境监测数据由传感器自动上传，本服务仅提供删除、批量删除和查询功能，不提供新增和修改功能。</p>
 */
@Service
@RequiredArgsConstructor
public class EnvironmentDataServiceImpl implements EnvironmentDataService {

    private final EnvironmentDataMapper environmentDataMapper;

    private final DataPermissionService dataPermissionService;

    /** 数据状态：正常。 */
    private static final int DATA_STATUS_NORMAL = 1;

    /** 数据状态：异常。 */
    private static final int DATA_STATUS_ABNORMAL = 2;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEnvironmentData(Long id) {
        requireId(id);
        EnvironmentData data = getEnvironmentDataById(id);
        dataPermissionService.requireFarmManager(data.getOwnerUserId());
        int rows = environmentDataMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "环境监测数据不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteEnvironmentDataBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的环境监测数据");
        }
        for (Long id : ids) {
            requireId(id);
            EnvironmentData data = getEnvironmentDataById(id);
            dataPermissionService.requireFarmManager(data.getOwnerUserId());
        }
        return environmentDataMapper.deleteBatchByIds(ids);
    }

    @Override
    public EnvironmentData getEnvironmentDataById(Long id) {
        requireId(id);
        EnvironmentData data = environmentDataMapper.selectById(id);
        if (data == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "环境监测数据不存在");
        }
        dataPermissionService.requireOwnedResource(data.getOwnerUserId());
        return data;
    }

    @Override
    public PageInfo<EnvironmentData> listEnvironmentData(Long plotId, Long deviceId, String deviceCode, Integer dataStatus,
                                                          LocalDateTime startTime, LocalDateTime endTime,
                                                          Integer pageNum, Integer pageSize) {
        validateQueryParams(dataStatus, startTime, endTime);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(environmentDataMapper.selectList(
                scopedUserId, plotId, deviceId, normalizeOptionalText(deviceCode), dataStatus, startTime, endTime));
    }

    @Override
    public Map<String, Object> statisticsEnvironmentData(Long plotId, Long deviceId, String deviceCode,
                                                         Integer dataStatus, LocalDateTime startTime,
                                                         LocalDateTime endTime) {
        validateQueryParams(dataStatus, startTime, endTime);
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        return environmentDataMapper.selectStatistics(
                scopedUserId, plotId, deviceId, normalizeOptionalText(deviceCode), dataStatus, startTime, endTime);
    }

    /**
     * 校验ID是否为空。
     */
    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "环境监测数据ID不能为空");
        }
    }

    /** 校验列表和统计接口的公共查询参数。 */
    private void validateQueryParams(Integer dataStatus, LocalDateTime startTime, LocalDateTime endTime) {
        if (dataStatus != null && dataStatus != DATA_STATUS_NORMAL && dataStatus != DATA_STATUS_ABNORMAL) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "数据状态只能为1或2");
        }
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "开始时间不能晚于结束时间");
        }
    }

    /** 可选字符串标准化，空字符串统一转成 null。 */
    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
