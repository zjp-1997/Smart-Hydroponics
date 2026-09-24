package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.DiseaseControl;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.DiseaseControlService;
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
 * 病虫害防治措施管理控制器。
 *
 * <p>该控制器参考用户管理模块的接口风格，提供新增、删除、修改、状态修改、详情和分页列表接口。</p>
 */
@RestController
@RequestMapping("/disease-control")
@RequiredArgsConstructor
public class DiseaseControlController {

    /** 注入防治措施业务服务，控制器只负责接收请求和返回统一响应。 */
    private final DiseaseControlService diseaseControlService;

    /** 新增预防或治疗措施。 */
    @PostMapping("/add")
    @RequirePermission("disease_control:manage")
    public R<DiseaseControl> addDiseaseControl(@RequestBody DiseaseControl diseaseControl) {
        return R.success(diseaseControlService.addDiseaseControl(diseaseControl));
    }

    /** 根据ID删除单条防治措施。 */
    @DeleteMapping("/{id}")
    @RequirePermission("disease_control:manage")
    public R<Void> deleteDiseaseControl(@PathVariable Long id) {
        diseaseControlService.deleteDiseaseControl(id);
        return R.success();
    }

    /** 根据ID集合批量删除防治措施。 */
    @DeleteMapping("/batch")
    @RequirePermission("disease_control:manage")
    public R<Integer> deleteDiseaseControls(@RequestBody List<Long> ids) {
        return R.success(diseaseControlService.deleteDiseaseControls(ids));
    }

    /** 修改防治措施。 */
    @PutMapping
    @RequirePermission("disease_control:manage")
    public R<DiseaseControl> updateDiseaseControl(@RequestBody DiseaseControl diseaseControl) {
        return R.success(diseaseControlService.updateDiseaseControl(diseaseControl));
    }

    /** 修改防治措施状态，status=1表示启用，status=0表示停用。 */
    @PutMapping("/{id}/status")
    @RequirePermission("disease_control:manage")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        diseaseControlService.updateStatus(id, status);
        return R.success();
    }

    /** 根据ID查询防治措施详情。 */
    @GetMapping("/{id}")
    public R<DiseaseControl> getDiseaseControlById(@PathVariable Long id) {
        return R.success(diseaseControlService.getDiseaseControlById(id));
    }

    /** 分页查询防治措施列表，支持按病虫害、预防/治疗类型、防治手段和状态筛选。 */
    @GetMapping("/list")
    public R<PageInfo<DiseaseControl>> listDiseaseControls(@RequestParam(required = false) Long diseaseId,
                                                           @RequestParam(required = false) String diseaseName,
                                                           @RequestParam(required = false) Integer controlType,
                                                           @RequestParam(required = false) Integer controlCategory,
                                                           @RequestParam(required = false) Integer status,
                                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(diseaseControlService.listDiseaseControls(
                diseaseId, diseaseName, controlType, controlCategory, status, pageNum, pageSize));
    }
}
