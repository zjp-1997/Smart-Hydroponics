package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.ClientEnvironmentListResponse;
import com.smart_plant.smart_plant.entity.EnvironmentData;
import com.smart_plant.smart_plant.entity.LightData;
import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.entity.PumpData;
import com.smart_plant.smart_plant.entity.WaterQualityData;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.EnvironmentDataMapper;
import com.smart_plant.smart_plant.mapper.LightDataMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.mapper.PumpDataMapper;
import com.smart_plant.smart_plant.mapper.WaterQualityDataMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.ClientEnvironmentService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * farm 用户端环境监测服务实现。
 *
 * <p>这里复用后台已有监测数据 Mapper，查询条件固定为当前账号可见的农场主 ID。
 * 各 Mapper 已按采集时间倒序排序，因此取第一条即可得到该类型设备的最新数据。</p>
 */
@Service
@RequiredArgsConstructor
public class ClientEnvironmentServiceImpl implements ClientEnvironmentService {

    /** 空气环境数据 Mapper，提供温度、湿度、CO2、PM2.5 数据。 */
    private final EnvironmentDataMapper environmentDataMapper;

    /** 水质数据 Mapper，提供水温、PH、EC、溶解氧数据。 */
    private final WaterQualityDataMapper waterQualityDataMapper;

    /** 水泵数据 Mapper，提供水流量、水压数据。 */
    private final PumpDataMapper pumpDataMapper;

    /** 光照数据 Mapper，提供光照强度数据。 */
    private final LightDataMapper lightDataMapper;

    /** 地块 Mapper，用于校验移动端传入的 plotId 是否属于当前登录用户。 */
    private final PlotMapper plotMapper;

    /** 当前用户权限服务，用于从 token 中解析登录用户。 */
    private final DataPermissionService dataPermissionService;

    @Override
    public ClientEnvironmentListResponse getCurrentClientEnvironmentData() {
        Long currentUserId = dataPermissionService.currentClientOwnerId();
        return buildEnvironmentResponse(currentUserId, null);
    }

    @Override
    public ClientEnvironmentListResponse getCurrentClientEnvironmentDataByPlotId(Long plotId) {
        if (plotId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "地块ID不能为空");
        }

        Long currentUserId = dataPermissionService.currentClientOwnerId();
        Plot plot = plotMapper.selectById(plotId);
        if (plot == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "地块不存在");
        }
        if (!currentUserId.equals(plot.getUserId())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "无权查看该地块环境数据");
        }

        return buildEnvironmentResponse(currentUserId, plotId);
    }

    /**
     * 聚合指定用户、指定地块的最新监测数据。
     *
     * <p>plotId 为空时表示查询该用户所有地块中的最新数据；plotId 有值时只查询该地块。
     * Mapper 层仍然带上 userId 条件，形成“身份 + 地块”的双重数据隔离。</p>
     */
    private ClientEnvironmentListResponse buildEnvironmentResponse(Long currentUserId, Long plotId) {
        ClientEnvironmentListResponse response = new ClientEnvironmentListResponse();

        EnvironmentData environmentData = first(environmentDataMapper.selectList(currentUserId, plotId, null, null, null, null, null));
        if (environmentData != null) {
            response.setAirTemperature(environmentData.getAirTemperature());
            response.setAirHumidity(environmentData.getAirHumidity());
            response.setWindSpeed(environmentData.getWindSpeed());
            response.setAirPressure(environmentData.getAirPressure());
            response.setCo2Concentration(environmentData.getCo2Concentration());
            response.setPm25(environmentData.getPm25());
            response.setEnvironmentCollectTime(environmentData.getCollectTime());
        }

        WaterQualityData waterQualityData = first(waterQualityDataMapper.selectList(currentUserId, plotId, null, null, null, null, null));
        if (waterQualityData != null) {
            response.setWaterTemperature(waterQualityData.getWaterTemperature());
            response.setPh(waterQualityData.getPh());
            response.setEcValue(waterQualityData.getEcValue());
            response.setDissolvedOxygen(waterQualityData.getDissolvedOxygen());
            response.setWaterQualityCollectTime(waterQualityData.getCollectTime());
        }

        PumpData pumpData = first(pumpDataMapper.selectList(currentUserId, plotId, null, null, null, null, null));
        if (pumpData != null) {
            response.setWaterFlow(pumpData.getWaterFlow());
            response.setWaterPressure(pumpData.getWaterPressure());
            response.setPumpCollectTime(pumpData.getCollectTime());
        }

        LightData lightData = first(lightDataMapper.selectList(currentUserId, plotId, null, null, null, null, null));
        if (lightData != null) {
            response.setLightIntensity(lightData.getLightIntensity());
            response.setLightCollectTime(lightData.getCollectTime());
        }

        return response;
    }

    /** 从倒序结果中取第一条最新数据；没有数据时返回 null，前端会展示兜底占位。 */
    private <T> T first(List<T> dataList) {
        return dataList == null || dataList.isEmpty() ? null : dataList.get(0);
    }
}
