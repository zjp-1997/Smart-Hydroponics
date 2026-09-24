package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.Crop;
import com.smart_plant.smart_plant.entity.GrowthStage;
import com.smart_plant.smart_plant.entity.PlantingBatch;
import com.smart_plant.smart_plant.entity.Plot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class PlantingBatchDisplayMapperIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlotMapper plotMapper;

    @Autowired
    private PlantingBatchMapper plantingBatchMapper;

    @Autowired
    private CropMapper cropMapper;

    @Autowired
    private GrowthStageMapper growthStageMapper;

    @Test
    void currentBatchUsesDynamicGrownDaysAndFormattedNumber() {
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM `user` ORDER BY id LIMIT 1", Long.class);

        Crop crop = new Crop();
        crop.setCropName("阶段匹配测试作物-" + UUID.randomUUID());
        crop.setStatus(1);
        assertEquals(1, cropMapper.insert(crop));

        GrowthStage firstStage = growthStage(crop.getId(), "萌芽期", 1, 0, 1, 2);
        GrowthStage currentStage = growthStage(crop.getId(), "生长期", 2, 2, null, 5);
        assertEquals(1, growthStageMapper.insert(firstStage));
        assertEquals(1, growthStageMapper.insert(currentStage));

        Plot plot = new Plot();
        plot.setUserId(userId);
        plot.setPlotName("批次展示测试-" + UUID.randomUUID());
        plot.setStatus(1);
        assertEquals(1, plotMapper.insert(plot));

        PlantingBatch batch = new PlantingBatch();
        batch.setPlotId(plot.getId());
        batch.setCropId(crop.getId());
        batch.setUserId(userId);
        batch.setPlantedAt(LocalDate.now().minusDays(2));
        batch.setStatus(1);
        assertEquals(1, plantingBatchMapper.insert(batch));

        String expectedBatchNo = "Batch_" + String.format("%03d", batch.getId());
        PlantingBatch savedBatch = plantingBatchMapper.selectById(batch.getId());
        assertEquals(expectedBatchNo, savedBatch.getBatchNo());
        assertEquals(2, savedBatch.getGrownDays());
        assertEquals(currentStage.getId(), savedBatch.getGrowthStageId());
        assertEquals("生长期", savedBatch.getGrowthStageName());

        Plot savedPlot = plotMapper.selectById(plot.getId());
        assertEquals(expectedBatchNo, savedPlot.getCurrentBatchNo());
        assertEquals(2, savedPlot.getCurrentGrownDays());
        assertEquals(currentStage.getId(), savedPlot.getCurrentGrowthStageId());
        assertEquals("生长期", savedPlot.getCurrentGrowthStageName());

        assertEquals(1, plantingBatchMapper.completeHarvest(batch.getId(), "测试采收人",
                new BigDecimal("12.50"), "kg", LocalDate.now()));
        PlantingBatch harvestedBatch = plantingBatchMapper.selectById(batch.getId());
        assertEquals(2, harvestedBatch.getStatus());
        assertEquals("测试采收人", harvestedBatch.getHarvester());
        assertEquals(new BigDecimal("12.50"), harvestedBatch.getYieldAmount());
        assertEquals(LocalDate.now(), harvestedBatch.getActualHarvestAt());
        assertEquals(2, harvestedBatch.getGrownDays());
        assertNull(plotMapper.selectById(plot.getId()).getCurrentBatchId());
    }

    private GrowthStage growthStage(Long cropId, String name, int order, int startDay,
                                    Integer endDay, int duration) {
        GrowthStage stage = new GrowthStage();
        stage.setCropId(cropId);
        stage.setStageName(name);
        stage.setStageOrder(order);
        stage.setStartDay(startDay);
        stage.setEndDay(endDay);
        stage.setDuration(duration);
        stage.setStatus(1);
        return stage;
    }
}
