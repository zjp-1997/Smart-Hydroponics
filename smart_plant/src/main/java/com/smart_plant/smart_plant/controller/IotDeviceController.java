package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.IotDevice;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.IotDeviceService;
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备信息管理控制器。
 *
 * <p>该控制器参考用户管理模块的接口风格，提供 iot_device 表的新增、删除、修改、详情和分页查询接口。</p>
 */
@RestController
@RequestMapping("/iot-device")
@RequiredArgsConstructor
@RequirePermission("iot_device:manage")
public class IotDeviceController {

    /** 注入设备业务服务，控制器只负责 HTTP 参数接收和统一响应包装。 */
    private final IotDeviceService iotDeviceService;

    /** 新增设备信息。 */
    @PostMapping("/add")
    public R<IotDevice> addDevice(@RequestBody IotDevice iotDevice) {
        return R.success(iotDeviceService.addDevice(iotDevice));
    }

    /** 根据设备ID删除单台设备。 */
    @DeleteMapping("/{id}")
    public R<Void> deleteDevice(@PathVariable Long id) {
        iotDeviceService.deleteDevice(id);
        return R.success();
    }

    /** 根据设备ID集合批量删除设备。 */
    @DeleteMapping("/batch")
    public R<Integer> deleteDevices(@RequestBody List<Long> ids) {
        return R.success(iotDeviceService.deleteDevices(ids));
    }

    /** 修改设备基础信息和状态信息。 */
    @PutMapping
    public R<IotDevice> updateDevice(@RequestBody IotDevice iotDevice) {
        return R.success(iotDeviceService.updateDevice(iotDevice));
    }

    /** 修改设备业务控制状态，controlStatus=0 表示关闭，controlStatus=1 表示开启；保留 /status 路径兼容旧调用。 */
    @PutMapping({"/{id}/control-status", "/{id}/status"})
    public R<Void> updateControlStatus(@PathVariable Long id,
                                       @RequestParam(required = false) Integer controlStatus,
                                       @RequestParam(required = false) Integer status) {
        iotDeviceService.updateControlStatus(id, controlStatus != null ? controlStatus : status);
        return R.success();
    }

    /** 修改设备在线状态，onlineStatus=0 表示离线，onlineStatus=1 表示在线。 */
    @PutMapping("/{id}/online-status")
    public R<Void> updateOnlineStatus(@PathVariable Long id, @RequestParam Integer onlineStatus) {
        iotDeviceService.updateOnlineStatus(id, onlineStatus);
        return R.success();
    }

    /** 修改设备健康状态，healthStatus=0正常，1故障，2维护。 */
    @PutMapping("/{id}/health-status")
    public R<Void> updateHealthStatus(@PathVariable Long id, @RequestParam Integer healthStatus) {
        iotDeviceService.updateHealthStatus(id, healthStatus);
        return R.success();
    }

    /** 更新设备心跳时间，不传 lastHeartbeatTime 时由业务层使用当前服务器时间。 */
    @PutMapping("/{id}/heartbeat")
    public R<Void> heartbeat(@PathVariable Long id,
                             @RequestParam(required = false) LocalDateTime lastHeartbeatTime) {
        iotDeviceService.heartbeat(id, lastHeartbeatTime);
        return R.success();
    }

    /** 根据设备ID查询设备详情。 */
    @GetMapping("/{id}")
    public R<IotDevice> getDeviceById(@PathVariable Long id) {
        return R.success(iotDeviceService.getDeviceById(id));
    }

    /** 分页查询设备列表，支持地块、设备编码、名称、类型、控制状态、在线状态和健康状态筛选。 */
    @GetMapping("/list")
    public R<PageInfo<IotDevice>> listDevices(@RequestParam(required = false) Long plotId,
                                              @RequestParam(required = false) String plotName,
                                              @RequestParam(required = false) String deviceCode,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) Long typeId,
                                              @RequestParam(required = false) Integer controlStatus,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(required = false) Integer onlineStatus,
                                              @RequestParam(required = false) Integer healthStatus,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(iotDeviceService.listDevices(
                plotId, plotName, deviceCode, name, typeId,
                controlStatus != null ? controlStatus : status,
                onlineStatus, healthStatus, pageNum, pageSize));
    }
}
