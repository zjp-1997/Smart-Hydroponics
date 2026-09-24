package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ClientPlotDetailResponse;
import com.smart_plant.smart_plant.dto.ClientPlotUpdateRequest;
import com.smart_plant.smart_plant.dto.ClientPlotListResponse;
import com.smart_plant.smart_plant.dto.PlotStatisticsResponse;
import com.smart_plant.smart_plant.dto.PlotHarvestRequest;
import com.smart_plant.smart_plant.entity.PlantingBatch;
import com.smart_plant.smart_plant.entity.Crop;
import com.smart_plant.smart_plant.entity.Plot;

import java.util.List;

public interface PlotService {

    Plot addPlot(Plot plot);

    void deletePlot(Long id);

    int deletePlots(List<Long> ids);

    Plot updatePlot(Plot plot);

    void updateStatus(Long id, Integer status);

    PlantingBatch harvestPlot(Long id, PlotHarvestRequest request);

    Plot getPlotById(Long id);

    PageInfo<Plot> listPlots(String plotName, String plotCode, Long farmId, Long userId, Integer type,
                             Integer status, Integer pageNum, Integer pageSize);

    PlotStatisticsResponse statisticsPlots();

    /**
     * 查询当前登录用户拥有的全部启用地块，用于 farm 用户端“全部地块”页面。
     *
     * @return 当前用户可见的地块列表，已转换为移动端展示所需字段
     */
    List<ClientPlotListResponse> listCurrentClientPlots();

    /**
     * 查询当前登录用户指定农场下的启用地块，用于 farm 用户端从农场卡片进入地块列表。
     *
     * @param farmId 农场主键，服务层会校验该农场是否归属于当前用户
     * @return 指定农场下当前用户可见的地块列表
     */
    List<ClientPlotListResponse> listCurrentClientPlotsByFarmId(Long farmId);

    /**
     * 查询当前登录用户可见的地块详情，包含监测数据、农事记录和设备状态。
     *
     * @param id 地块主键，服务层会校验该地块是否归属于当前用户
     * @return 用户端地块详情聚合数据
     */
    ClientPlotDetailResponse getCurrentClientPlotDetail(Long id);

    /** 农场主更新自己地块的基本信息及当前种植批次。 */
    void updateCurrentClientPlot(Long id, ClientPlotUpdateRequest request);

    /** 返回农场主可选的已启用作物，供地块编辑弹框使用。 */
    List<Crop> listCurrentClientCrops();
}
