package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.PumpData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 水泵监测数据业务接口。
 *
 * <p>该接口面向控制器提供水泵监测数据管理能力，包括删除、批量删除、分页查询和详情查询。
 * 由于数据由传感器自动上传，不提供新增和修改方法。</p>
 */
public interface PumpDataService {

    /** 根据ID删除水泵监测数据 */
    void deletePumpData(Long id);

    /** 批量删除水泵监测数据 */
    int deletePumpDataBatch(List<Long> ids);

    /** 根据ID查询水泵监测数据详情（弹框展示） */
    PumpData getPumpDataById(Long id);

    /** 分页条件查询水泵监测数据列表，支持地块、设备、数据状态和采集时间范围筛选。 */
    PageInfo<PumpData> listPumpData(Long plotId, Long deviceId, String deviceCode, Integer dataStatus,
                                     LocalDateTime startTime, LocalDateTime endTime,
                                     Integer pageNum, Integer pageSize);

    /** 统计水泵监测数据总数、异常数、设备数和运行指标平均值。 */
    Map<String, Object> statisticsPumpData(Long plotId, Long deviceId, String deviceCode, Integer dataStatus,
                                           LocalDateTime startTime, LocalDateTime endTime);
}
