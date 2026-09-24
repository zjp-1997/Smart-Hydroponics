package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.CropType;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.CropTypeService;
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
 * 作物分类管理控制器，提供作物分类维护接口。
 */
@RestController
@RequestMapping("/crop-type")
@RequiredArgsConstructor
public class CropTypeController {

    private final CropTypeService cropTypeService;

    /**
     * 新增作物分类。
     */
    @PostMapping("/add")
    @RequirePermission("crop_type:manage")
    public R<CropType> addCropType(@RequestBody CropType cropType) {
        return R.success(cropTypeService.addCropType(cropType));
    }

    /**
     * 编辑作物分类。
     */
    @PutMapping
    @RequirePermission("crop_type:manage")
    public R<CropType> updateCropType(@RequestBody CropType cropType) {
        return R.success(cropTypeService.updateCropType(cropType));
    }

    /**
     * 删除作物分类。
     */
    @DeleteMapping("/{id}")
    @RequirePermission("crop_type:manage")
    public R<Void> deleteCropType(@PathVariable Long id) {
        cropTypeService.deleteCropType(id);
        return R.success();
    }

    /**
     * 批量删除作物分类。
     */
    @DeleteMapping("/batch")
    @RequirePermission("crop_type:manage")
    public R<Integer> deleteCropTypes(@RequestBody List<Long> ids) {
        return R.success(cropTypeService.deleteCropTypes(ids));
    }

    /**
     * 启用或禁用作物分类，status=1为启用，status=0为禁用。
     */
    @PutMapping("/{id}/status")
    @RequirePermission("crop_type:manage")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        cropTypeService.updateStatus(id, status);
        return R.success();
    }

    /**
     * 根据ID查询作物分类详情。
     */
    @GetMapping("/{id}")
    public R<CropType> getCropTypeById(@PathVariable Long id) {
        return R.success(cropTypeService.getCropTypeById(id));
    }

    /**
     * 分页查询作物分类列表。
     */
    @GetMapping("/list")
    public R<PageInfo<CropType>> listCropTypes(@RequestParam(required = false) String typeName,
                                               @RequestParam(required = false) Long parentId,
                                               @RequestParam(required = false) Integer status,
                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(cropTypeService.listCropTypes(typeName, parentId, status, pageNum, pageSize));
    }
}
