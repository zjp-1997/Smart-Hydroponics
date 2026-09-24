package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.EnvironmentData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 环境监测数据业务接口。
 *
 * <p>该接口面向控制器提供环境监测数据管理能力，包括删除、批量删除、分页查询和详情查询。
 * 由于数据由传感器自动上传，不提供新增和修改方法。</p>
 */
public interface EnvironmentDataService {

    /** 根据ID删除环境监测数据 */
    void deleteEnvironmentData(Long id);

    /** 批量删除环境监测数据 */
    int deleteEnvironmentDataBatch(List<Long> ids);

    /** 根据ID查询环境监测数据详情（弹框展示） */
    EnvironmentData getEnvironmentDataById(Long id);

    /** 分页条件查询环境监测数据列表，支持地块、设备、数据状态和采集时间范围筛选。 */
    PageInfo<EnvironmentData> listEnvironmentData(Long plotId, Long deviceId, String deviceCode, Integer dataStatus,
                                                   LocalDateTime startTime, LocalDateTime endTime,
                                                   Integer pageNum, Integer pageSize);

    /** 统计环境监测数据总数、异常数、设备数和核心指标平均值。 */
    Map<String, Object> statisticsEnvironmentData(Long plotId, Long deviceId, String deviceCode, Integer dataStatus,
                                                  LocalDateTime startTime, LocalDateTime endTime);
}
