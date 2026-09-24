package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ExpertAssetUploadResult;
import com.smart_plant.smart_plant.dto.ExpertCertificationRequest;
import com.smart_plant.smart_plant.entity.ExpertCertificate;
import com.smart_plant.smart_plant.entity.ExpertDetail;
import com.smart_plant.smart_plant.entity.ExpertProfile;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.ExpertCertificateMapper;
import com.smart_plant.smart_plant.mapper.ExpertDetailMapper;
import com.smart_plant.smart_plant.mapper.ExpertProfileMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.ExpertProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 专家信息业务实现类，处理字段校验、唯一性校验和事务控制。
 */
@Service
@RequiredArgsConstructor
public class ExpertProfileServiceImpl implements ExpertProfileService {

    private static final long MAX_ASSET_UPLOAD_BYTES = 5L * 1024L * 1024L;
    private static final String UPLOAD_ROOT = "uploads";
    private static final String EXPERT_ASSET_UPLOAD_DIR = "expert-assets";
    private static final Set<String> ALLOWED_ASSET_TYPES = Set.of("avatar", "certificate");
    private static final Set<String> ALLOWED_ASSET_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final String EXPERT_ROLE_CODE = "expert";
    private static final int STATUS_ENABLED = 1;
    private static final int AUDIT_NOT_SUBMITTED = 0;
    private static final int AUDIT_PENDING = 1;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final ExpertProfileMapper expertProfileMapper;

    private final ExpertDetailMapper expertDetailMapper;

    private final ExpertCertificateMapper expertCertificateMapper;

    private final UserMapper userMapper;

