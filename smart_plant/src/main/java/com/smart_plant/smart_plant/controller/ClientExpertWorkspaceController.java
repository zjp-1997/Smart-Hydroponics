package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientExpertChatResponse;
import com.smart_plant.smart_plant.dto.ConsultAttachmentResponse;
import com.smart_plant.smart_plant.dto.ExpertChatDetailResponse;
import com.smart_plant.smart_plant.dto.ExpertCertificationRequest;
import com.smart_plant.smart_plant.dto.ExpertAssetUploadResult;
import com.smart_plant.smart_plant.dto.ExpertReplyRequest;
import com.smart_plant.smart_plant.entity.ConsultSession;
import com.smart_plant.smart_plant.entity.ExpertProfile;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.ConsultationService;
import com.smart_plant.smart_plant.service.ExpertProfileService;
import com.smart_plant.smart_plant.service.impl.ConsultAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** farm 专家工作台接口；服务层按登录账号绑定的专家档案逐次校验归属。 */
@RestController
@RequestMapping({"/client/expert-workspace", "/api/client/expert-workspace"})
@RequiredArgsConstructor
public class ClientExpertWorkspaceController {

    private final ConsultationService consultationService;
    private final ConsultAttachmentService attachmentService;
    private final ExpertProfileService expertProfileService;

    /** 专家上传聊天附件；具体会话归属在回复接口中再次校验。 */
    @PostMapping("/attachments")
    public R<ConsultAttachmentResponse> uploadAttachment(@RequestParam("file") MultipartFile file,
                                                          @RequestParam("kind") String kind,
                                                          @RequestParam(value = "displayName", required = false) String displayName) {
        // 停用的专家档案不得提前上传附件占用存储；发送时仍逐次校验会话归属。
        if (!Integer.valueOf(1).equals(consultationService.getCurrentExpertProfile().getStatus())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "专家档案尚未启用");
        }
        return R.success(attachmentService.upload(file, kind, displayName));
    }

    /** 身份认证页读取本人专家档案和审核状态。 */
    @GetMapping("/profile")
    public R<ExpertProfile> profile() {
        return R.success(consultationService.getCurrentExpertProfile());
    }

    /** 保存本人认证资料；审核中的材料不可修改。 */
    @PutMapping("/profile")
    public R<ExpertProfile> saveProfile(@RequestBody ExpertCertificationRequest request) {
        ExpertProfile profile = consultationService.getCurrentExpertProfile();
        return R.success(expertProfileService.saveExpertCertification(profile.getId(), request));
    }

    /** 补齐认证资料后主动提交审核。 */
    @PostMapping("/profile/submit")
    public R<ExpertProfile> submitProfile() {
        ExpertProfile profile = consultationService.getCurrentExpertProfile();
        return R.success(expertProfileService.submitExpertCertification(profile.getId()));
    }

    /** 上传本人身份认证证书，复用专家图片的文件安全校验和存储目录。 */
    @PostMapping("/profile/certificate")
    public R<ExpertAssetUploadResult> uploadCertificate(@RequestParam("file") MultipartFile file) {
        consultationService.getCurrentExpertProfile();
        return R.success(expertProfileService.uploadExpertAsset(file, "certificate"));
    }

    /** 消息首页只返回分配给当前专家的咨询会话。 */
    @GetMapping("/sessions")
    public R<List<ConsultSession>> sessions() {
        return R.success(consultationService.listExpertSessions());
    }

    /** 打开会话后返回历史消息，并标记当前专家收到的消息已读。 */
    @GetMapping("/sessions/{id}")
    public R<ExpertChatDetailResponse> detail(@PathVariable Long id,
                                               @RequestParam(required = false) Long beforeId,
                                               @RequestParam(required = false) Long afterId,
                                               @RequestParam(required = false) Integer pageSize) {
        return R.success(consultationService.getExpertChatDetail(id, beforeId, afterId, pageSize));
    }

    /** 仅允许专家对本人会话发送文字、图片或文件回复。 */
    @PostMapping("/sessions/{id}/reply")
    public R<ClientExpertChatResponse> reply(@PathVariable Long id, @RequestBody ExpertReplyRequest request) {
        return R.success(consultationService.replyAsExpert(id, request == null ? null : request.content(),
                request == null ? null : request.messageType(), request == null ? null : request.mediaUrl()));
    }
}
