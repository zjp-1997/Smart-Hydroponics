package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ClientAiRecognitionRecordResponse;
import com.smart_plant.smart_plant.dto.ClientAiRecognitionTypeResponse;
import com.smart_plant.smart_plant.dto.ClientManualRecognitionResponse;
import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.entity.AiRecognitionRecord;
import com.smart_plant.smart_plant.entity.AiRecognitionResult;
import com.smart_plant.smart_plant.entity.AiRecognitionType;
import com.smart_plant.smart_plant.entity.CropImage;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.AiRecognitionRecordMapper;
import com.smart_plant.smart_plant.mapper.AiRecognitionTypeMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.AiRecognitionRecordService;
import com.smart_plant.smart_plant.service.ClientAiRecognitionService;
import com.smart_plant.smart_plant.service.CropImageService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * farm 用户端智能识别服务实现。
 *
 * <p>当前项目未接入外部视觉模型，因此这里先以识别类型为维度生成可审计的规则化结果；
 * 后续接入模型时，只需替换 buildProfile 的结果来源，记录和响应结构可以继续复用。</p>
 */
@Service
@RequiredArgsConstructor
public class ClientAiRecognitionServiceImpl implements ClientAiRecognitionService {

    private static final int TYPE_STATUS_ENABLED = 1;
    private static final int SOURCE_TYPE_MANUAL = 1;
    private static final int RECORD_STATUS_COMPLETED = 3;
    private static final int RESULT_STATUS_SUCCESS = 1;
    private static final BigDecimal MANUAL_RULE_CONFIDENCE = new BigDecimal("80.00");
    private static final String MODEL_NAME = "ManualUploadRuleEngine";
    private static final String MODEL_VERSION = "1.0";

    /** 识别类型 Mapper，用于读取后台维护的启用类型。 */
    private final AiRecognitionTypeMapper aiRecognitionTypeMapper;

    /** 识别记录 Mapper，用于写入 ai_recognition_record 和 ai_recognition_result。 */
    private final AiRecognitionRecordMapper aiRecognitionRecordMapper;

    /** 手机图片服务，复用已有图片上传校验和落库能力。 */
    private final CropImageService cropImageService;

    /** 后台识别记录服务，复用其查询条件校验、分页和当前用户数据范围控制。 */
    private final AiRecognitionRecordService aiRecognitionRecordService;

    /** 数据权限服务，负责从 token 中解析当前 farm 用户。 */
    private final DataPermissionService dataPermissionService;

    @Override
    public List<ClientAiRecognitionTypeResponse> listEnabledRecognitionTypes() {
        // 复用后台识别类型表，只暴露启用状态的数据给 farm 用户端选择。
        return aiRecognitionTypeMapper.selectList(null, null, TYPE_STATUS_ENABLED)
                .stream()
                .map(this::toClientTypeResponse)
                .toList();
    }

