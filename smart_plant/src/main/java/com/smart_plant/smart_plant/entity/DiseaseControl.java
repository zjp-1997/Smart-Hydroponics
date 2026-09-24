package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 病虫害防治措施实体。
 *
 * <p>该实体对应数据库中的 disease_control 表，用于保存某个病虫害的预防或治疗措施。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseControl {

    /** 防治措施主键ID。 */
    private Long id;

    /** 关联的病虫害ID，对应 disease_pest.id。 */
    private Long diseaseId;

    /** 病虫害名称，列表展示时通过关联 disease_pest 表带出。 */
    private String diseaseName;

    /** 防治类型：1预防，2治疗。 */
    private Integer controlType;

    /** 防治手段：1农业防治，2物理防治，3化学防治，4生物防治。 */
    private Integer controlCategory;

    /** 措施内容，描述具体预防或治疗方案。 */
    private String method;

    /** 药剂名称，可为空，适用于需要记录用药的措施。 */
    private String drugName;

    /** 使用方法，描述药剂或措施的操作方式。 */
    private String usageMethod;

    /** 剂型、浓度或稀释倍数，例如5%乳油1500倍液。 */
    private String dosageSpec;

    /** 药剂安全间隔期，单位为天。 */
    private Integer safetyIntervalDays;

    /** 禁限用、轮换用药和施药安全等注意事项。 */
    private String precautions;

    /** 适用阶段，例如苗期、花期、结果期等。 */
    private String suitableStage;

    /** 状态：1启用，0停用。 */
    private Integer status;

    /** 展示顺序，数值越小越靠前。 */
    private Integer sortOrder;

    /** 备注信息，用于记录补充说明。 */
    private String remark;

    /** 创建时间，由数据库默认维护。 */
    private LocalDateTime createTime;

    /** 更新时间，由数据库在更新时自动维护。 */
    private LocalDateTime updateTime;
}