    /** 新增专家信息；关联专家用户为可选字段。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpertProfile addExpertProfile(ExpertProfile expertProfile) {
        validateCreateExpertProfile(expertProfile);
        if (expertProfile.getUserId() != null) {
            checkExpertUserForBinding(expertProfile.getUserId());
        }
        checkUserIdUnique(expertProfile);
        normalizeDefaults(expertProfile);
        validateBoundUserCanEnableExpert(expertProfile);
        expertProfileMapper.insert(expertProfile);
        saveExpertDetailForCreate(expertProfile);
        saveExpertCertificateForCreate(expertProfile);
        return expertProfileMapper.selectById(expertProfile.getId());
    }

    /**
     * 编辑专家信息，按非空字段动态更新。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpertProfile updateExpertProfile(ExpertProfile expertProfile) {
        if (expertProfile == null || expertProfile.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家信息ID不能为空");
        }
        ExpertProfile oldExpertProfile = expertProfileMapper.selectById(expertProfile.getId());
        if (oldExpertProfile == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "专家信息不存在");
        }
        if (Integer.valueOf(2).equals(expertProfile.getAuditStatus())) {
            validateCertificationForApproval(oldExpertProfile, expertProfile);
        }
        Long effectiveUserId = expertProfile.getUserId() == null ? oldExpertProfile.getUserId() : expertProfile.getUserId();
        if (expertProfile.getUserId() != null) {
            checkExpertUserForBinding(expertProfile.getUserId());
        }
        checkUserIdUnique(expertProfile);
        syncAuditPassTime(expertProfile, oldExpertProfile);
        syncConsultationAndServiceStatus(expertProfile);
        validateEnumFields(expertProfile);
        validateNumericFields(expertProfile);
        validateBoundUserCanEnableExpert(effectiveUserId, expertProfile);
        if (hasProfileMainFieldUpdate(expertProfile)) {
            int rows = expertProfileMapper.updateById(expertProfile);
            if (rows == 0) {
                throw new BusinessException(ResponseCode.FAIL, "专家信息修改失败");
            }
        }
        saveExpertDetailForUpdate(expertProfile);
        saveExpertCertificateForUpdate(expertProfile);
        return expertProfileMapper.selectById(expertProfile.getId());
    }

    /**
     * 专家本人保存认证材料。审核中的快照不可修改；其他状态保存后回到“未提交”，
     * 防止已经通过的身份在无复核的情况下替换关键资质。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpertProfile saveExpertCertification(Long id, ExpertCertificationRequest request) {
        ExpertProfile current = getExpertProfileById(id);
        if (Integer.valueOf(AUDIT_PENDING).equals(current.getAuditStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "认证资料审核中，暂不可修改");
        }
        validateCertificationRequest(request);

        ExpertProfile update = new ExpertProfile();
        update.setId(id);
        update.setRealName(trimOptional(request.getRealName()));
        update.setOrganization(trimOptional(request.getOrganization()));
        update.setJobTitle(trimOptional(request.getJobTitle()));
        update.setPhone(trimOptional(request.getPhone()));
        update.setEmail(trimOptional(request.getEmail()));
        update.setSpecialty(trimOptional(request.getSpecialty()));
        update.setIntroduction(trimOptional(request.getIntroduction()));
        update.setCertificateName(trimOptional(request.getCertificateName()));
        update.setCertificateUrl(trimOptional(request.getCertificateUrl()));
        update.setCertificateAuditStatus(AUDIT_PENDING);
        update.setAuditStatus(AUDIT_NOT_SUBMITTED);
        update.setServiceStatus(0);
        update.setConsultationStatus(0);
        update.setStatus(0);
        update.setRemark("");
        return updateExpertProfile(update);
    }

    /** 仅完整材料可进入待审核状态，注册生成的空档案不会出现在审核队列。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpertProfile submitExpertCertification(Long id) {
        ExpertProfile current = getExpertProfileById(id);
        if (Integer.valueOf(AUDIT_PENDING).equals(current.getAuditStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "认证资料已提交，请耐心等待审核");
        }
        if (Integer.valueOf(2).equals(current.getAuditStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家身份已认证，无需重复提交");
        }
        validateCertificationForSubmit(current);

        ExpertProfile update = new ExpertProfile();
        update.setId(id);
        update.setAuditStatus(AUDIT_PENDING);
        update.setCertificateAuditStatus(AUDIT_PENDING);
        update.setServiceStatus(0);
        update.setConsultationStatus(0);
        update.setStatus(0);
        update.setRemark("");
        return updateExpertProfile(update);
    }

    /**
     * 上传专家资料图片，并返回数据库可保存的相对访问地址。
     *
     * <p>专家头像会被 farm 专家列表、消息列表和聊天详情复用，因此不能保存管理端 localStorage key。</p>
     */
    @Override
    public ExpertAssetUploadResult uploadExpertAsset(MultipartFile file, String assetType) {
        validateExpertAssetFile(file);
        String normalizedAssetType = normalizeAssetType(assetType);
        String contentType = file.getContentType() == null ? null : file.getContentType().trim().toLowerCase(Locale.ROOT);
        String extension = extensionFromContentType(contentType);
        if (extension == null) {
            extension = extensionFromName(file.getOriginalFilename());
        }
        if (extension == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家图片文件类型不支持");
        }

        LocalDate today = LocalDate.now();
        Path uploadDir = Paths.get(System.getProperty("user.dir"))
                .resolve(UPLOAD_ROOT)
                .resolve(EXPERT_ASSET_UPLOAD_DIR)
                .resolve(normalizedAssetType)
                .resolve(today.toString());
        String fileName = UUID.randomUUID() + "." + extension.replace(".", "").toLowerCase(Locale.ROOT);

        try {
            Files.createDirectories(uploadDir);
            Path targetPath = uploadDir.resolve(fileName).normalize();
            if (!targetPath.startsWith(uploadDir.normalize())) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "专家图片文件名不合法");
            }
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new BusinessException(ResponseCode.FAIL, "专家图片上传失败");
        }

        String url = "/" + UPLOAD_ROOT + "/" + EXPERT_ASSET_UPLOAD_DIR + "/" + normalizedAssetType + "/" + today + "/" + fileName;
        return new ExpertAssetUploadResult(url, file.getSize());
    }

    /**
     * 删除单条专家信息。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteExpertProfile(Long id) {
        requireId(id);
        int rows = expertProfileMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "专家信息不存在");
        }
    }

    /**
     * 批量删除专家信息。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteExpertProfiles(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的专家信息");
        }
        return expertProfileMapper.deleteBatchByIds(ids);
    }

    /**
     * 修改专家账号状态，status=1 表示启用，status=0 表示禁用。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireId(id);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家账号状态只能为0或1");
        }
        if (status == STATUS_ENABLED) {
            ensureBoundExpertUserEnabled(id);
        }
        int rows = expertProfileMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "专家信息不存在");
        }
    }

    /**
     * 修改专家咨询状态，consultationStatus=1 表示可咨询，consultationStatus=0 表示不可咨询。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConsultationStatus(Long id, Integer consultationStatus) {
        requireId(id);
        if (consultationStatus == null || (consultationStatus != 0 && consultationStatus != 1)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家咨询状态只能为0或1");
        }
        if (consultationStatus == STATUS_ENABLED) {
            ensureBoundExpertUserEnabled(id);
        }
        int rows = expertProfileMapper.updateConsultationStatus(id, consultationStatus);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "专家信息不存在");
        }
    }

    /**
     * 修改专家入驻审核状态。档案列表只展示审核通过数据，改为不通过后会自动从档案列表移除。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAuditStatus(Long id, Integer auditStatus) {
        requireId(id);
        if (auditStatus == null || (auditStatus != 2 && auditStatus != 3)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "入驻审核状态只能为2或3");
        }
        if (auditStatus == 2) {
            validateCertificationForSubmit(getExpertProfileById(id));
        }
        int rows = expertProfileMapper.updateAuditStatus(id, auditStatus);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "专家信息不存在");
        }
    }

    /**
     * 根据ID查询专家详情。
     */
    @Override
    public ExpertProfile getExpertProfileById(Long id) {
        requireId(id);
        ExpertProfile expertProfile = expertProfileMapper.selectById(id);
        if (expertProfile == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "专家信息不存在");
        }
        return expertProfile;
    }

    /**
     * 分页查询专家档案列表，固定只展示入驻审核通过的专家。
     */
    @Override
    public PageInfo<ExpertProfile> listExpertProfiles(String realName, String organization, String specialty,
                                                      Integer auditStatus,
                                                      Integer serviceStatus, Integer status,
                                                      Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(expertProfileMapper.selectList(realName, organization, specialty, 2, serviceStatus, status));
    }

    /**
     * 查询专家管理统计数据，供管理端统计卡片直接展示。
     */
    @Override
    public Map<String, Object> statisticsExpertProfiles() {
        return expertProfileMapper.selectStatistics();
    }

    /**
     * 分页查询专家入驻审核列表，可按审核状态筛选待审核、通过或不通过记录。
     */
    @Override
    public PageInfo<ExpertProfile> listExpertAuditProfiles(String realName, String organization, String specialty,
                                                           Integer auditStatus,
                                                           Integer status, Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(expertProfileMapper.selectAuditList(realName, organization, specialty, auditStatus, status));
    }

    /** 新增时的必填字段校验。 */
    private void validateCreateExpertProfile(ExpertProfile expertProfile) {
        if (expertProfile == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家信息不能为空");
        }
        if (!StringUtils.hasText(expertProfile.getRealName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家真实姓名不能为空");
        }
        if (!StringUtils.hasText(expertProfile.getOrganization())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "所属机构不能为空");
        }
        if (!StringUtils.hasText(expertProfile.getJobTitle())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "职称不能为空");
        }
        if (!StringUtils.hasText(expertProfile.getCertificateUrl())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "资质证书不能为空");
        }
        if (expertProfile.getRating() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家评分不能为空");
        }
        if (expertProfile.getStatus() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家账号状态不能为空");
        }
        if (expertProfile.getServiceStatus() == null && expertProfile.getConsultationStatus() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家咨询状态不能为空");
        }
        if (expertProfile.getAuditStatus() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家审核状态不能为空");
        }
        syncConsultationAndServiceStatus(expertProfile);
        validateEnumFields(expertProfile);
        validateNumericFields(expertProfile);
    }

    /** 保存草稿时校验格式和数据库字段长度，完整性在主动提交时统一校验。 */
    private void validateCertificationRequest(ExpertCertificationRequest request) {
        if (request == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "认证资料不能为空");
        }
        requireMaxLength(request.getRealName(), 50, "真实姓名");
        requireMaxLength(request.getOrganization(), 100, "所属机构");
        requireMaxLength(request.getJobTitle(), 50, "职称");
        requireMaxLength(request.getPhone(), 20, "手机号");
        requireMaxLength(request.getEmail(), 100, "邮箱");
        requireMaxLength(request.getSpecialty(), 255, "擅长方向");
        requireMaxLength(request.getIntroduction(), 1000, "专家简介");
        requireMaxLength(request.getCertificateName(), 100, "证书名称");
        requireMaxLength(request.getCertificateUrl(), 255, "证书地址");

        String phone = trimOptional(request.getPhone());
        if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请输入正确的11位手机号");
        }
        String email = trimOptional(request.getEmail());
        if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "邮箱格式不正确");
        }
        String certificateUrl = trimOptional(request.getCertificateUrl());
        if (!certificateUrl.isEmpty() && !certificateUrl.startsWith("/uploads/expert-assets/certificate/")) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请重新上传资质证书");
        }
    }

    /** 与 smart_farm 后台新增专家的核心必填材料保持一致。 */
    private void validateCertificationForSubmit(ExpertProfile profile) {
        if (!StringUtils.hasText(profile.getRealName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请填写真实姓名");
        }
        if (!StringUtils.hasText(profile.getOrganization())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请填写所属机构");
        }
        if (!StringUtils.hasText(profile.getJobTitle())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请填写职称");
        }
        if (!StringUtils.hasText(profile.getCertificateUrl())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请上传资质证书");
        }
    }

    /** 管理员编辑并审核可在同一次请求中完成，因此按“本次值优先、原值兜底”检查。 */
    private void validateCertificationForApproval(ExpertProfile current, ExpertProfile update) {
        ExpertProfile effective = new ExpertProfile();
        effective.setRealName(update.getRealName() == null ? current.getRealName() : update.getRealName());
        effective.setOrganization(update.getOrganization() == null ? current.getOrganization() : update.getOrganization());
        effective.setJobTitle(update.getJobTitle() == null ? current.getJobTitle() : update.getJobTitle());
        effective.setCertificateUrl(update.getCertificateUrl() == null ? current.getCertificateUrl() : update.getCertificateUrl());
        validateCertificationForSubmit(effective);
    }

    private void requireMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.trim().length() > maxLength) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, fieldName + "不能超过" + maxLength + "个字符");
        }
    }

    private String trimOptional(String value) {
        return value == null ? "" : value.trim();
    }

    /** 校验专家资料图片上传参数，避免超大文件和非图片文件进入静态资源目录。 */
    private void validateExpertAssetFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择需要上传的专家图片");
        }
        if (file.getSize() > MAX_ASSET_UPLOAD_BYTES) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家图片大小不能超过5MB");
        }
        String contentType = file.getContentType() == null ? null : file.getContentType().trim().toLowerCase(Locale.ROOT);
        if (contentType == null || !ALLOWED_ASSET_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "仅支持JPG、PNG或WEBP专家图片");
        }
    }

    /** 资源类型只允许头像和证书，防止前端传入任意目录名影响文件落盘位置。 */
    private String normalizeAssetType(String assetType) {
        String normalized = StringUtils.hasText(assetType) ? assetType.trim().toLowerCase(Locale.ROOT) : "avatar";
        if (!ALLOWED_ASSET_TYPES.contains(normalized)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家图片类型不合法");
        }
        return normalized;
    }

    /** 根据 MIME 类型推导文件扩展名，统一 jpeg 存为 jpg。 */
    private String extensionFromContentType(String contentType) {
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            return null;
        }
        String extension = contentType.substring("image/".length()).toLowerCase(Locale.ROOT);
        return "jpeg".equals(extension) ? "jpg" : extension;
    }

    /** MIME 类型缺失时兜底读取原始文件名后缀。 */
    private String extensionFromName(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        int slashIndex = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        int dotIndex = value.lastIndexOf('.');
        if (dotIndex <= slashIndex || dotIndex == value.length() - 1) {
            return null;
        }
        return value.substring(dotIndex + 1);
    }

    /** 数据库已有默认值，这里仅对显式为空的常用字段补齐默认值。 */
    private void normalizeDefaults(ExpertProfile expertProfile) {
        if (expertProfile.getRating() == null) {
            expertProfile.setRating(new BigDecimal("5.00"));
        }
        if (expertProfile.getConsultationCount() == null) {
            expertProfile.setConsultationCount(0);
        }
        if (expertProfile.getReviewCount() == null) {
            expertProfile.setReviewCount(0);
        }
        if (expertProfile.getAuditStatus() == null) {
            expertProfile.setAuditStatus(1);
        }
        if (expertProfile.getCertificateAuditStatus() == null) {
            expertProfile.setCertificateAuditStatus(1);
        }
        if (Integer.valueOf(2).equals(expertProfile.getAuditStatus()) && expertProfile.getAuditPassTime() == null) {
            expertProfile.setAuditPassTime(LocalDateTime.now());
        }
        if (expertProfile.getServiceStatus() == null) {
            expertProfile.setServiceStatus(1);
        }
        if (expertProfile.getConsultationStatus() == null) {
            expertProfile.setConsultationStatus(1);
        }
        if (expertProfile.getStatus() == null) {
            expertProfile.setStatus(1);
        }
    }

    /** 新增专家时保存详情表字段。 */
    private void saveExpertDetailForCreate(ExpertProfile expertProfile) {
        if (!hasDetailFieldUpdate(expertProfile)) {
            return;
        }
        expertDetailMapper.upsertByExpertId(buildExpertDetail(expertProfile));
    }

    /** 编辑专家时保存详情表字段，未传入的字段保持原值。 */
    private void saveExpertDetailForUpdate(ExpertProfile expertProfile) {
        if (!hasDetailFieldUpdate(expertProfile)) {
            return;
        }
        ExpertDetail oldDetail = expertDetailMapper.selectByExpertId(expertProfile.getId());
        ExpertDetail detail = new ExpertDetail();
        detail.setExpertId(expertProfile.getId());
        detail.setSpecialty(expertProfile.getSpecialty() != null
                ? expertProfile.getSpecialty()
                : oldDetail == null ? null : oldDetail.getSpecialty());
        detail.setIntroduction(expertProfile.getIntroduction() != null
                ? expertProfile.getIntroduction()
                : oldDetail == null ? null : oldDetail.getIntroduction());
        expertDetailMapper.upsertByExpertId(detail);
    }

    /** 新增专家时保存证书表字段。 */
    private void saveExpertCertificateForCreate(ExpertProfile expertProfile) {
        if (!StringUtils.hasText(expertProfile.getCertificateUrl())) {
            return;
        }
        expertCertificateMapper.insert(buildExpertCertificate(expertProfile));
    }

    /** 编辑专家时保存证书字段，certificateUrl 传空字符串表示清空原证书。 */
    private void saveExpertCertificateForUpdate(ExpertProfile expertProfile) {
        if (!hasCertificateFieldUpdate(expertProfile)) {
            return;
        }
        ExpertCertificate oldCertificate = expertCertificateMapper.selectFirstByExpertId(expertProfile.getId());
        String certificateUrl = expertProfile.getCertificateUrl() != null
                ? expertProfile.getCertificateUrl()
                : oldCertificate == null ? null : oldCertificate.getCertificateUrl();
        expertCertificateMapper.deleteByExpertId(expertProfile.getId());
        if (StringUtils.hasText(certificateUrl)) {
            expertCertificateMapper.insert(buildExpertCertificate(expertProfile, oldCertificate, certificateUrl));
        }
    }

    /** 根据专家聚合对象构造详情实体。 */
    private ExpertDetail buildExpertDetail(ExpertProfile expertProfile) {
        ExpertDetail detail = new ExpertDetail();
        detail.setExpertId(expertProfile.getId());
        detail.setSpecialty(expertProfile.getSpecialty());
        detail.setIntroduction(expertProfile.getIntroduction());
        return detail;
    }

    /** 根据专家聚合对象构造证书实体。 */
    private ExpertCertificate buildExpertCertificate(ExpertProfile expertProfile) {
        return buildExpertCertificate(expertProfile, null, expertProfile.getCertificateUrl());
    }

    /** 根据专家聚合对象构造证书实体，编辑时可使用旧证书补齐未传字段。 */
    private ExpertCertificate buildExpertCertificate(ExpertProfile expertProfile, ExpertCertificate oldCertificate,
                                                     String certificateUrl) {
        ExpertCertificate certificate = new ExpertCertificate();
        certificate.setExpertId(expertProfile.getId());
        certificate.setCertificateName(StringUtils.hasText(expertProfile.getCertificateName())
                ? expertProfile.getCertificateName().trim()
                : oldCertificate == null ? null : oldCertificate.getCertificateName());
        certificate.setCertificateUrl(certificateUrl);
        certificate.setAuditStatus(expertProfile.getCertificateAuditStatus() != null
                ? expertProfile.getCertificateAuditStatus()
                : oldCertificate == null || oldCertificate.getAuditStatus() == null ? 1 : oldCertificate.getAuditStatus());
        certificate.setIsExpired(oldCertificate == null || oldCertificate.getIsExpired() == null
                ? 0
                : oldCertificate.getIsExpired());
        return certificate;
    }

    /** 判断本次编辑是否包含 expert_profile 主表字段。 */
    private boolean hasProfileMainFieldUpdate(ExpertProfile expertProfile) {
        return expertProfile.getUserId() != null
                || expertProfile.getInstitutionId() != null
                || expertProfile.getOrganization() != null
                || StringUtils.hasText(expertProfile.getUsername())
                || StringUtils.hasText(expertProfile.getPasswordHash())
                || StringUtils.hasText(expertProfile.getRealName())
                || expertProfile.getJobTitle() != null
                || expertProfile.getPhone() != null
                || expertProfile.getEmail() != null
                || expertProfile.getAvatar() != null
                || expertProfile.getRating() != null
                || expertProfile.getReviewCount() != null
                || expertProfile.getConsultationCount() != null
                || expertProfile.getAuditStatus() != null
                || expertProfile.getAuditPassTime() != null
                || expertProfile.getServiceStatus() != null
                || expertProfile.getConsultationStatus() != null
                || expertProfile.getStatus() != null
                || expertProfile.getRemark() != null;
    }

    /** 判断本次编辑是否包含 expert_detail 字段。 */
    private boolean hasDetailFieldUpdate(ExpertProfile expertProfile) {
        return expertProfile.getSpecialty() != null || expertProfile.getIntroduction() != null;
    }

    /** 判断本次编辑是否包含 expert_certificate 字段。 */
    private boolean hasCertificateFieldUpdate(ExpertProfile expertProfile) {
        return expertProfile.getCertificateUrl() != null
                || expertProfile.getCertificateName() != null
                || expertProfile.getCertificateAuditStatus() != null;
    }

    /** 枚举字段取值校验，避免写入数据库 CHECK 约束不允许的值。 */
    private void validateEnumFields(ExpertProfile expertProfile) {
        Integer auditStatus = expertProfile.getAuditStatus();
        if (auditStatus != null && auditStatus != 0 && auditStatus != 1 && auditStatus != 2 && auditStatus != 3) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "审核状态只能为0、1、2或3");
        }
        Integer serviceStatus = expertProfile.getServiceStatus();
        if (serviceStatus != null && serviceStatus != 0 && serviceStatus != 1) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "服务状态只能为0或1");
        }
        Integer consultationStatus = expertProfile.getConsultationStatus();
        if (consultationStatus != null && consultationStatus != 0 && consultationStatus != 1) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "咨询状态只能为0或1");
        }
        Integer status = expertProfile.getStatus();
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家账号状态只能为0或1");
        }
        Integer certificateAuditStatus = expertProfile.getCertificateAuditStatus();
        if (certificateAuditStatus != null
                && certificateAuditStatus != 1
                && certificateAuditStatus != 2
                && certificateAuditStatus != 3) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "证书审核状态只能为1、2或3");
        }
    }

    /** 审核状态变更时维护审核通过时间，普通资料编辑不覆盖历史通过时间。 */
    private void syncAuditPassTime(ExpertProfile expertProfile, ExpertProfile oldExpertProfile) {
        if (expertProfile.getAuditStatus() == null) {
            return;
        }
        if (expertProfile.getAuditStatus() == 2) {
            expertProfile.setAuditPassTime(oldExpertProfile.getAuditStatus() != null
                    && oldExpertProfile.getAuditStatus() == 2
                    ? oldExpertProfile.getAuditPassTime()
                    : LocalDateTime.now());
            expertProfile.setStatus(1);
            expertProfile.setServiceStatus(1);
            expertProfile.setConsultationStatus(1);
            return;
        }
        expertProfile.setAuditPassTime(null);
        expertProfile.setStatus(0);
        expertProfile.setServiceStatus(0);
        expertProfile.setConsultationStatus(0);
    }

    /**
     * 专家咨询能力在历史表结构中有 service_status 和 consultation_status 两列。
     * 对外统一按“咨询状态”处理，写入时保持两列一致。
     */
    private void syncConsultationAndServiceStatus(ExpertProfile expertProfile) {
        if (expertProfile.getConsultationStatus() != null) {
            expertProfile.setServiceStatus(expertProfile.getConsultationStatus());
            return;
        }
        if (expertProfile.getServiceStatus() != null) {
            expertProfile.setConsultationStatus(expertProfile.getServiceStatus());
        }
    }

    /** 数值字段范围校验，评分范围为0到5，咨询次数不能为负数。 */
    private void validateNumericFields(ExpertProfile expertProfile) {
        BigDecimal rating = expertProfile.getRating();
        if (rating != null && (rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(new BigDecimal("5.00")) > 0)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家评分必须在0到5之间");
        }
        Integer consultationCount = expertProfile.getConsultationCount();
        if (consultationCount != null && consultationCount < 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "咨询次数不能小于0");
        }
    }

    /** 校验关联用户必须是专家角色，避免用户管理和专家档案产生身份错配。 */
    private User checkExpertUserForBinding(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择关联的专家用户");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "关联用户不存在");
        }
        if (!EXPERT_ROLE_CODE.equalsIgnoreCase(user.getRoleCode())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "关联用户必须是专家角色");
        }
        return user;
    }

    /** 专家档案要进入启用或可咨询状态时，绑定用户必须仍是启用的专家账号。 */
    private void validateBoundUserCanEnableExpert(ExpertProfile expertProfile) {
        validateBoundUserCanEnableExpert(expertProfile.getUserId(), expertProfile);
    }

    private void validateBoundUserCanEnableExpert(Long userId, ExpertProfile expertProfile) {
        if (userId == null) {
            return;
        }
        boolean enablingProfile = Integer.valueOf(STATUS_ENABLED).equals(expertProfile.getStatus())
                || Integer.valueOf(STATUS_ENABLED).equals(expertProfile.getServiceStatus())
                || Integer.valueOf(STATUS_ENABLED).equals(expertProfile.getConsultationStatus())
                || Integer.valueOf(2).equals(expertProfile.getAuditStatus());
        if (!enablingProfile) {
            return;
        }
        User user = checkExpertUserForBinding(userId);
        if (!Integer.valueOf(STATUS_ENABLED).equals(user.getStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该账号已禁用，请启用后再试");
        }
    }

    private void ensureBoundExpertUserEnabled(Long expertId) {
        ExpertProfile expertProfile = expertProfileMapper.selectById(expertId);
        if (expertProfile == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "专家信息不存在");
        }
        if (expertProfile.getUserId() == null) {
            return;
        }
        User user = checkExpertUserForBinding(expertProfile.getUserId());
        if (!Integer.valueOf(STATUS_ENABLED).equals(user.getStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该账号已禁用，请启用后再试");
        }
    }

    /** 校验一个用户只能绑定一条专家资料。 */
    private void checkUserIdUnique(ExpertProfile expertProfile) {
        if (expertProfile.getUserId() == null) {
            return;
        }
        int count = expertProfileMapper.countByUserId(expertProfile.getUserId(), expertProfile.getId());
        if (count > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该用户已绑定专家信息");
        }
    }

    /** 公共ID非空校验。 */
    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "专家信息ID不能为空");
        }
    }
}
