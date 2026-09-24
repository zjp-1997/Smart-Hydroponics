package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientAiChatRequest;
import com.smart_plant.smart_plant.dto.ClientAiChatResponse;
import com.smart_plant.smart_plant.dto.ClientAiModelResponse;
import com.smart_plant.smart_plant.dto.ConsultAttachmentResponse;
import com.smart_plant.smart_plant.entity.AiModel;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.AiChatService;
import com.smart_plant.smart_plant.service.AiModelService;
import com.smart_plant.smart_plant.service.impl.ConsultAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * farm 用户端 AI 咨询控制器。
 *
 * <p>用户端只能获取已启用模型的非敏感字段，并通过后端代理向模型发送消息。</p>
 */
@RestController
@RequestMapping({"/client", "/api/client"})
@RequiredArgsConstructor
public class ClientAiChatController {

    /** 模型配置服务，用于查询启用模型选项。 */
    private final AiModelService aiModelService;

    /** AI对话服务，用于发送咨询消息并保存对话记录。 */
    private final AiChatService aiChatService;

    /** 图片与文件上传复用专家聊天的类型校验、大小限制和用户目录隔离。 */
    private final ConsultAttachmentService attachmentService;

    @PostMapping("/ai-chat/attachments")
    public R<ConsultAttachmentResponse> uploadAttachment(@RequestParam("file") MultipartFile file,
                                                          @RequestParam("kind") String kind,
                                                          @RequestParam(value = "displayName", required = false) String displayName) {
        // 上传只保存附件；发送消息时由 AI 服务再次校验文件是否属于当前用户。
        return R.success(attachmentService.upload(file, kind, displayName));
    }

    @GetMapping("/models/list")
    public R<List<ClientAiModelResponse>> listEnabledModels() {
        // 仅返回启用模型，且不包含 API Key，避免敏感配置泄露到用户端。
        List<ClientAiModelResponse> models = aiModelService.listModels(null, null, 1, 1, 1000)
                .getList()
                .stream()
                // AI 咨询只展示 DeepSeek，避免前端误选其他供应商模型。
                .filter(model -> model.getModel() != null && model.getModel().toLowerCase().startsWith("deepseek-"))
                .map(this::toClientModelResponse)
                .toList();
        return R.success(models);
    }

    @PostMapping("/ai-chat/send")
    public R<ClientAiChatResponse> sendMessage(@RequestBody ClientAiChatRequest request) {
        // 后端统一调用模型并落库，farm 端无需保存 API Key，也不直接访问模型供应商。
        return R.success(aiChatService.sendClientMessage(request));
    }

    @GetMapping("/ai-chat/history")
    public R<List<ClientAiChatResponse>> listHistory(@RequestParam(value = "beforeId", required = false) Long beforeId,
                                                      @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        // 从服务端当前登录身份确定用户ID，客户端不能传 userId 查询他人的历史对话。
        return R.success(aiChatService.listClientHistory(beforeId, pageSize));
    }

    private ClientAiModelResponse toClientModelResponse(AiModel model) {
        return new ClientAiModelResponse(model.getId(), model.getModelName(), model.getModel());
    }
}
