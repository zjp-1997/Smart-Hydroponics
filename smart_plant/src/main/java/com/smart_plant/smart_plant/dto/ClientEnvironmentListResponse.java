package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * farm 用户端环境监测数据响应对象。
 *
 * <p>该对象聚合空气环境、水质、水泵和光照设备的最新采集数据。
 * 后端返回完整指标，移动端页面按当前 UI 需求选择展示其中一部分。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientEnvironmentListResponse {

    /** 空气温度（℃），来自 environment_data.air_temperature。 */
    private BigDecimal airTemperature;

    /** 空气湿度（%），来自 environment_data.air_humidity。 */
    private BigDecimal airHumidity;

    /** 风速（m/s），来自 environment_data.wind_speed。 */
    private BigDecimal windSpeed;

    /** 气压（hPa），来自 environment_data.air_pressure。 */
    private BigDecimal airPressure;

    /** 二氧化碳浓度（ppm），来自 environment_data.co2_concentration。 */
    private BigDecimal co2Concentration;

    /** PM2.5（μg/m³），来自 environment_data.pm25。 */
    private BigDecimal pm25;

    /** 水温（℃），来自 water_quality_data.water_temperature。 */
    private BigDecimal waterTemperature;

    /** PH 值，来自 water_quality_data.ph。 */
    private BigDecimal ph;

    /** EC 值（mS/cm），来自 water_quality_data.ec_value。 */
    private BigDecimal ecValue;

    /** 溶解氧（mg/L），来自 water_quality_data.dissolved_oxygen。 */
    private BigDecimal dissolvedOxygen;

    /** 光照强度（lux），来自 light_data.light_intensity。 */
    private BigDecimal lightIntensity;

    /** 水流量（m³/h），来自 pump_data.water_flow。 */
    private BigDecimal waterFlow;

    /** 水压（MPa），来自 pump_data.water_pressure。 */
    private BigDecimal waterPressure;

    /** 空气环境数据最近采集时间。 */
    private LocalDateTime environmentCollectTime;

    /** 水质数据最近采集时间。 */
    private LocalDateTime waterQualityCollectTime;

    /** 水泵数据最近采集时间。 */
    private LocalDateTime pumpCollectTime;

    /** 光照数据最近采集时间。 */
    private LocalDateTime lightCollectTime;
}
