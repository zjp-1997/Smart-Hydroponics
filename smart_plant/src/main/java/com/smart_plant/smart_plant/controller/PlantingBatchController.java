package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.PlantingBatch;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.PlantingBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/planting-batch")
@RequiredArgsConstructor
@RequirePermission("planting_batch:manage")
public class PlantingBatchController {

    private final PlantingBatchService plantingBatchService;

    @PostMapping("/add")
    public R<PlantingBatch> addPlantingBatch(@RequestBody PlantingBatch plantingBatch) {
        return R.success(plantingBatchService.addPlantingBatch(plantingBatch));
    }

    @PutMapping
    public R<PlantingBatch> updatePlantingBatch(@RequestBody PlantingBatch plantingBatch) {
        return R.success(plantingBatchService.updatePlantingBatch(plantingBatch));
    }

    @DeleteMapping("/{id}")
    public R<Void> deletePlantingBatch(@PathVariable Long id) {
        plantingBatchService.deletePlantingBatch(id);
        return R.success();
    }

    @DeleteMapping("/batch")
    public R<Integer> deletePlantingBatches(@RequestBody List<Long> ids) {
        return R.success(plantingBatchService.deletePlantingBatches(ids));
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        plantingBatchService.updateStatus(id, status);
        return R.success();
    }

    @GetMapping("/{id}")
    public R<PlantingBatch> getPlantingBatchById(@PathVariable Long id) {
        return R.success(plantingBatchService.getPlantingBatchById(id));
    }

    @GetMapping("/list")
    public R<PageInfo<PlantingBatch>> listPlantingBatches(@RequestParam(required = false) Long plotId,
                                                          @RequestParam(required = false) Long cropId,
                                                          @RequestParam(required = false) Long userId,
                                                          @RequestParam(required = false) String batchNo,
                                                          @RequestParam(required = false) Integer status,
                                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(plantingBatchService.listPlantingBatches(
                plotId, cropId, userId, batchNo, status, pageNum, pageSize));
    }
}
