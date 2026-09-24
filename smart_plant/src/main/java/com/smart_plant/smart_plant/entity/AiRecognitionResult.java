package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI识别结果实体类。
 *
 * <p>该类对应 ai_recognition_result 表，用于保存一次识别记录下的模型或规则识别结论。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRecognitionResult {

    /** 识别结果主键ID，由数据库自增生成。 */
    private Long id;

    /** 关联的AI识别记录ID，对应 ai_recognition_record.id。 */
    private Long recordId;

    /** 识别出的作物ID，无法确认具体作物时允许为空。 */
    private Long cropId;

    /** 识别类型ID，对应 ai_recognition_type.id。 */
    private Long recognitionType;

    /** 识别结果名称，用于结果页的主结论展示。 */
    private String resultName;

    /** 识别结果摘要，用于向用户解释结论含义。 */
    private String resultSummary;

    /** 识别结果详情，保留更完整的识别说明或后续模型返回内容。 */
    private String resultDetail;

    /** 关联病虫害ID，病虫害识别命中知识库时回填。 */
    private Long diseasePestId;

    /** 识别置信度，单位为百分比。 */
    private BigDecimal confidence;

    /** 严重程度，1轻微、2中等、3严重，正常结果可为空。 */
    private Integer severityLevel;

    /** 处理建议或注意事项，前端可拆分为注意事项列表展示。 */
    private String suggestion;

    /** 识别模型或规则引擎名称，方便后续审计识别来源。 */
    private String modelName;

    /** 识别模型或规则版本，便于模型升级后追踪结果差异。 */
    private String modelVersion;

    /** 识别状态，1成功、2失败、3处理中。 */
    private Integer status;

    /** 失败原因，仅 status=2 时使用。 */
    private String failReason;

    /** 识别完成时间。 */
    private LocalDateTime recognizeTime;

    /** 结果备注，用于标识手工上传识别等业务来源。 */
    private String remark;

    /** 记录创建时间，由数据库默认写入。 */
    private LocalDateTime createTime;

    /** 记录更新时间，由数据库自动维护。 */
    private LocalDateTime updateTime;
}
