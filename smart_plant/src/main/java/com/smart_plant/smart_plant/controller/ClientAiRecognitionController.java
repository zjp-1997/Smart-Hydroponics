package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientAiRecognitionTypeResponse;
import com.smart_plant.smart_plant.dto.ClientAiRecognitionRecordResponse;
import com.smart_plant.smart_plant.dto.ClientManualRecognitionResponse;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.ClientAiRecognitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * farm 用户端智能识别控制器。
 *
 * <p>接口统一挂在 /client 和 /api/client 下，保持与 farm 用户端其他接口一致。</p>
 */
@RestController
@RequestMapping({"/client", "/api/client"})
@RequiredArgsConstructor
public class ClientAiRecognitionController {

    /** 用户端智能识别服务，负责识别类型查询、图片保存、识别记录和结果落库。 */
    private final ClientAiRecognitionService clientAiRecognitionService;

    @GetMapping("/recognition-types/list")
    public R<List<ClientAiRecognitionTypeResponse>> listEnabledRecognitionTypes() {
        // 只返回已启用识别类型，避免用户端选择到后台停用的模型分类。
        return R.success(clientAiRecognitionService.listEnabledRecognitionTypes());
    }

    @GetMapping("/recognitions/records")
    public R<List<ClientAiRecognitionRecordResponse>> listManualRecognitionRecords(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                                  @RequestParam(defaultValue = "50") Integer pageSize) {
        // 识别记录列表只返回当前登录用户自己的手工识别记录，用户身份由 token 决定。
        return R.success(clientAiRecognitionService.listManualRecognitionRecords(pageNum, pageSize));
    }

    @PostMapping("/recognitions/manual")
    public R<ClientManualRecognitionResponse> recognizeManually(@RequestParam("recognitionType") Long recognitionType,
                                                               @RequestParam("image") MultipartFile image) {
        // recognitionType 和 image 与 farm 端上传字段保持一致，便于移动端直接构建 multipart 请求。
        return R.success(clientAiRecognitionService.recognizeManually(recognitionType, image));
    }
}
