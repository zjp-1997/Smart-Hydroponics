package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 作物分类实体，对应 crop_type 表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CropType {

    /** 类型ID */
    private Long id;

    /** 父类型ID，一级分类为空 */
    private Long parentId;

    /** 父分类名称，列表展示字段 */
    private String parentName;

    /** 分类名称 */
    private String typeName;

    /** 描述 */
    private String description;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
