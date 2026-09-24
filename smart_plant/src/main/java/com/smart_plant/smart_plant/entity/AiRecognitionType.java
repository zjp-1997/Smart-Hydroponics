package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI识别类型实体类。
 *
 * <p>该类与数据库 ai_recognition_type 表对应，用于维护作物识别、病害识别、虫害识别等AI识别类型。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRecognitionType {

    /** 识别类型主键ID，由数据库自增生成。 */
    private Long id;

    /** 类型编码，全表唯一，用于接口、模型和前端做稳定识别。 */
    private String typeCode;

    /** 类型名称，用于页面展示。 */
    private String typeName;

    /** 类型说明，描述该识别类型适用的业务场景。 */
    private String description;

    /** 状态，1启用，0禁用。 */
    private Integer status;

    /** 记录创建时间，由数据库默认写入。 */
    private LocalDateTime createTime;

    /** 记录更新时间，由数据库自动维护。 */
    private LocalDateTime updateTime;
}
