package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.DiseasePest;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.DiseasePestService;
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
 * 病虫害基础信息管理控制器。
 *
 * <p>该控制器参考用户管理模块的接口风格，提供 disease_pest 表的基础管理接口。</p>
 */
@RestController
@RequestMapping("/disease-pest")
@RequiredArgsConstructor
public class DiseasePestController {

    /** 注入病虫害基础信息业务服务，控制器保持轻量。 */
    private final DiseasePestService diseasePestService;

    /** 新增病虫害基础信息。 */
    @PostMapping("/add")
    @RequirePermission("disease_pest:manage")
    public R<DiseasePest> addDiseasePest(@RequestBody DiseasePest diseasePest) {
        return R.success(diseasePestService.addDiseasePest(diseasePest));
    }

    /** 根据ID删除单条病虫害基础信息，关联防治措施由数据库外键级联删除。 */
    @DeleteMapping("/{id}")
    @RequirePermission("disease_pest:manage")
    public R<Void> deleteDiseasePest(@PathVariable Long id) {
        diseasePestService.deleteDiseasePest(id);
        return R.success();
    }

    /** 根据ID集合批量删除病虫害基础信息。 */
    @DeleteMapping("/batch")
    @RequirePermission("disease_pest:manage")
    public R<Integer> deleteDiseasePests(@RequestBody List<Long> ids) {
        return R.success(diseasePestService.deleteDiseasePests(ids));
    }

    /** 修改病虫害基础信息。 */
    @PutMapping
    @RequirePermission("disease_pest:manage")
    public R<DiseasePest> updateDiseasePest(@RequestBody DiseasePest diseasePest) {
        return R.success(diseasePestService.updateDiseasePest(diseasePest));
    }

    /** 修改病虫害基础信息状态，status=1表示启用，status=0表示停用。 */
    @PutMapping("/{id}/status")
    @RequirePermission("disease_pest:manage")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        diseasePestService.updateStatus(id, status);
        return R.success();
    }

    /** 根据ID查询病虫害详情，详情中包含预防和治疗措施列表。 */
    @GetMapping("/{id}")
    public R<DiseasePest> getDiseasePestById(@PathVariable Long id) {
        return R.success(diseasePestService.getDiseasePestById(id));
    }

    /** 分页查询病虫害基础信息列表，支持名称、作物类型、类型和状态筛选。 */
    @GetMapping("/list")
    public R<PageInfo<DiseasePest>> listDiseasePests(@RequestParam(required = false) String name,
                                                     @RequestParam(required = false) Long cropTypeId,
                                                     @RequestParam(required = false) Integer type,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(diseasePestService.listDiseasePests(
                name, cropTypeId, type, status, pageNum, pageSize));
    }
}
