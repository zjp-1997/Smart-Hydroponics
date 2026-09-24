package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.ClientEnvironmentListResponse;

/**
 * farm 用户端环境监测服务。
 *
 * <p>所有查询都基于当前登录用户执行，避免移动端传入 userId 造成越权访问。</p>
 */
public interface ClientEnvironmentService {

    /**
     * 查询当前登录用户的最新环境监测数据。
     *
     * @return 聚合后的空气环境、水质、水泵和光照数据
     */
    ClientEnvironmentListResponse getCurrentClientEnvironmentData();

    /**
     * 根据地块 ID 查询当前登录用户该地块下的最新环境监测和水质监测数据。
     *
     * @param plotId 地块主键
     * @return 聚合后的空气环境、水质、水泵和光照数据
     */
    ClientEnvironmentListResponse getCurrentClientEnvironmentDataByPlotId(Long plotId);
}
