package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.CameraDevice;

import java.util.List;

/**
 * 监控设备业务接口。
 */
public interface CameraDeviceService {

    /** 新增监控设备。 */
    CameraDevice addCameraDevice(CameraDevice cameraDevice);

    /** 删除单个监控设备。 */
    void deleteCameraDevice(Long id);

    /** 批量删除监控设备。 */
    int deleteCameraDevices(List<Long> ids);

    /** 修改监控设备。 */
    CameraDevice updateCameraDevice(CameraDevice cameraDevice);

    /** 修改在线状态。 */
    void updateOnlineStatus(Long id, Integer onlineStatus);

    /** 查询监控设备详情。 */
    CameraDevice getCameraDeviceById(Long id);

    /** 分页查询监控设备列表。 */
    PageInfo<CameraDevice> listCameraDevices(Long plotId, String plotName, String name,
                                             String streamProtocol, Integer onlineStatus,
                                             Integer pageNum, Integer pageSize);
}
