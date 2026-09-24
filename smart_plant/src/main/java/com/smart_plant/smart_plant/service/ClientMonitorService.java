package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.CameraPtzResponse;
import com.smart_plant.smart_plant.dto.ClientMonitorResponse;

import java.util.List;

/** farm 用户端实时监控服务。 */
public interface ClientMonitorService {

    /** 查询当前登录用户的监控列表，可按地块和名称筛选。 */
    List<ClientMonitorResponse> listCurrentClientMonitors(Long plotId, String keyword);

    /** 查询当前登录用户的单个监控详情。 */
    ClientMonitorResponse getCurrentClientMonitor(Long id);

    /** 校验并下发当前登录用户的云台方向指令。 */
    CameraPtzResponse controlCurrentClientMonitor(Long id, String direction);
}
