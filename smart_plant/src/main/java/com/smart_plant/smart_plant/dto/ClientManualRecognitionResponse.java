package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * farm 用户端手工识别结果响应对象。
 *
 * <p>字段名称直接贴近结果页展示需要，避免移动端再理解后台表结构。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientManualRecognitionResponse {

    /** 识别记录ID，可用于后续识别记录详情或问题追踪。 */
    private Long recordId;

    /** 识别结果ID，对应 ai_recognition_result.id。 */
    private Long resultId;

    /** 识别来源，1表示用户手工上传识别。 */
    private Integer sourceType;

    /** 识别类型名称。 */
    private String recognitionType;

    /** 识别图片地址，前端会统一补齐后端域名后展示。 */
    private String recognitionImage;

    /** 识别结果主结论。 */
    private String recognitionResult;

    /** 识别结果介绍，解释主结论的业务含义。 */
    private String resultIntroduction;

    /** 注意事项列表，结果页按序号渲染。 */
    private List<String> precautions = new ArrayList<>();
}