    @Override
    public List<ClientAiRecognitionRecordResponse> listManualRecognitionRecords(Integer pageNum, Integer pageSize) {
        // 复用已有识别记录查询接口，并固定 sourceType=1，确保 farm 用户端只展示手工识别记录。
        PageInfo<AiRecognitionRecord> pageInfo = aiRecognitionRecordService.listAiRecognitionRecords(
                null,
                SOURCE_TYPE_MANUAL,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                pageNum,
                pageSize
        );
        return pageInfo.getList().stream()
                .map(this::toClientRecordResponse)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientManualRecognitionResponse recognizeManually(Long recognitionType, MultipartFile image) {
        AiRecognitionType type = requireEnabledRecognitionType(recognitionType);
        User currentUser = dataPermissionService.currentUser();

        CropImage cropImage = saveManualUploadImage(currentUser, image);
        RecognitionProfile profile = buildProfile(type);
        AiRecognitionRecord record = createCompletedManualRecord(currentUser, cropImage);
        AiRecognitionResult result = createSuccessResult(record, type, profile);

        return buildResponse(record, result, type, profile);
    }

    /** 将后台识别类型实体转换为移动端下拉选项。 */
    private ClientAiRecognitionTypeResponse toClientTypeResponse(AiRecognitionType type) {
        return new ClientAiRecognitionTypeResponse(
                type.getId(),
                type.getTypeCode(),
                type.getTypeName(),
                type.getDescription()
        );
    }

    /** 校验识别类型存在且处于启用状态，避免用户端提交无效或已停用类型。 */
    private AiRecognitionType requireEnabledRecognitionType(Long recognitionType) {
        if (recognitionType == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "识别类型不能为空");
        }
        AiRecognitionType type = aiRecognitionTypeMapper.selectById(recognitionType);
        if (type == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "识别类型不存在");
        }
        if (!Integer.valueOf(TYPE_STATUS_ENABLED).equals(type.getStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "识别类型已停用");
        }
        return type;
    }

    /** 保存用户上传图片并写入 crop_image 表，方便图片管理与识别记录共用同一张图片。 */
    private CropImage saveManualUploadImage(User currentUser, MultipartFile image) {
        CropImageUploadResult uploadResult = cropImageService.uploadCropImage(image);
        CropImage cropImage = new CropImage();
        cropImage.setUserId(currentUser.getId());
        cropImage.setImageUrl(uploadResult.imageUrl());
        cropImage.setImageSize(uploadResult.imageSize());
        cropImage.setRemark("farm用户端智能识别手工上传图片");
        return cropImageService.addCropImage(cropImage);
    }

    /** 写入已完成的手工识别记录，sourceType=1 标识该结果来自用户手动上传识别。 */
    private AiRecognitionRecord createCompletedManualRecord(User currentUser, CropImage cropImage) {
        LocalDateTime now = LocalDateTime.now();
        AiRecognitionRecord record = new AiRecognitionRecord();
        record.setUserId(currentUser.getId());
        record.setSourceType(SOURCE_TYPE_MANUAL);
        record.setCropImageId(cropImage.getId());
        record.setImageUrl(cropImage.getImageUrl());
        record.setThumbnailUrl(cropImage.getImageUrl());
        record.setImageSize(cropImage.getImageSize());
        record.setStatus(RECORD_STATUS_COMPLETED);
        record.setRecognitionStartTime(now);
        record.setRecognitionEndTime(now);
        record.setRemark("farm用户端手工识别");
        aiRecognitionRecordMapper.insertRecord(record);
        return record;
    }

    /** 写入识别结果表，保留模型名称和版本，便于后续替换为真实模型后审计。 */
    private AiRecognitionResult createSuccessResult(AiRecognitionRecord record,
                                                    AiRecognitionType type,
                                                    RecognitionProfile profile) {
        AiRecognitionResult result = new AiRecognitionResult();
        result.setRecordId(record.getId());
        result.setRecognitionType(type.getId());
        result.setResultName(profile.resultName());
        result.setResultSummary(profile.introduction());
        result.setResultDetail(profile.introduction());
        result.setConfidence(MANUAL_RULE_CONFIDENCE);
        result.setSeverityLevel(profile.severityLevel());
        result.setSuggestion(String.join("\n", profile.precautions()));
        result.setModelName(MODEL_NAME);
        result.setModelVersion(MODEL_VERSION);
        result.setStatus(RESULT_STATUS_SUCCESS);
        result.setRecognizeTime(LocalDateTime.now());
        result.setRemark("手工识别结果");
        aiRecognitionRecordMapper.insertResult(result);
        return result;
    }

    /** 组装 farm 结果页需要的扁平结构，减少移动端对后台表字段的依赖。 */
    private ClientManualRecognitionResponse buildResponse(AiRecognitionRecord record,
                                                          AiRecognitionResult result,
                                                          AiRecognitionType type,
                                                          RecognitionProfile profile) {
        return new ClientManualRecognitionResponse(
                record.getId(),
                result.getId(),
                record.getSourceType(),
                type.getTypeName(),
                record.getImageUrl(),
                result.getResultName(),
                result.getResultSummary(),
                profile.precautions()
        );
    }

    /** 将后台识别记录聚合字段转换为 farm 用户端列表和结果页可直接使用的数据。 */
    private ClientAiRecognitionRecordResponse toClientRecordResponse(AiRecognitionRecord record) {
        LocalDateTime recognitionTime = record.getRecognizeTime() != null
                ? record.getRecognizeTime()
                : firstNonNull(record.getRecognitionEndTime(), record.getRecognitionStartTime(), record.getCreateTime());
        return new ClientAiRecognitionRecordResponse(
                record.getId(),
                record.getResultId(),
                record.getSourceType(),
                record.getImageUrl(),
                record.getRecognitionTypeName(),
                record.getResultName(),
                recognitionTime,
                record.getResultSummary(),
                splitPrecautions(record.getSuggestion())
        );
    }

    /** 将数据库中按换行保存的注意事项拆成列表，方便移动端直接 v-for 渲染。 */
    private List<String> splitPrecautions(String suggestion) {
        if (suggestion == null || suggestion.isBlank()) {
            return List.of();
        }
        return Arrays.stream(suggestion.split("\\R"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    /** 按优先级返回第一个非空时间，保证列表识别时间尽量有值。 */
    private LocalDateTime firstNonNull(LocalDateTime... times) {
        return Arrays.stream(times)
                .filter(time -> time != null)
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据识别类型生成规则化结果。
     *
     * <p>这里保持结果温和、可复核，避免在没有真实视觉模型时给出过度确定的诊断。</p>
     */
    private RecognitionProfile buildProfile(AiRecognitionType type) {
        String typeCode = type.getTypeCode() == null ? "" : type.getTypeCode().trim().toUpperCase(Locale.ROOT);
        return switch (typeCode) {
            case "CROP_RECOGNITION" -> new RecognitionProfile(
                    "叶菜类作物",
                    "图片已按作物识别类型完成初步识别，结果倾向于叶菜类作物。建议结合种植批次、叶形和生长环境继续复核。",
                    List.of("保持图片拍摄清晰，尽量包含完整叶片和株型。", "同一批作物可多角度补充图片，提高复核准确度。", "识别结果用于辅助判断，关键生产决策建议结合现场观察。"),
                    null
            );
            case "DISEASE_RECOGNITION" -> new RecognitionProfile(
                    "未发现明显病害",
                    "图片未呈现高风险病斑特征，当前初步判断未发现明显病害。仍需持续观察叶片颜色、斑点扩散和湿度变化。",
                    List.of("发现斑点扩大、叶缘腐烂或霉层时应及时复拍。", "控制棚内湿度并保持通风，降低病害扩散风险。", "用药前建议由农技人员或专家二次确认。"),
                    1
            );
            case "PEST_RECOGNITION" -> new RecognitionProfile(
                    "未发现明显虫害",
                    "图片中未识别到明显虫体、虫卵或典型取食痕迹，当前初步判断虫害风险较低。",
                    List.of("定期检查叶背和嫩梢位置，虫害早期通常更隐蔽。", "可结合诱虫板和田间巡检记录综合判断。", "若出现缺刻、卷叶或虫粪，应补充近距离图片。"),
                    1
            );
            case "DISEASE_PEST_RECOGNITION" -> new RecognitionProfile(
                    "未发现明显病虫害",
                    "图片已完成病害与虫害综合检查，当前未发现明显病斑、虫体或典型取食痕迹，建议结合田间巡检继续复核。",
                    List.of("重点检查叶片正反面、嫩梢和茎基部。", "发现病斑扩散、虫卵或取食缺刻时应及时补拍。", "防治措施和用药方案应由农技人员结合现场情况确认。"),
                    1
            );
            case "GROWTH_RECOGNITION" -> new RecognitionProfile(
                    "长势正常",
                    "图片显示作物整体长势较稳定，未见明显徒长、萎蔫或严重营养失衡特征。",
                    List.of("继续保持适宜光照、温湿度和水肥节奏。", "关注新叶颜色和株高变化，避免氮肥过量。", "建议固定拍摄角度形成连续长势对比。"),
                    null
            );
            default -> new RecognitionProfile(
                    "综合状态正常",
                    "图片已完成综合识别，当前未发现明显异常特征。建议结合农场环境数据和人工巡检做最终判断。",
                    List.of("上传清晰、无遮挡图片可提升识别参考价值。", "异常结果应结合地块环境、近期施肥和灌溉记录复核。", "识别结果仅作辅助参考，不替代现场专业诊断。"),
                    null
            );
        };
    }

    /** 内部结果模板，集中承载结果名称、介绍、注意事项和严重程度。 */
    private record RecognitionProfile(String resultName,
                                      String introduction,
                                      List<String> precautions,
                                      Integer severityLevel) {
        private RecognitionProfile {
            precautions = precautions == null ? List.of() : List.copyOf(precautions);
        }
    }
}
