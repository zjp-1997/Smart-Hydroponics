package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.Crop;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.CropService;
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
 * 作物信息管理控制器，提供作物信息维护接口。
 */
@RestController
@RequestMapping("/crop")
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;

    /**
     * 新增作物信息。
     */
    @PostMapping("/add")
    @RequirePermission("crop:add")
    public R<Crop> addCrop(@RequestBody Crop crop) {
        return R.success(cropService.addCrop(crop));
    }

    /**
     * 编辑作物信息。
     */
    @PutMapping
    @RequirePermission("crop:update")
    public R<Crop> updateCrop(@RequestBody Crop crop) {
        return R.success(cropService.updateCrop(crop));
    }

    /**
     * 删除作物信息。
     */
    @DeleteMapping("/{id}")
    @RequirePermission("crop:delete")
    public R<Void> deleteCrop(@PathVariable Long id) {
        cropService.deleteCrop(id);
        return R.success();
    }

    /**
     * 批量删除作物信息。
     */
    @DeleteMapping("/batch")
    @RequirePermission("crop:delete")
    public R<Integer> deleteCrops(@RequestBody List<Long> ids) {
        return R.success(cropService.deleteCrops(ids));
    }

    /**
     * 启用或禁用作物，status=1为启用，status=0为禁用。
     */
    @PutMapping("/{id}/status")
    @RequirePermission("crop:update")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        cropService.updateStatus(id, status);
        return R.success();
    }

    /**
     * 根据ID查询作物详情。
     */
    @GetMapping("/{id}")
    public R<Crop> getCropById(@PathVariable Long id) {
        return R.success(cropService.getCropById(id));
    }

    /**
     * 分页查询作物列表。
     */
    @GetMapping("/list")
    public R<PageInfo<Crop>> listCrops(@RequestParam(required = false) String cropName,
                                       @RequestParam(required = false) Long typeId,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(cropService.listCrops(cropName, typeId, status, pageNum, pageSize));
    }
}
