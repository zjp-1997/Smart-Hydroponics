package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientDeviceListResponse;
import com.smart_plant.smart_plant.dto.ClientDeviceOnlineStatusRequest;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.IotDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * farm 用户端设备列表接口。
 *
 * <p>该接口不接收 userId 参数，所有设备数据都根据登录 token 中的当前用户过滤，
 * 防止移动端通过篡改参数查看其他用户的设备。</p>
 */
@RestController
@RequestMapping({"/client", "/api/client"})
@RequiredArgsConstructor
public class ClientDeviceController {

    /** 设备业务服务，负责设备查询、权限边界和分组转换。 */
    private final IotDeviceService iotDeviceService;

    @GetMapping("/devices/list")
    public R<ClientDeviceListResponse> listCurrentUserDevices() {
        // 返回当前登录用户的补光灯、水泵、摄像头、水质检测仪和环境检测仪分组列表。
        return R.success(iotDeviceService.listCurrentClientDeviceGroups());
    }

    /** 农场主切换普通设备或摄像头的在线状态。 */
    @PutMapping("/devices/{source}/{id}/online-status")
    public R<ClientDeviceListResponse.DeviceItem> updateOnlineStatus(
            @PathVariable String source,
            @PathVariable Long id,
            @RequestBody ClientDeviceOnlineStatusRequest request) {
        Integer onlineStatus = request == null ? null : request.getOnlineStatus();
        return R.success(iotDeviceService.updateCurrentClientDeviceOnlineStatus(source, id, onlineStatus));
    }
}
