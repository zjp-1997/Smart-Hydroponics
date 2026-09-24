package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.CameraDevice;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.CameraDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 监控设备管理控制器。
 *
 * <p>该控制器提供 camera_device 表的新增、删除、修改、详情、分页查询和在线状态维护接口。</p>
 */
@RestController
@RequestMapping("/camera-device")
@RequiredArgsConstructor
@RequirePermission("camera_device:manage")
public class CameraDeviceController {

    private final CameraDeviceService cameraDeviceService;

    /** 新增监控设备。 */
    @PostMapping("/add")
    public R<CameraDevice> addCameraDevice(@RequestBody CameraDevice cameraDevice) {
        return R.success(cameraDeviceService.addCameraDevice(cameraDevice));
    }

    /** 删除单个监控设备。 */
    @DeleteMapping("/{id}")
    public R<Void> deleteCameraDevice(@PathVariable Long id) {
        cameraDeviceService.deleteCameraDevice(id);
        return R.success();
    }

    /** 批量删除监控设备。 */
    @DeleteMapping("/batch")
    public R<Integer> deleteCameraDevices(@RequestBody List<Long> ids) {
        return R.success(cameraDeviceService.deleteCameraDevices(ids));
    }

    /** 修改监控设备。 */
    @PutMapping
    public R<CameraDevice> updateCameraDevice(@RequestBody CameraDevice cameraDevice) {
        return R.success(cameraDeviceService.updateCameraDevice(cameraDevice));
    }

    /** 修改监控设备在线状态，onlineStatus=0 离线，onlineStatus=1 在线。 */
    @PutMapping("/{id}/online-status")
    public R<Void> updateOnlineStatus(@PathVariable Long id, @RequestParam Integer onlineStatus) {
        cameraDeviceService.updateOnlineStatus(id, onlineStatus);
        return R.success();
    }

    /** 根据ID查询监控设备详情。 */
    @GetMapping("/{id}")
    public R<CameraDevice> getCameraDeviceById(@PathVariable Long id) {
        return R.success(cameraDeviceService.getCameraDeviceById(id));
    }

    /** 分页查询监控设备列表，支持按地块、名称、协议和在线状态筛选。 */
    @GetMapping("/list")
    public R<PageInfo<CameraDevice>> listCameraDevices(@RequestParam(required = false) Long plotId,
                                                       @RequestParam(required = false) String plotName,
                                                       @RequestParam(required = false) String name,
                                                       @RequestParam(required = false) String streamProtocol,
                                                       @RequestParam(required = false) Integer onlineStatus,
                                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(cameraDeviceService.listCameraDevices(
                plotId, plotName, name, streamProtocol, onlineStatus, pageNum, pageSize));
    }
}
