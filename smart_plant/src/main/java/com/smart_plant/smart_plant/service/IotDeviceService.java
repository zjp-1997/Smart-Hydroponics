package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ClientDeviceListResponse;
import com.smart_plant.smart_plant.entity.IotDevice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备信息业务接口。
 *
 * <p>该接口面向控制器提供设备管理能力，方法设计与用户管理模块保持一致。</p>
 */
public interface IotDeviceService {

    /** 新增设备信息。 */
    IotDevice addDevice(IotDevice iotDevice);

    /** 根据ID删除单台设备。 */
    void deleteDevice(Long id);

    /** 批量删除设备。 */
    int deleteDevices(List<Long> ids);

    /** 修改设备基础信息和状态信息。 */
    IotDevice updateDevice(IotDevice iotDevice);

    /** 修改设备业务控制状态。 */
    void updateControlStatus(Long id, Integer controlStatus);

    /** 修改设备在线状态。 */
    void updateOnlineStatus(Long id, Integer onlineStatus);

    /** 修改设备健康状态。 */
    void updateHealthStatus(Long id, Integer healthStatus);

    /** 更新设备心跳时间。 */
    void heartbeat(Long id, LocalDateTime lastHeartbeatTime);

    /** 根据ID查询设备详情。 */
    IotDevice getDeviceById(Long id);

    /** 分页查询设备列表。 */
    PageInfo<IotDevice> listDevices(Long plotId, String plotName, String deviceCode, String name, Long typeId,
                                    Integer controlStatus, Integer onlineStatus, Integer healthStatus,
                                    Integer pageNum, Integer pageSize);

    /**
     * 查询当前登录用户的设备分组列表，用于 farm 用户端设备管理页面。
     *
     * @return 按补光灯、水泵、摄像头、水质检测仪、环境检测仪分组后的设备数据
     */
    ClientDeviceListResponse listCurrentClientDeviceGroups();

    /** 更新 farm 端设备在线状态，并返回最新的列表行数据。 */
    ClientDeviceListResponse.DeviceItem updateCurrentClientDeviceOnlineStatus(
            String source, Long id, Integer onlineStatus);
}
