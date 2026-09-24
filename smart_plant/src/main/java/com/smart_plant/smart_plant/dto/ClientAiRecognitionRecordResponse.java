package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * farm 用户端识别记录列表响应对象。
 *
 * <p>该对象只暴露移动端列表和结果页需要的字段，避免 farm 端直接依赖后台管理表结构。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientAiRecognitionRecordResponse {

    /** 识别记录ID，用作列表唯一 key 和结果页缓存匹配标识。 */
    private Long recordId;

    /** 识别结果ID，对应 ai_recognition_result.id。 */
    private Long resultId;

    /** 识别来源，1表示手工上传识别。 */
    private Integer sourceType;

    /** 识别图片地址，前端统一补齐后端访问域名。 */
    private String recognitionImage;

    /** 识别类型名称，例如病害识别、生长状态识别。 */
    private String recognitionType;

    /** 识别结果名称，用于列表主结果和结果页主结论展示。 */
    private String recognitionResult;

    /** 识别时间，优先取结果完成时间，其次取记录完成时间。 */
    private LocalDateTime recognitionTime;

    /** 识别结果介绍，供点击列表记录进入结果页后展示。 */
    private String resultIntroduction;

    /** 注意事项列表，供结果页按序号展示。 */
    private List<String> precautions = new ArrayList<>();
}
