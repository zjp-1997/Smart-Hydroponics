package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.CameraPtzRequest;
import com.smart_plant.smart_plant.dto.CameraPtzResponse;
import com.smart_plant.smart_plant.dto.ClientMonitorResponse;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.ClientMonitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** farm 用户端实时监控接口，所有数据范围均由登录 token 决定。 */
@RestController
@RequestMapping({"/client/monitors", "/api/client/monitors"})
@RequiredArgsConstructor
public class ClientMonitorController {

    /** 用户端监控服务，负责数据权限、播放信息和云台控制。 */
    private final ClientMonitorService clientMonitorService;

    /** 查询监控列表；plotId 用于从指定地块进入时收窄结果。 */
    @GetMapping
    public R<List<ClientMonitorResponse>> listMonitors(@RequestParam(required = false) Long plotId,
                                                       @RequestParam(required = false) String keyword) {
        return R.success(clientMonitorService.listCurrentClientMonitors(plotId, keyword));
    }

    /** 查询单个监控的实时播放信息。 */
    @GetMapping("/{id}")
    public R<ClientMonitorResponse> getMonitor(@PathVariable Long id) {
        return R.success(clientMonitorService.getCurrentClientMonitor(id));
    }

    /** 下发云台上下左右或停止指令。 */
    @PostMapping("/{id}/ptz")
    public R<CameraPtzResponse> controlPtz(@PathVariable Long id,
                                          @Valid @RequestBody CameraPtzRequest request) {
        return R.success(clientMonitorService.controlCurrentClientMonitor(id, request.getDirection()));
    }
}
