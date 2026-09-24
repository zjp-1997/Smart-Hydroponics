package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.IotDeviceFault;

import java.util.List;

/**
 * 设备故障管理业务接口。
 */
public interface IotDeviceFaultService {

    IotDeviceFault addFault(IotDeviceFault fault);

    void deleteFault(Long id);

    int deleteFaults(List<Long> ids);

    IotDeviceFault updateFault(IotDeviceFault fault);

    void updateStatus(Long id, Integer status, String handleResult);

    void assignFault(Long id, Long handleUserId);

    void acceptAssignment(Long id);

    void rejectAssignment(Long id, String rejectReason);

    IotDeviceFault getFaultById(Long id);

    PageInfo<IotDeviceFault> listFaults(Long deviceId, String deviceName, String faultCode, String faultName,
                                        Integer faultType, Integer severity, Integer status, Long handleUserId,
                                        Integer pageNum, Integer pageSize);

}
