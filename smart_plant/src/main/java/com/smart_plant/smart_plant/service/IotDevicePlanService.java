package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.IotDevicePlanPageResponse;
import com.smart_plant.smart_plant.dto.IotDevicePlanResponse;
import com.smart_plant.smart_plant.dto.IotDevicePlanSaveRequest;

public interface IotDevicePlanService {
    /** 分页查询设备计划。 */
    IotDevicePlanPageResponse listPlans(Long plotId, String typeCode, String keyword, Integer pageNum, Integer pageSize);

    /** 查询单台设备计划。 */
    IotDevicePlanResponse getByDeviceId(Long deviceId);

    /** 新增或更新单台设备计划。 */
    IotDevicePlanResponse save(IotDevicePlanSaveRequest request);

    /** 删除单台设备计划。 */
    void deleteByDeviceId(Long deviceId);
}
