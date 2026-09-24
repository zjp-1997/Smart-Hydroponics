package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.GrowthStage;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.GrowthStageService;
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

/**
 * 作物生长期管理控制器，提供作物生长期维护接口。
 */
@RestController
@RequestMapping("/growth-stage")
@RequiredArgsConstructor
public class GrowthStageController {

    private final GrowthStageService growthStageService;

    /**
     * 新增作物生长期。
     */
    @PostMapping("/add")
    @RequirePermission("growth_stage:manage")
    public R<GrowthStage> addGrowthStage(@RequestBody GrowthStage growthStage) {
        return R.success(growthStageService.addGrowthStage(growthStage));
    }

    /**
     * 编辑作物生长期。
     */
    @PutMapping
    @RequirePermission("growth_stage:manage")
    public R<GrowthStage> updateGrowthStage(@RequestBody GrowthStage growthStage) {
        return R.success(growthStageService.updateGrowthStage(growthStage));
    }

    /**
     * 删除作物生长期。
     */
    @DeleteMapping("/{id}")
    @RequirePermission("growth_stage:manage")
    public R<Void> deleteGrowthStage(@PathVariable Long id) {
        growthStageService.deleteGrowthStage(id);
        return R.success();
    }

    /**
     * 批量删除作物生长期。
     */
    @DeleteMapping("/batch")
    @RequirePermission("growth_stage:manage")
    public R<Integer> deleteGrowthStages(@RequestBody List<Long> ids) {
        return R.success(growthStageService.deleteGrowthStages(ids));
    }

    /**
     * 启用或禁用作物生长期，status=1为启用，status=0为禁用。
     */
    @PutMapping("/{id}/status")
    @RequirePermission("growth_stage:manage")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        growthStageService.updateStatus(id, status);
        return R.success();
    }

    /**
     * 根据ID查询作物生长期详情。
     */
    @GetMapping("/{id}")
    public R<GrowthStage> getGrowthStageById(@PathVariable Long id) {
        return R.success(growthStageService.getGrowthStageById(id));
    }

    /**
     * 分页查询作物生长期列表。
     */
    @GetMapping("/list")
    public R<PageInfo<GrowthStage>> listGrowthStages(@RequestParam(required = false) Long cropId,
                                                     @RequestParam(required = false) String stageName,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(growthStageService.listGrowthStages(cropId, stageName, status, pageNum, pageSize));
    }
}
