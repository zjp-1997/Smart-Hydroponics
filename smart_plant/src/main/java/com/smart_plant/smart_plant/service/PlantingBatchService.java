package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.PlantingBatch;
import com.smart_plant.smart_plant.dto.PlotHarvestRequest;

import java.util.List;

public interface PlantingBatchService {

    PlantingBatch addPlantingBatch(PlantingBatch plantingBatch);

    PlantingBatch updatePlantingBatch(PlantingBatch plantingBatch);

    void deletePlantingBatch(Long id);

    int deletePlantingBatches(List<Long> ids);

    void updateStatus(Long id, Integer status);

    PlantingBatch harvestActiveBatch(Long plotId, PlotHarvestRequest request);

    PlantingBatch getPlantingBatchById(Long id);

    PageInfo<PlantingBatch> listPlantingBatches(Long plotId, Long cropId, Long userId,
                                                String batchNo, Integer status,
                                                Integer pageNum, Integer pageSize);
}
