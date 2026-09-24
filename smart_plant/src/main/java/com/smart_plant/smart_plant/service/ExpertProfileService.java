package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ExpertAssetUploadResult;
import com.smart_plant.smart_plant.dto.ExpertCertificationRequest;
import com.smart_plant.smart_plant.entity.ExpertProfile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 专家信息业务接口，定义管理员端专家管理能力。
 */
public interface ExpertProfileService {

    /** 新增专家信息 */
    ExpertProfile addExpertProfile(ExpertProfile expertProfile);

    /** 编辑专家信息 */
    ExpertProfile updateExpertProfile(ExpertProfile expertProfile);

    /** 专家本人保存身份认证草稿；已通过资料修改后重新变为未提交。 */
    ExpertProfile saveExpertCertification(Long id, ExpertCertificationRequest request);

    /** 专家本人提交完整认证材料，进入后台待审核队列。 */
    ExpertProfile submitExpertCertification(Long id);

    /** 上传专家头像或证书图片，返回可跨端访问的 /uploads 相对路径 */
    ExpertAssetUploadResult uploadExpertAsset(MultipartFile file, String assetType);

    /** 删除专家信息 */
    void deleteExpertProfile(Long id);

    /** 批量删除专家信息 */
    int deleteExpertProfiles(List<Long> ids);

    /** 启用或禁用专家账号 */
    void updateStatus(Long id, Integer status);

    /** 修改专家咨询状态 */
    void updateConsultationStatus(Long id, Integer consultationStatus);

    /** 修改专家入驻审核状态 */
    void updateAuditStatus(Long id, Integer auditStatus);

    /** 根据ID查询专家信息 */
    ExpertProfile getExpertProfileById(Long id);

    /** 分页查询专家信息列表 */
    PageInfo<ExpertProfile> listExpertProfiles(String realName, String organization, String specialty, Integer auditStatus,
                                               Integer serviceStatus, Integer status,
                                               Integer pageNum, Integer pageSize);

    /** 查询专家管理统计数据 */
    Map<String, Object> statisticsExpertProfiles();

    /** 分页查询专家入驻审核列表 */
    PageInfo<ExpertProfile> listExpertAuditProfiles(String realName, String organization, String specialty, Integer auditStatus,
                                                    Integer status, Integer pageNum, Integer pageSize);
}
