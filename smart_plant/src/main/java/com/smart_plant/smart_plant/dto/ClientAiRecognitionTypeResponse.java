package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * farm 用户端识别类型响应对象。
 *
 * <p>用户端只需要下拉选择所需字段，不暴露管理端的创建时间、状态维护等后台信息。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientAiRecognitionTypeResponse {

    /** 识别类型ID，提交手工识别时作为 recognitionType 字段上传。 */
    private Long id;

    /** 识别类型编码，作为前端缓存和后续模型路由的稳定标识。 */
    private String typeCode;

    /** 识别类型名称，用于 picker 展示。 */
    private String typeName;

    /** 识别类型说明，用于后续需要展示类型说明时复用。 */
    private String description;
}
