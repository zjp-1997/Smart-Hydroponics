package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.IotDevicePlanPageResponse;
import com.smart_plant.smart_plant.dto.IotDevicePlanResponse;
import com.smart_plant.smart_plant.dto.IotDevicePlanSaveRequest;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.IotDevicePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** smart_farm 后台设备计划管理接口。 */
@RestController
@RequestMapping("/iot-device-plan")
@RequiredArgsConstructor
@RequirePermission("iot_device_plan:manage")
public class IotDevicePlanController {
    private final IotDevicePlanService planService;

    /** 分页查询四类支持计划管理的设备及其当前配置。 */
    @GetMapping("/list")
    public R<IotDevicePlanPageResponse> list(@RequestParam(required = false) Long plotId,
                                             @RequestParam(required = false) String typeCode,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(planService.listPlans(plotId, typeCode, keyword, pageNum, pageSize));
    }

    /** 查询单台设备的当前计划；未配置时返回服务端建议默认值。 */
    @GetMapping("/device/{deviceId}")
    public R<IotDevicePlanResponse> get(@PathVariable Long deviceId) {
        return R.success(planService.getByDeviceId(deviceId));
    }

    /** 按设备维度新增或更新当前计划。 */
    @PutMapping
    public R<IotDevicePlanResponse> save(@RequestBody IotDevicePlanSaveRequest request) {
        return R.success(planService.save(request));
    }

    /** 删除设备当前计划并恢复为默认建议状态。 */
    @DeleteMapping("/device/{deviceId}")
    public R<Void> delete(@PathVariable Long deviceId) {
        planService.deleteByDeviceId(deviceId);
        return R.success();
    }
}
