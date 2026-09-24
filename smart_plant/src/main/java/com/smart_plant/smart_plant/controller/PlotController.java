package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.FarmAddressResolveResult;
import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.dto.PlotStatisticsResponse;
import com.smart_plant.smart_plant.dto.PlotHarvestRequest;
import com.smart_plant.smart_plant.entity.PlantingBatch;
import com.smart_plant.smart_plant.entity.Plot;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.FarmService;
import com.smart_plant.smart_plant.service.CropImageService;
import com.smart_plant.smart_plant.service.PlotService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/plot")
@RequiredArgsConstructor
@RequirePermission("plot:manage")
public class PlotController {

    private final PlotService plotService;

    private final FarmService farmService;

    private final CropImageService cropImageService;

    @PostMapping("/add")
    public R<Plot> addPlot(@RequestBody Plot plot) {
        return R.success(plotService.addPlot(plot));
    }

    @PostMapping("/upload-crop-image")
    public R<CropImageUploadResult> uploadCropImage(@RequestParam("file") MultipartFile file) {
        return R.success(cropImageService.uploadPlotCropImage(file));
    }

    @DeleteMapping("/{id}")
    public R<Void> deletePlot(@PathVariable Long id) {
        plotService.deletePlot(id);
        return R.success();
    }

    @DeleteMapping("/batch")
    public R<Integer> deletePlots(@RequestBody List<Long> ids) {
        return R.success(plotService.deletePlots(ids));
    }

    @PutMapping
    public R<Plot> updatePlot(@RequestBody Plot plot) {
        return R.success(plotService.updatePlot(plot));
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        plotService.updateStatus(id, status);
        return R.success();
    }

    @PutMapping("/{id}/harvest")
    public R<PlantingBatch> harvestPlot(@PathVariable Long id, @Valid @RequestBody PlotHarvestRequest request) {
        return R.success(plotService.harvestPlot(id, request));
    }

    @GetMapping("/{id}")
    public R<Plot> getPlotById(@PathVariable Long id) {
        return R.success(plotService.getPlotById(id));
    }

    @GetMapping("/list")
    @RequirePermission({"plot:manage", "map:view"})
    public R<PageInfo<Plot>> listPlots(@RequestParam(required = false) String plotName,
                                       @RequestParam(required = false) String plotCode,
                                       @RequestParam(required = false) Long farmId,
                                       @RequestParam(required = false) Long userId,
                                       @RequestParam(required = false) Integer type,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(plotService.listPlots(plotName, plotCode, farmId, userId, type, status, pageNum, pageSize));
    }

    @GetMapping("/statistics")
    public R<PlotStatisticsResponse> statisticsPlots() {
        return R.success(plotService.statisticsPlots());
    }

    @GetMapping("/location/address")
    public R<FarmAddressResolveResult> resolveAddressByCoordinate(@RequestParam String coordinate,
                                                                  @RequestParam(defaultValue = "wgs84") String coordinateSystem) {
        return R.success(farmService.resolveAddressByCoordinate(coordinate, coordinateSystem));
    }
}
