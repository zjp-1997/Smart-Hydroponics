package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.DeviceType;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.DeviceTypeService;
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
 * 设备类型管理控制器。
 *
 * <p>该控制器提供设备类型的新增、删除、修改、状态维护、详情和分页查询接口。</p>
 */
@RestController
@RequestMapping("/device-type")
@RequiredArgsConstructor
public class DeviceTypeController {

    /** 设备类型业务服务，负责具体校验和数据库操作。 */
    private final DeviceTypeService deviceTypeService;

    /** 新增设备类型。 */
    @PostMapping("/add")
    @RequirePermission("device_type:manage")
    public R<DeviceType> addDeviceType(@RequestBody DeviceType deviceType) {
        return R.success(deviceTypeService.addDeviceType(deviceType));
    }

    /** 删除单个设备类型，已被设备引用的类型会在业务层禁止删除。 */
    @DeleteMapping("/{id}")
    @RequirePermission("device_type:manage")
    public R<Void> deleteDeviceType(@PathVariable Long id) {
        deviceTypeService.deleteDeviceType(id);
        return R.success();
    }

    /** 批量删除设备类型。 */
    @DeleteMapping("/batch")
    @RequirePermission("device_type:manage")
    public R<Integer> deleteDeviceTypes(@RequestBody List<Long> ids) {
        return R.success(deviceTypeService.deleteDeviceTypes(ids));
    }

    /** 修改设备类型基础信息。 */
    @PutMapping
    @RequirePermission("device_type:manage")
    public R<DeviceType> updateDeviceType(@RequestBody DeviceType deviceType) {
        return R.success(deviceTypeService.updateDeviceType(deviceType));
    }

    /** 修改设备类型状态，status 为 1 启用、0 禁用。 */
    @PutMapping("/{id}/status")
    @RequirePermission("device_type:manage")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        deviceTypeService.updateStatus(id, status);
        return R.success();
    }

    /** 根据ID查询设备类型详情。 */
    @GetMapping("/{id}")
    public R<DeviceType> getDeviceTypeById(@PathVariable Long id) {
        return R.success(deviceTypeService.getDeviceTypeById(id));
    }

    /** 分页查询设备类型列表，支持按编码、名称、分类和状态筛选。 */
    @GetMapping("/list")
    public R<PageInfo<DeviceType>> listDeviceTypes(@RequestParam(required = false) String typeCode,
                                                   @RequestParam(required = false) String typeName,
                                                   @RequestParam(required = false) Integer category,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(deviceTypeService.listDeviceTypes(typeCode, typeName, category, status, pageNum, pageSize));
    }
}
