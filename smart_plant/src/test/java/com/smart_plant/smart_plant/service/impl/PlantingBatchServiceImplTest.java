package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.PlotHarvestRequest;
import com.smart_plant.smart_plant.entity.PlantingBatch;
import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CropMapper;
import com.smart_plant.smart_plant.mapper.GrowthStageMapper;
import com.smart_plant.smart_plant.mapper.PlantingBatchMapper;
import com.smart_plant.smart_plant.mapper.PlotMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlantingBatchServiceImplTest {

    private final PlantingBatchMapper plantingBatchMapper = mock(PlantingBatchMapper.class);
    private final PlotMapper plotMapper = mock(PlotMapper.class);
    private final CropMapper cropMapper = mock(CropMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);
    private final PlantingBatchServiceImpl service = new PlantingBatchServiceImpl(
            plantingBatchMapper, plotMapper, cropMapper, userMapper, mock(GrowthStageMapper.class), dataPermissionService);

    PlantingBatchServiceImplTest() {
        when(dataPermissionService.isAdmin()).thenReturn(true);
    }

    @Test
    void addDefaultsPlantingAreaFromPlot() {
        PlantingBatch batch = validBatch();
        Plot plot = ownedPlot();
        plot.setArea(new BigDecimal("3.50"));
        plot.setAreaUnit("亩");
        when(plotMapper.selectById(2L)).thenReturn(plot);
        when(cropMapper.selectById(3L)).thenReturn(new com.smart_plant.smart_plant.entity.Crop());
        when(userMapper.selectById(1L)).thenReturn(new com.smart_plant.smart_plant.entity.User());
        when(plantingBatchMapper.selectById(batch.getId())).thenReturn(batch);

        service.addPlantingBatch(batch);

        assertEquals(new BigDecimal("3.50"), batch.getPlantingArea());
        assertEquals("亩", batch.getAreaUnit());
        verify(plantingBatchMapper).insert(batch);
    }

    @Test
    void addRejectsSecondActiveBatchForPlot() {
        PlantingBatch batch = validBatch();
        when(plotMapper.selectById(2L)).thenReturn(ownedPlot());
        when(cropMapper.selectById(3L)).thenReturn(new com.smart_plant.smart_plant.entity.Crop());
        when(userMapper.selectById(1L)).thenReturn(new com.smart_plant.smart_plant.entity.User());
        when(plantingBatchMapper.countActiveByPlotId(2L, null)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.addPlantingBatch(batch));

        assertEquals("一个地块只能存在一个种植中批次", exception.getMessage());
    }

    @Test
    void harvestedBatchRequiresActualHarvestDateAndYield() {
        PlantingBatch batch = validBatch();
        batch.setStatus(2);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.addPlantingBatch(batch));

        assertEquals("已采收批次必须填写实际采收日期", exception.getMessage());

        batch.setActualHarvestAt(LocalDate.of(2026, 9, 10));
        exception = assertThrows(BusinessException.class, () -> service.addPlantingBatch(batch));

        assertEquals("已采收批次必须填写实际产量", exception.getMessage());
    }

    @Test
    void harvestCompletesActiveBatch() {
        PlantingBatch batch = validBatch();
        batch.setId(8L);
        batch.setPlantedAt(LocalDate.now().minusDays(2));
        batch.setYieldUnit("kg");
        when(plantingBatchMapper.selectActiveByPlotIdForUpdate(2L)).thenReturn(batch);
        when(plantingBatchMapper.completeHarvest(8L, "张三", new BigDecimal("12.50"), "kg", LocalDate.now()))
                .thenReturn(1);
        when(plantingBatchMapper.selectById(8L)).thenReturn(batch);

        PlotHarvestRequest request = new PlotHarvestRequest();
        request.setHarvester("张三");
        request.setYieldAmount(new BigDecimal("12.50"));
        request.setYieldUnit("kg");
        request.setActualHarvestAt(LocalDate.now());

        assertEquals(batch, service.harvestActiveBatch(2L, request));
        verify(plantingBatchMapper).completeHarvest(
                8L, "张三", new BigDecimal("12.50"), "kg", LocalDate.now());
    }

    private PlantingBatch validBatch() {
        PlantingBatch batch = new PlantingBatch();
        batch.setUserId(1L);
        batch.setPlotId(2L);
        batch.setCropId(3L);
        batch.setStatus(1);
        return batch;
    }

    private Plot ownedPlot() {
        Plot plot = new Plot();
        plot.setId(2L);
        plot.setUserId(1L);
        return plot;
    }
}
