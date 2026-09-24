package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.DeviceType;

import java.util.List;

/**
 * 设备类型业务接口。
 *
 * <p>该接口定义设备类型管理功能的业务能力，控制器通过它调用具体实现。</p>
 */
public interface DeviceTypeService {

    /** 新增设备类型。 */
    DeviceType addDeviceType(DeviceType deviceType);

    /** 删除单个设备类型。 */
    void deleteDeviceType(Long id);

    /** 批量删除设备类型。 */
    int deleteDeviceTypes(List<Long> ids);

    /** 修改设备类型基础信息。 */
    DeviceType updateDeviceType(DeviceType deviceType);

    /** 修改设备类型启用或禁用状态。 */
    void updateStatus(Long id, Integer status);

    /** 根据ID查询设备类型详情。 */
    DeviceType getDeviceTypeById(Long id);

    /** 分页查询设备类型列表。 */
    PageInfo<DeviceType> listDeviceTypes(String typeCode, String typeName, Integer category,
                                         Integer status, Integer pageNum, Integer pageSize);
}
