package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ConsultAttachmentResponse;
import com.smart_plant.smart_plant.dto.FarmChatDetailResponse;
import com.smart_plant.smart_plant.dto.FarmChatSendRequest;
import com.smart_plant.smart_plant.dto.FarmChatSendResponse;
import com.smart_plant.smart_plant.dto.FarmChatSessionResponse;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.FarmChatService;
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

/** farm 端农场主与普通用户、技术人员的统一聊天接口。 */
@RestController
@RequestMapping({"/client/farm-chat", "/api/client/farm-chat"})
@RequiredArgsConstructor
public class ClientFarmChatController {
    private final FarmChatService farmChatService;
    private final ConsultAttachmentService attachmentService;

    /** 会话列表同时返回尚未发送消息的有效绑定联系人，方便直接发起沟通。 */
    @GetMapping("/sessions")
    public R<List<FarmChatSessionResponse>> sessions() {
        return R.success(farmChatService.listSessions());
    }

    /** sessionId 和 peerUserId 二选一；服务层会核验参与人和绑定关系。 */
    @GetMapping("/detail")
    public R<FarmChatDetailResponse> detail(@RequestParam(required = false) Long sessionId,
                                             @RequestParam(required = false) Long peerUserId,
                                             @RequestParam(required = false) Long beforeId,
                                             @RequestParam(required = false) Long afterId,
                                             @RequestParam(required = false) Integer pageSize) {
        return R.success(farmChatService.getDetail(sessionId, peerUserId, beforeId, afterId, pageSize));
    }

    @PostMapping("/send")
    public R<FarmChatSendResponse> send(@RequestBody FarmChatSendRequest request) {
        return R.success(farmChatService.send(request));
    }

    /** 附件沿用经过文件头、大小和上传人归属校验的现有聊天存储。 */
    @PostMapping("/attachments")
    public R<ConsultAttachmentResponse> upload(@RequestParam("file") MultipartFile file,
                                                @RequestParam("kind") String kind,
                                                @RequestParam(value = "displayName", required = false) String displayName) {
        farmChatService.requireCurrentChatRole();
        return R.success(attachmentService.upload(file, kind, displayName));
    }
}
