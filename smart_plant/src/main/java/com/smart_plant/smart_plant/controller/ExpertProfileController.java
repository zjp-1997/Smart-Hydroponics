package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ExpertAssetUploadResult;
import com.smart_plant.smart_plant.entity.ExpertProfile;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.ExpertProfileService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 专家信息管理控制器，提供管理员端专家信息维护接口。
 */
@RestController
@RequestMapping("/expert-profile")
@RequiredArgsConstructor
@RequirePermission({"expert:manage", "expert:audit"})
public class ExpertProfileController {

    private final ExpertProfileService expertProfileService;

    /**
     * 新增专家信息。
     */
    @PostMapping("/add")
    public R<ExpertProfile> addExpertProfile(@RequestBody ExpertProfile expertProfile) {
        return R.success(expertProfileService.addExpertProfile(expertProfile));
    }

    /**
     * 编辑专家信息。
     */
    @PutMapping
    public R<ExpertProfile> updateExpertProfile(@RequestBody ExpertProfile expertProfile) {
        return R.success(expertProfileService.updateExpertProfile(expertProfile));
    }

    /**
     * 上传专家头像或证书图片。
     *
     * <p>管理端保存接口只存 URL 字符串，真实文件必须先上传到后端 /uploads 目录，farm 刷新后才能访问同一张图片。</p>
     */
    @PostMapping("/upload-asset")
    public R<ExpertAssetUploadResult> uploadExpertAsset(@RequestParam("file") MultipartFile file,
                                                        @RequestParam(defaultValue = "avatar") String assetType) {
        return R.success(expertProfileService.uploadExpertAsset(file, assetType));
    }

    /**
     * 删除专家信息。
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteExpertProfile(@PathVariable Long id) {
        expertProfileService.deleteExpertProfile(id);
        return R.success();
    }

    /**
     * 批量删除专家信息。
     */
    @DeleteMapping("/batch")
    public R<Integer> deleteExpertProfiles(@RequestBody List<Long> ids) {
        return R.success(expertProfileService.deleteExpertProfiles(ids));
    }

    /**
     * 启用或禁用专家账号，status=1为启用，status=0为禁用。
     */
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        expertProfileService.updateStatus(id, status);
        return R.success();
    }

    /**
     * 启用或禁用专家咨询能力，consultationStatus=1为可咨询，consultationStatus=0为不可咨询。
     */
    @PutMapping("/{id}/consultation-status")
    public R<Void> updateConsultationStatus(@PathVariable Long id,
                                            @RequestParam Integer consultationStatus) {
        expertProfileService.updateConsultationStatus(id, consultationStatus);
        return R.success();
    }

    /**
     * 根据ID查询专家详情。
     */
    @GetMapping("/{id}")
    public R<ExpertProfile> getExpertProfileById(@PathVariable Long id) {
        return R.success(expertProfileService.getExpertProfileById(id));
    }

    /**
     * 分页查询专家档案列表，仅返回入驻审核通过的专家。
     */
    @GetMapping("/list")
    public R<PageInfo<ExpertProfile>> listExpertProfiles(@RequestParam(required = false) String realName,
                                                         @RequestParam(required = false) String organization,
                                                         @RequestParam(required = false) String specialty,
                                                         @RequestParam(required = false) Integer auditStatus,
                                                         @RequestParam(required = false) Integer serviceStatus,
                                                         @RequestParam(required = false) Integer status,
                                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(expertProfileService.listExpertProfiles(
                realName,
                organization,
                specialty,
                auditStatus,
                serviceStatus,
                status,
                pageNum,
                pageSize
        ));
    }

    /**
     * 查询专家管理统计数据。
     */
    @GetMapping("/statistics")
    public R<Map<String, Object>> statisticsExpertProfiles() {
        return R.success(expertProfileService.statisticsExpertProfiles());
    }

    /**
     * 分页查询专家入驻审核列表。
     */
    @GetMapping("/audit/list")
    public R<PageInfo<ExpertProfile>> listExpertAuditProfiles(@RequestParam(required = false) String realName,
                                                              @RequestParam(required = false) String organization,
                                                              @RequestParam(required = false) String specialty,
                                                              @RequestParam(required = false) Integer auditStatus,
                                                              @RequestParam(required = false) Integer status,
                                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(expertProfileService.listExpertAuditProfiles(
                realName,
                organization,
                specialty,
                auditStatus,
                status,
                pageNum,
                pageSize
        ));
    }

    /**
     * 专家入驻审核，auditStatus=2为通过，auditStatus=3为不通过。
     */
    @PutMapping("/{id}/audit")
    public R<Void> updateAuditStatus(@PathVariable Long id, @RequestParam Integer auditStatus) {
        expertProfileService.updateAuditStatus(id, auditStatus);
        return R.success();
    }
}
