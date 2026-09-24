package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientExpertChatDetailResponse;
import com.smart_plant.smart_plant.dto.ClientExpertChatRequest;
import com.smart_plant.smart_plant.dto.ClientExpertChatResponse;
import com.smart_plant.smart_plant.dto.ClientExpertChatSessionResponse;
import com.smart_plant.smart_plant.dto.ConsultAttachmentResponse;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.ConsultationService;
import com.smart_plant.smart_plant.service.impl.ConsultAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * farm 用户端专家聊天控制器。
 *
 * <p>用户端只提交消息，后端负责选择或校验专家、维护会话并保存聊天记录。</p>
 */
@RestController
@RequestMapping({"/client", "/api/client"})
@RequiredArgsConstructor
public class ClientExpertChatController {

    /** 专家咨询服务，复用会话创建和消息保存逻辑。 */
    private final ConsultationService consultationService;
    private final ConsultAttachmentService attachmentService;

    /** 上传图片或文件后返回服务端地址；发送消息时仍会检查附件归属。 */
    @PostMapping("/expert-chat/attachments")
    public R<ConsultAttachmentResponse> uploadAttachment(@RequestParam("file") MultipartFile file,
                                                          @RequestParam("kind") String kind,
                                                          @RequestParam(value = "displayName", required = false) String displayName) {
        return R.success(attachmentService.upload(file, kind, displayName));
    }

    @PostMapping("/expert-chat/send")
    public R<ClientExpertChatResponse> sendExpertMessage(@RequestBody ClientExpertChatRequest request) {
        return R.success(consultationService.sendClientMessage(request));
    }

    @GetMapping("/expert-chat/sessions")
    public R<List<ClientExpertChatSessionResponse>> listExpertChatSessions() {
        // 消息页只查询当前登录用户自己的专家会话列表，避免用户间消息串读。
        return R.success(consultationService.listClientSessions());
    }

    @GetMapping("/expert-chat/detail")
    public R<ClientExpertChatDetailResponse> getExpertChatDetail(@RequestParam(required = false) Long sessionId,
                                                                 @RequestParam(required = false) Long expertId) {
        // 聊天页通过 sessionId 或 expertId 统一回放历史消息，保证不同入口进入同一专家时消息同步。
        return R.success(consultationService.getClientChatDetail(sessionId, expertId));
    }
}
