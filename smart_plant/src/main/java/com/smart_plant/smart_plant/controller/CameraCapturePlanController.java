package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.CameraCapturePlanSaveRequest;
import com.smart_plant.smart_plant.entity.CameraCapturePlan;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.CameraCapturePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 摄像头作物图像采集计划管理控制器。 */
@RestController
@RequestMapping("/camera-capture-plan")
@RequiredArgsConstructor
@RequirePermission("camera_capture_plan:manage")
public class CameraCapturePlanController {
    private final CameraCapturePlanService planService;

    /** 新增采集计划。 */
    @PostMapping("/add")
    public R<CameraCapturePlan> add(@RequestBody CameraCapturePlanSaveRequest request) {
        return R.success(planService.add(request));
    }

    /** 修改采集计划。 */
    @PutMapping
    public R<CameraCapturePlan> update(@RequestBody CameraCapturePlanSaveRequest request) {
        return R.success(planService.update(request));
    }

    /** 删除采集计划。 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return R.success();
    }

    /** 查询采集计划详情。 */
    @GetMapping("/{id}")
    public R<CameraCapturePlan> get(@PathVariable Long id) {
        return R.success(planService.get(id));
    }

    /** 分页查询采集计划。 */
    @GetMapping("/list")
    public R<PageInfo<CameraCapturePlan>> list(@RequestParam(required = false) Long plotId,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer enabled,
                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(planService.list(plotId, keyword, enabled, pageNum, pageSize));
    }
}
