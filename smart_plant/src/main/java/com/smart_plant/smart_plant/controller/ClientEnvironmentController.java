package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientEnvironmentListResponse;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.ClientEnvironmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * farm 用户端环境监测接口。
 *
 * <p>接口不接收 userId，后端从登录 token 中识别当前用户，
 * 只返回当前用户自己设备采集到的环境监测数据。</p>
 */
@RestController
@RequestMapping({"/client", "/api/client"})
@RequiredArgsConstructor
public class ClientEnvironmentController {

    /** 环境监测聚合服务，负责跨数据表获取最新监测指标。 */
    private final ClientEnvironmentService clientEnvironmentService;

    @GetMapping("/environment/list")
    public R<ClientEnvironmentListResponse> getCurrentClientEnvironmentData() {
        // 返回当前登录用户的空气环境、水质、水泵和光照监测数据。
        return R.success(clientEnvironmentService.getCurrentClientEnvironmentData());
    }

    @GetMapping("/plots/{plotId}/environment")
    public R<ClientEnvironmentListResponse> getCurrentClientEnvironmentDataByPlotId(@PathVariable Long plotId) {
        // 按地块查询时，后端会校验该地块是否属于当前登录用户，避免越权查看其他用户数据。
        return R.success(clientEnvironmentService.getCurrentClientEnvironmentDataByPlotId(plotId));
    }
}
