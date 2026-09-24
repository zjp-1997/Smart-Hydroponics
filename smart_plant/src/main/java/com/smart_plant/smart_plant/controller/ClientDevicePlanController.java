package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.IotDevicePlanPageResponse;
import com.smart_plant.smart_plant.dto.IotDevicePlanResponse;
import com.smart_plant.smart_plant.dto.IotDevicePlanSaveRequest;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.IotDevicePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** farm 用户端设备计划接口，业务层强制按登录用户隔离数据。 */
@RestController
@RequestMapping({"/client/device-plans", "/api/client/device-plans"})
@RequiredArgsConstructor
public class ClientDevicePlanController {
    private final IotDevicePlanService planService;

    @GetMapping
    public R<IotDevicePlanPageResponse> list() {
        return R.success(planService.listPlans(null, null, null, 1, 100));
    }

    @GetMapping("/{deviceId}")
    public R<IotDevicePlanResponse> get(@PathVariable Long deviceId) {
        return R.success(planService.getByDeviceId(deviceId));
    }

    @PutMapping
    public R<IotDevicePlanResponse> save(@RequestBody IotDevicePlanSaveRequest request) {
        return R.success(planService.save(request));
    }
}
