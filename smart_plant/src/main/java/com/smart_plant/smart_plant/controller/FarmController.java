package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.FarmAddressResolveResult;
import com.smart_plant.smart_plant.dto.FarmImageUploadResult;
import com.smart_plant.smart_plant.entity.Farm;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.FarmService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/farm")
@RequiredArgsConstructor
@RequirePermission("farm:manage")
public class FarmController {

    private final FarmService farmService;

    @PostMapping("/add")
    public R<Farm> addFarm(@RequestBody Farm farm) {
        return R.success(farmService.addFarm(farm));
    }

    @PostMapping("/upload-image")
    public R<FarmImageUploadResult> uploadFarmImage(@RequestParam("file") MultipartFile file) {
        return R.success(farmService.uploadFarmImage(file));
    }

    @GetMapping("/location/address")
    public R<FarmAddressResolveResult> resolveAddressByCoordinate(@RequestParam String coordinate,
                                                                  @RequestParam(defaultValue = "wgs84") String coordinateSystem) {
        return R.success(farmService.resolveAddressByCoordinate(coordinate, coordinateSystem));
    }

    @PutMapping
    public R<Farm> updateFarm(@RequestBody Farm farm) {
        return R.success(farmService.updateFarm(farm));
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteFarm(@PathVariable Long id) {
        farmService.deleteFarm(id);
        return R.success();
    }

    @DeleteMapping("/batch")
    public R<Integer> deleteFarms(@RequestBody List<Long> ids) {
        return R.success(farmService.deleteFarms(ids));
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        farmService.updateStatus(id, status);
        return R.success();
    }

    @GetMapping("/{id}")
    public R<Farm> getFarmById(@PathVariable Long id) {
        return R.success(farmService.getFarmById(id));
    }

    @GetMapping("/list")
    @RequirePermission({"farm:manage", "map:view"})
    public R<PageInfo<Farm>> listFarms(@RequestParam(required = false) String farmName,
                                       @RequestParam(required = false) String farmCode,
                                       @RequestParam(required = false) Long userId,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(farmService.listFarms(farmName, farmCode, userId, status, pageNum, pageSize));
    }

    @GetMapping("/statistics")
    public R<Map<String, Object>> statisticsFarms() {
        return R.success(farmService.statisticsFarms());
    }
}
