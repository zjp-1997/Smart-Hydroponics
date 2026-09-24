package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.PlotStatisticsResponse;
import com.smart_plant.smart_plant.dto.PlotHarvestRequest;
import com.smart_plant.smart_plant.dto.ClientPlotUpdateRequest;
import com.smart_plant.smart_plant.entity.Crop;
import com.smart_plant.smart_plant.entity.Farm;
import com.smart_plant.smart_plant.entity.IotDevice;
import com.smart_plant.smart_plant.entity.PlantingBatch;
import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.EnvironmentDataMapper;
import com.smart_plant.smart_plant.mapper.CropMapper;
import com.smart_plant.smart_plant.mapper.FarmMapper;
import com.smart_plant.smart_plant.mapper.FarmTaskMapper;
import com.smart_plant.smart_plant.mapper.IotDeviceMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.mapper.WaterQualityDataMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.PlantingBatchService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class PlotServiceImplTest {

    private final PlotMapper plotMapper = mock(PlotMapper.class);

    private final CropMapper cropMapper = mock(CropMapper.class);

    private final UserMapper userMapper = mock(UserMapper.class);

    private final FarmMapper farmMapper = mock(FarmMapper.class);

    private final EnvironmentDataMapper environmentDataMapper = mock(EnvironmentDataMapper.class);

    private final WaterQualityDataMapper waterQualityDataMapper = mock(WaterQualityDataMapper.class);

    private final FarmTaskMapper farmTaskMapper = mock(FarmTaskMapper.class);

    private final IotDeviceMapper iotDeviceMapper = mock(IotDeviceMapper.class);

    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);

    private final PlantingBatchService plantingBatchService = mock(PlantingBatchService.class);

    private final PlotServiceImpl plotService = new PlotServiceImpl(
            plotMapper,
            cropMapper,
            userMapper,
            farmMapper,
            environmentDataMapper,
            waterQualityDataMapper,
            farmTaskMapper,
            iotDeviceMapper,
            dataPermissionService,
            plantingBatchService
    );

    @Test
    void idleClientPlotDetailKeepsOnlyPlotIdentityAndShowsDevicesOffline() {
        Plot idle = new Plot();
        idle.setId(11L);
        idle.setUserId(7L);
        idle.setPlotName("东区地块");
        idle.setArea(new BigDecimal("2.50"));
        idle.setAreaUnit("亩");
        // 即便有历史设备在线，空闲详情也只展示离线状态且不改写设备实体。
        IotDevice device = new IotDevice();
        device.setId(3L);
        device.setName("土壤传感器");
        device.setOnlineStatus(1);
        device.setControlStatus(1);
        when(dataPermissionService.currentClientOwnerId()).thenReturn(7L);
        when(plotMapper.selectById(11L)).thenReturn(idle);
        when(iotDeviceMapper.selectList(7L, 11L, null, null, null, null, null, null, null))
                .thenReturn(List.of(device));

        var detail = plotService.getCurrentClientPlotDetail(11L);

        assertEquals(true, detail.getIdle());
        assertEquals("东区地块", detail.getPlotName());
        assertEquals("2.5亩", detail.getPlotArea());
        assertEquals("-", detail.getCropName());
        assertEquals(null, detail.getPlantingTime());
        assertEquals(null, detail.getHarvestTime());
        assertEquals("-", detail.getMonitorItems().get(0).getValue());
        assertEquals(0, detail.getFarmTasks().size());
        assertEquals("离线", detail.getDevices().get(0).getState());
        assertEquals(false, detail.getDevices().get(0).getEnabled());
        assertEquals(1, device.getOnlineStatus());
        verify(environmentDataMapper, never()).selectList(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void clientPlotEditRejectsNonOwnerRoleBeforeWriting() {
        User viewer = new User();
        viewer.setId(8L);
        viewer.setRoleCode("user");
        when(dataPermissionService.currentUser()).thenReturn(viewer);

        assertThrows(BusinessException.class,
                () -> plotService.updateCurrentClientPlot(11L, new ClientPlotUpdateRequest()));
        verify(plotMapper, never()).updateById(any(Plot.class));
    }

    @Test
    void clientPlotEditRejectsAnotherOwnersPlot() {
        User owner = new User();
        owner.setId(7L);
        owner.setRoleCode("farm_owner");
        when(dataPermissionService.currentUser()).thenReturn(owner);
        Plot anotherOwnersPlot = new Plot();
        anotherOwnersPlot.setId(11L);
        anotherOwnersPlot.setUserId(9L);
        when(plotMapper.selectById(11L)).thenReturn(anotherOwnersPlot);

        assertThrows(BusinessException.class,
                () -> plotService.updateCurrentClientPlot(11L, new ClientPlotUpdateRequest()));
        verify(plotMapper, never()).updateById(any(Plot.class));
    }

    @Test
    void clientPlotEditUpdatesOwnedPlotAndCurrentBatch() {
        User owner = new User();
        owner.setId(7L);
        owner.setRoleCode("farm_owner");
        when(dataPermissionService.currentUser()).thenReturn(owner);
        Plot existing = new Plot();
        existing.setId(11L);
        existing.setUserId(7L);
        existing.setFarmId(3L);
        existing.setAreaUnit("亩");
        existing.setCurrentBatchId(21L);
        when(plotMapper.selectById(11L)).thenReturn(existing);
        Farm farm = new Farm();
        farm.setId(3L);
        farm.setUserId(7L);
        farm.setStatus(1);
        when(farmMapper.selectById(3L)).thenReturn(farm);
        Crop crop = new Crop();
        crop.setId(5L);
        crop.setUserId(7L);
        crop.setStatus(1);
        when(cropMapper.selectById(5L)).thenReturn(crop);
        when(cropMapper.countVisibleByUserId(5L, 7L)).thenReturn(1);
        when(plotMapper.updateById(any(Plot.class))).thenReturn(1);
        ClientPlotUpdateRequest request = new ClientPlotUpdateRequest();
        request.setCropId(5L);
        request.setPlotName("东区地块");
        request.setPlantingArea(new BigDecimal("2.50"));
        request.setPlantedAt(LocalDate.of(2026, 9, 1));
        request.setExpectedHarvestAt(LocalDate.of(2026, 10, 1));

        plotService.updateCurrentClientPlot(11L, request);

        verify(plotMapper).updateById(argThat(plot -> "东区地块".equals(plot.getPlotName())
                && new BigDecimal("2.50").equals(plot.getArea())));
        verify(plantingBatchService).updatePlantingBatch(argThat(batch -> Long.valueOf(21L).equals(batch.getId())
                && Long.valueOf(5L).equals(batch.getCropId())
                && LocalDate.of(2026, 10, 1).equals(batch.getExpectedHarvestAt())));
    }

    @Test
    void statisticsPlotsUsesCurrentDataScope() {
        PlotStatisticsResponse expected = new PlotStatisticsResponse(
                15L,
                10L,
                4L,
                new BigDecimal("23.50"),
                "亩");
        when(dataPermissionService.restrictUserId(null)).thenReturn(7L);
        when(plotMapper.selectStatistics(7L)).thenReturn(expected);

        PlotStatisticsResponse result = plotService.statisticsPlots();

        assertSame(expected, result);
        verify(plotMapper).selectStatistics(7L);
    }

    @Test
    void statisticsPlotsReturnsZeroValuesWhenMapperReturnsNoRow() {
        when(dataPermissionService.restrictUserId(null)).thenReturn(null);
        when(plotMapper.selectStatistics(null)).thenReturn(null);

        PlotStatisticsResponse result = plotService.statisticsPlots();

        assertEquals(0L, result.getPlotTotalCount());
        assertEquals(0L, result.getIdlePlotCount());
        assertEquals(0L, result.getCropTypeCount());
        assertEquals(BigDecimal.ZERO, result.getPlantingArea());
        assertEquals("亩", result.getAreaUnit());
    }

    @Test
    void addPlotRejectsUserWithoutFarmOwnerRole() {
        User user = new User();
        user.setId(7L);
        user.setRoleCode("expert");
        user.setStatus(1);
        Farm farm = new Farm();
        farm.setId(3L);
        farm.setUserId(7L);
        farm.setStatus(1);
        when(dataPermissionService.isAdmin()).thenReturn(true);
        when(farmMapper.selectById(3L)).thenReturn(farm);
        when(userMapper.selectById(7L)).thenReturn(user);

        Plot plot = new Plot();
        plot.setUserId(7L);
        plot.setFarmId(3L);
        plot.setPlotName("测试地块");

        BusinessException exception = assertThrows(BusinessException.class, () -> plotService.addPlot(plot));

        assertEquals("所属用户必须为农场主", exception.getMessage());
    }

    @Test
    void addPlotSynchronizesCoordinateAndChecksFarmOwnership() {
        User user = new User();
        user.setId(7L);
        user.setRoleCode("farm_owner");
        user.setStatus(1);
        Farm farm = new Farm();
        farm.setId(3L);
        farm.setUserId(7L);
        farm.setStatus(1);
        Plot plot = new Plot();
        plot.setUserId(7L);
        plot.setFarmId(3L);
        plot.setPlotName("测试地块");
        plot.setCoordinate("120.123400,30.567800");
        when(dataPermissionService.isAdmin()).thenReturn(true);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(farmMapper.selectById(3L)).thenReturn(farm);
        doAnswer(invocation -> {
            Plot inserted = invocation.getArgument(0);
            inserted.setId(11L);
            return 1;
        }).when(plotMapper).insert(any(Plot.class));
        when(plotMapper.selectById(11L)).thenAnswer(invocation -> plot);

        plotService.addPlot(plot);

        assertEquals("120.1234,30.5678", plot.getCoordinate());
        assertEquals(new BigDecimal("120.123400"), plot.getLongitude());
        assertEquals(new BigDecimal("30.567800"), plot.getLatitude());
        verify(plotMapper).insert(plot);
    }

    @Test
    void addPlotCreatesCurrentPlantingBatchWhenCropInfoProvided() {
        User user = new User();
        user.setId(7L);
        user.setRoleCode("farm_owner");
        user.setStatus(1);
        Farm farm = new Farm();
        farm.setId(3L);
        farm.setUserId(7L);
        farm.setStatus(1);
        Plot plot = new Plot();
        plot.setUserId(7L);
        plot.setFarmId(3L);
        plot.setPlotName("测试地块");
        plot.setCurrentCropId(5L);
        plot.setArea(new BigDecimal("2.50"));
        plot.setCurrentPlantedAt(LocalDate.of(2026, 9, 1));
        plot.setCurrentExpectedHarvestAt(LocalDate.of(2026, 10, 1));
        plot.setCurrentGrowthStageId(9L);
        plot.setCurrentExpectedYieldAmount(new BigDecimal("120.00"));
        plot.setCurrentGrownDays(2);
        plot.setCurrentBatchStatus(1);
        when(dataPermissionService.isAdmin()).thenReturn(true);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(farmMapper.selectById(3L)).thenReturn(farm);
        doAnswer(invocation -> {
            Plot inserted = invocation.getArgument(0);
            inserted.setId(11L);
            return 1;
        }).when(plotMapper).insert(any(Plot.class));
        when(plotMapper.selectById(11L)).thenReturn(plot);

        plotService.addPlot(plot);

        verify(plantingBatchService).addPlantingBatch(argThat(batch ->
                Long.valueOf(11L).equals(batch.getPlotId())
                        && Long.valueOf(7L).equals(batch.getUserId())
                        && Long.valueOf(5L).equals(batch.getCropId())
                        && new BigDecimal("2.50").equals(batch.getPlantingArea())
                        && LocalDate.of(2026, 9, 1).equals(batch.getPlantedAt())
                        && LocalDate.of(2026, 10, 1).equals(batch.getExpectedHarvestAt())
                        && Long.valueOf(9L).equals(batch.getGrowthStageId())
                        && new BigDecimal("120.00").equals(batch.getExpectedYieldAmount())
                        && Integer.valueOf(2).equals(batch.getGrownDays())
                        && Integer.valueOf(1).equals(batch.getStatus())
        ));
    }

    @Test
    void updatePlotMarksCurrentBatchCancelledWhenPlantingStatusIsIdle() {
        User user = new User();
        user.setId(7L);
        user.setRoleCode("farm_owner");
        user.setStatus(1);
        Farm farm = new Farm();
        farm.setId(3L);
        farm.setUserId(7L);
        farm.setStatus(1);
        Plot oldPlot = new Plot();
        oldPlot.setId(11L);
        oldPlot.setUserId(7L);
        oldPlot.setFarmId(3L);
        oldPlot.setPlotName("旧地块");
        oldPlot.setCurrentBatchId(21L);
        Plot plot = new Plot();
        plot.setId(11L);
        plot.setUserId(7L);
        plot.setFarmId(3L);
        plot.setPlotName("测试地块");
        plot.setCurrentBatchStatus(0);
        when(dataPermissionService.isAdmin()).thenReturn(true);
        when(plotMapper.selectById(11L)).thenReturn(oldPlot, plot);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(farmMapper.selectById(3L)).thenReturn(farm);
        when(plotMapper.updateById(plot)).thenReturn(1);

        plotService.updatePlot(plot);

        verify(plantingBatchService).updateStatus(21L, 4);
    }

    @Test
    void updatePlotRejectsAreaExceedingFarmArea() {
        User user = new User();
        user.setId(7L);
        user.setRoleCode("farm_owner");
        user.setStatus(1);
        Farm farm = new Farm();
        farm.setId(3L);
        farm.setUserId(7L);
        farm.setStatus(1);
        farm.setTotalArea(new BigDecimal("10"));
        farm.setAreaUnit("亩");
        Plot oldPlot = new Plot();
        oldPlot.setId(11L);
        oldPlot.setUserId(7L);
        oldPlot.setFarmId(3L);
        Plot otherPlot = new Plot();
        otherPlot.setId(12L);
        otherPlot.setArea(new BigDecimal("6"));
        otherPlot.setAreaUnit("亩");
        Plot update = new Plot();
        update.setId(11L);
        update.setFarmId(3L);
        update.setArea(new BigDecimal("5"));
        update.setAreaUnit("亩");
        when(dataPermissionService.isAdmin()).thenReturn(true);
        when(plotMapper.selectById(11L)).thenReturn(oldPlot);
        when(farmMapper.selectById(3L)).thenReturn(farm);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(plotMapper.selectList(null, null, 3L, null, null, null)).thenReturn(java.util.List.of(oldPlot, otherPlot));

        BusinessException exception = assertThrows(BusinessException.class, () -> plotService.updatePlot(update));

        assertEquals("地块总面积不能超过农场面积", exception.getMessage());
        verify(plotMapper, never()).updateById(any(Plot.class));
    }

    @Test
    void harvestPlotOnlyAllowsMatureBatch() {
        Plot plot = new Plot();
        plot.setId(11L);
        plot.setUserId(7L);
        plot.setCurrentBatchId(21L);
        plot.setCurrentGrowthStageName("幼苗期");
        when(plotMapper.selectById(11L)).thenReturn(plot);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> plotService.harvestPlot(11L, new PlotHarvestRequest()));

        assertEquals("当前批次尚未进入成熟期", exception.getMessage());
        verify(plantingBatchService, never()).harvestActiveBatch(any(), any());
    }

    @Test
    void harvestPlotDelegatesMatureBatchToPlantingBatchService() {
        Plot plot = new Plot();
        plot.setId(11L);
        plot.setUserId(7L);
        plot.setCurrentBatchId(21L);
        plot.setCurrentBatchStatus(1);
        plot.setCurrentGrowthStageName("成熟期");
        PlotHarvestRequest request = new PlotHarvestRequest();
        PlantingBatch harvested = new PlantingBatch();
        harvested.setId(21L);
        when(plotMapper.selectById(11L)).thenReturn(plot);
        when(plantingBatchService.harvestActiveBatch(11L, request)).thenReturn(harvested);

        assertSame(harvested, plotService.harvestPlot(11L, request));
        verify(plantingBatchService).harvestActiveBatch(11L, request);
    }

    @Test
    void clientPlotListReturnsPlantingHarvestReadyAndIdleStatuses() {
        User currentUser = new User();
        currentUser.setId(7L);

        Plot planting = new Plot();
        planting.setId(1L);
        planting.setCurrentBatchId(11L);
        planting.setCurrentBatchStatus(1);
        planting.setCurrentExpectedHarvestAt(LocalDate.now().plusDays(1));

        Plot harvestReady = new Plot();
        harvestReady.setId(2L);
        harvestReady.setCurrentBatchId(12L);
        harvestReady.setCurrentBatchStatus(1);
        harvestReady.setCurrentExpectedHarvestAt(LocalDate.now());

        Plot idle = new Plot();
        idle.setId(3L);

        when(dataPermissionService.currentClientOwnerId()).thenReturn(currentUser.getId());
        when(plotMapper.selectList(null, null, null, 7L, null, 1))
                .thenReturn(List.of(planting, harvestReady, idle));

        List<com.smart_plant.smart_plant.dto.ClientPlotListResponse> result =
                plotService.listCurrentClientPlots();

        // 状态由后端统一计算，farm 端无需根据日期或生长阶段重复实现业务规则。
        assertEquals("PLANTING", result.get(0).getPlantingStatus());
        assertEquals("HARVEST_READY", result.get(1).getPlantingStatus());
        assertEquals("IDLE", result.get(2).getPlantingStatus());
        assertEquals("待采收", result.get(1).getPlantingStatusName());
    }
}
