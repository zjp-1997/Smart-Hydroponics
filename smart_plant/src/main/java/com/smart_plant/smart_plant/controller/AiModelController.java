package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AiModel;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.AiModelService;
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
 * AI模型配置管理控制器。
 *
 * <p>该控制器面向 smart_farm 后台，提供模型配置的增删改查、批量删除和统计接口。</p>
 */
@RestController
@RequestMapping("/model")
@RequiredArgsConstructor
@RequirePermission("model:manage")
public class AiModelController {

    /** AI模型配置服务，负责业务校验、敏感字段脱敏和数据库操作。 */
    private final AiModelService aiModelService;

    @PostMapping("/add")
    public R<AiModel> addModel(@RequestBody AiModel model) {
        // 新增模型配置时 API Key 必填，服务层会统一校验 URL、状态和字段长度。
        return R.success(aiModelService.addModel(model));
    }

    @PutMapping
    public R<AiModel> updateModel(@RequestBody AiModel model) {
        // 编辑模型时 API Key 可为空；为空表示沿用旧密钥，避免后台列表脱敏值覆盖真实密钥。
        return R.success(aiModelService.updateModel(model));
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteModel(@PathVariable Long id) {
        aiModelService.deleteModel(id);
        return R.success();
    }

    @DeleteMapping("/batch")
    public R<Integer> deleteModels(@RequestBody List<Long> ids) {
        return R.success(aiModelService.deleteModels(ids));
    }

    @GetMapping("/{id}")
    public R<AiModel> getModelById(@PathVariable Long id) {
        return R.success(aiModelService.getModelById(id));
    }

    @GetMapping("/list")
    public R<PageInfo<AiModel>> listModels(@RequestParam(required = false) String modelName,
                                           @RequestParam(required = false) String model,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(aiModelService.listModels(modelName, model, status, pageNum, pageSize));
    }

    @GetMapping("/statistics")
    public R<Map<String, Object>> statisticsModels(@RequestParam(required = false) String modelName,
                                                   @RequestParam(required = false) String model,
                                                   @RequestParam(required = false) Integer status) {
        return R.success(aiModelService.statisticsModels(modelName, model, status));
    }
}
