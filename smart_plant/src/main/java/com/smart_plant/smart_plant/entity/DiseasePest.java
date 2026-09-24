package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 病虫害基础信息实体。
 *
 * <p>该实体对应数据库中的 disease_pest 表，保存病害、虫害和生理性病害的基础知识。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiseasePest {

    /** 病虫害主键ID。 */
    private Long id;

    /** 关联作物类型ID，对应 crop_type.id，可为空表示通用病虫害。 */
    private Long cropTypeId;

    /** 作物类型名称，列表展示时通过关联 crop_type 表带出。 */
    private String cropTypeName;

    /** 危害作物范围展示文本，用于表达跨多个作物类型的适用范围。 */
    private String affectedCrops;

    /** 病虫害名称。 */
    private String name;

    /** 类型：1病害，2虫害，3生理性病害。 */
    private Integer type;

    /** 症状描述。 */
    private String symptom;

    /** 发生原因。 */
    private String cause;

    /** 易发生阶段，例如苗期、花期、成熟期等。 */
    private String suitableStage;

    /** 主要发生时期或季节，例如春秋季或4月至6月。 */
    private String occurrencePeriod;

    /** 害虫、病原或生理性病害的生活习性。 */
    private String livingHabits;

    /** 适宜发生的温度、湿度、连作等环境条件。 */
    private String suitableEnvironment;

    /** 飞行、土壤、种苗调运等传播或扩散途径。 */
    private String transmissionRoute;

    /** 封面图片地址。 */
    private String coverImage;

    /** 详情页图片地址集合，以 MySQL JSON 数组持久化。 */
    private List<String> imageUrls;

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

    /** 防治措施列表，详情查询时由 service 组装。 */
    private List<DiseaseControl> controls;
}
