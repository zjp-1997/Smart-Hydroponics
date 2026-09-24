package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 作物信息实体，对应 crop 表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Crop {

    /** 作物ID */
    private Long id;

    /** 所属用户ID，空值表示系统公共作物 */
    private Long userId;

    /** 所属用户名，列表展示字段 */
    private String username;

    /** 所属用户昵称，列表展示字段 */
    private String nickname;

    /** 作物类型ID */
    private Long typeId;

    /** 作物类型名称，列表展示字段 */
    private String typeName;

    /** 作物名称 */
    private String cropName;

    /** 系统自动生成的作物编码 */
    private String cropCode;

    /** 品种名称 */
    private String variety;

    /** 推荐生长周期，单位天 */
    private Integer growthDays;

    /** 适宜温度范围 */
    private String suitableTemperature;

    /** 适宜湿度范围 */
    private String suitableHumidity;

    /** 适宜PH范围 */
    private String suitablePh;

    /** 适宜CO2浓度范围，如400-1000ppm */
    private String suitableCo2;

    /** 适宜PM2.5范围，如0-75μg/m³ */
    private String suitablePm25;

    /** 适宜EC值范围，如1.0-2.5mS/cm */
    private String suitableEc;

    /** 适宜溶解氧范围，如5.0-8.0mg/L */
    private String suitableDissolvedOxygen;

    /** 适宜光照强度范围，如10000-30000lux */
    private String suitableLight;

    /** 作物图片 */
    private String imageUrl;

    /** 作物说明 */
    private String description;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
