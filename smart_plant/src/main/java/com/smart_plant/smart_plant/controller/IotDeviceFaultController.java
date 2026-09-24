package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.IotDeviceFault;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.IotDeviceFaultService;
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
 * 设备故障管理控制器。
 */
@RestController
@RequestMapping("/iot-device-fault")
@RequiredArgsConstructor
@RequirePermission("iot_device_fault:manage")
public class IotDeviceFaultController {

    private final IotDeviceFaultService faultService;

    @PostMapping("/add")
    public R<IotDeviceFault> addFault(@RequestBody IotDeviceFault fault) {
        return R.success(faultService.addFault(fault));
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteFault(@PathVariable Long id) {
        faultService.deleteFault(id);
        return R.success();
    }

    @DeleteMapping("/batch")
    public R<Integer> deleteFaults(@RequestBody List<Long> ids) {
        return R.success(faultService.deleteFaults(ids));
    }

    @PutMapping
    public R<IotDeviceFault> updateFault(@RequestBody IotDeviceFault fault) {
        return R.success(faultService.updateFault(fault));
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id,
                                @RequestParam Integer status,
                                @RequestParam(required = false) String handleResult) {
        faultService.updateStatus(id, status, handleResult);
        return R.success();
    }

    @PutMapping("/{id}/assign")
    public R<Void> assignFault(@PathVariable Long id, @RequestParam Long handleUserId) {
        faultService.assignFault(id, handleUserId);
        return R.success();
    }

    @PutMapping("/{id}/assignment/accept")
    public R<Void> acceptAssignment(@PathVariable Long id) {
        faultService.acceptAssignment(id);
        return R.success();
    }

    @PutMapping("/{id}/assignment/reject")
    public R<Void> rejectAssignment(@PathVariable Long id,
                                    @RequestParam(required = false) String rejectReason) {
        faultService.rejectAssignment(id, rejectReason);
        return R.success();
    }

    @GetMapping("/{id}")
    public R<IotDeviceFault> getFaultById(@PathVariable Long id) {
        return R.success(faultService.getFaultById(id));
    }

    @GetMapping("/list")
    public R<PageInfo<IotDeviceFault>> listFaults(@RequestParam(required = false) Long deviceId,
                                                  @RequestParam(required = false) String deviceName,
                                                  @RequestParam(required = false) String faultCode,
                                                  @RequestParam(required = false) String faultName,
                                                  @RequestParam(required = false) Integer faultType,
                                                  @RequestParam(required = false) Integer severity,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) Long handleUserId,
                                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(faultService.listFaults(
                deviceId, deviceName, faultCode, faultName, faultType, severity, status, handleUserId,
                pageNum, pageSize));
    }

}
