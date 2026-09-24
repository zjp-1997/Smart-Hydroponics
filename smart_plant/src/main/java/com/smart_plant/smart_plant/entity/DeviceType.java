package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备类型实体类。
 *
 * <p>该类与数据库 device_type 表对应，用于维护传感器、执行器等设备类型基础信息。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceType {

    /** 设备类型主键ID，由数据库自增生成。 */
    private Long id;

    /** 类型编码，全表唯一，用于前端选项、设备接入或第三方系统识别。 */
    private String typeCode;

    /** 类型名称，用于页面展示，例如环境传感器、水泵。 */
    private String typeName;

    /** 设备分类，1表示传感器，2表示执行器。 */
    private Integer category;

    /** 类型说明，描述该类设备的用途或采集/控制能力。 */
    private String description;

    /** 状态，1启用，0禁用；禁用后不允许新建设备继续选择该类型。 */
    private Integer status;

    /** 记录创建时间，由数据库默认写入。 */
    private LocalDateTime createTime;

    /** 记录更新时间，由数据库自动维护。 */
    private LocalDateTime updateTime;
}
