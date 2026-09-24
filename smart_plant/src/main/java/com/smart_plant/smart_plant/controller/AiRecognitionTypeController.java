package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AiRecognitionType;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.AiRecognitionTypeService;
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
import java.util.Map;

/**
 * AI识别类型管理控制器。
 *
 * <p>该控制器提供AI识别类型的新增、修改、删除、批量删除、状态维护、详情、分页查询和统计接口。</p>
 */
@RestController
@RequestMapping("/ai-recognition-type")
@RequiredArgsConstructor
public class AiRecognitionTypeController {

    /** AI识别类型业务服务，负责具体校验和数据库操作。 */
    private final AiRecognitionTypeService aiRecognitionTypeService;

    /** 新增AI识别类型。 */
    @PostMapping("/add")
    @RequirePermission("ai_recognition_type:manage")
    public R<AiRecognitionType> addAiRecognitionType(@RequestBody AiRecognitionType aiRecognitionType) {
        return R.success(aiRecognitionTypeService.addAiRecognitionType(aiRecognitionType));
    }

    /** 修改AI识别类型基础信息。 */
    @PutMapping
    @RequirePermission("ai_recognition_type:manage")
    public R<AiRecognitionType> updateAiRecognitionType(@RequestBody AiRecognitionType aiRecognitionType) {
        return R.success(aiRecognitionTypeService.updateAiRecognitionType(aiRecognitionType));
    }

    /** 删除单个AI识别类型，已被识别结果引用的类型会在业务层禁止删除。 */
    @DeleteMapping("/{id}")
    @RequirePermission("ai_recognition_type:manage")
    public R<Void> deleteAiRecognitionType(@PathVariable Long id) {
        aiRecognitionTypeService.deleteAiRecognitionType(id);
        return R.success();
    }

    /** 批量删除AI识别类型。 */
    @DeleteMapping("/batch")
    @RequirePermission("ai_recognition_type:manage")
    public R<Integer> deleteAiRecognitionTypes(@RequestBody List<Long> ids) {
        return R.success(aiRecognitionTypeService.deleteAiRecognitionTypes(ids));
    }

    /** 修改AI识别类型状态，status 为 1 启用、0 禁用。 */
    @PutMapping("/{id}/status")
    @RequirePermission("ai_recognition_type:manage")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        aiRecognitionTypeService.updateStatus(id, status);
        return R.success();
    }

    /** 根据ID查询AI识别类型详情。 */
    @GetMapping("/{id}")
    public R<AiRecognitionType> getAiRecognitionTypeById(@PathVariable Long id) {
        return R.success(aiRecognitionTypeService.getAiRecognitionTypeById(id));
    }

    /** 分页查询AI识别类型列表，支持按编码、名称和状态筛选。 */
    @GetMapping("/list")
    public R<PageInfo<AiRecognitionType>> listAiRecognitionTypes(@RequestParam(required = false) String typeCode,
                                                                 @RequestParam(required = false) String typeName,
                                                                 @RequestParam(required = false) Integer status,
                                                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(aiRecognitionTypeService.listAiRecognitionTypes(
                typeCode, typeName, status, pageNum, pageSize));
    }

    /** 统计AI识别类型数据，返回总数、启用数、禁用数和识别结果引用数。 */
    @GetMapping("/statistics")
    public R<Map<String, Object>> statisticsAiRecognitionTypes(@RequestParam(required = false) String typeCode,
                                                               @RequestParam(required = false) String typeName,
                                                               @RequestParam(required = false) Integer status) {
        return R.success(aiRecognitionTypeService.statisticsAiRecognitionTypes(typeCode, typeName, status));
    }
}
