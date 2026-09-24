package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.entity.DiseasePest;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.CropImageService;
import com.smart_plant.smart_plant.service.DiseasePestService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** farm 用户端病虫害信息上报接口。 */
@RestController
@RequestMapping({"/client", "/api/client"})
@RequiredArgsConstructor
public class ClientDiseasePestController {

    /** 复用统一图片服务校验并保存病虫害封面图。 */
    private final CropImageService cropImageService;

    /** 复用病虫害领域服务完成字段校验、重复校验和数据入库。 */
    private final DiseasePestService diseasePestService;

    /** 知识库写入仅允许农场主，普通用户可以查阅和使用识别。 */
    private final DataPermissionService dataPermissionService;

    /**
     * 查询 farm 用户端病虫害详情。
     * 详情由领域服务一次性组装基础信息与启用的防治措施，避免前端发起多次请求。
     */
    @GetMapping("/disease-pests/{id}")
    public R<DiseasePest> getDiseasePestDetail(@PathVariable Long id) {
        return R.success(diseasePestService.getEnabledDiseasePestById(id));
    }

    /**
     * 接收 farm 用户上传的图片和病虫害基础信息。
     * 危害等级不在此处接收，它应由具体识别或现场事件动态产生。
     */
    @PostMapping(value = "/disease-pests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<DiseasePest> addDiseasePest(@RequestParam("image") MultipartFile image,
                                         @RequestParam String name,
                                         @RequestParam Long cropTypeId,
                                         @RequestParam Integer type,
                                         @RequestParam String suitableStage,
                                         @RequestParam String symptom,
                                         @RequestParam Integer status) {
        dataPermissionService.requireFarmManager(dataPermissionService.currentUser().getId());
        CropImageUploadResult upload = cropImageService.uploadCropImage(image);
        DiseasePest diseasePest = new DiseasePest();
        diseasePest.setCoverImage(upload.imageUrl());
        diseasePest.setName(name);
        diseasePest.setCropTypeId(cropTypeId);
        diseasePest.setType(type);
        diseasePest.setSuitableStage(suitableStage);
        diseasePest.setSymptom(symptom);
        diseasePest.setStatus(status);
        return R.success(diseasePestService.addDiseasePest(diseasePest));
    }
}
