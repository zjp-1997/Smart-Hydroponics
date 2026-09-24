package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * farm 首页天气信息响应。
 *
 * <p>该 DTO 只返回首页红框区域需要展示的数据，减少移动端对第三方接口字段的依赖。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientWeatherResponse {

    /** 当前定位所属城市或区县。 */
    private String location;

    /** 高德行政区编码，后续可用于按城市缓存天气。 */
    private String adcode;

    /** 天气现象，例如：晴、多云。 */
    private String weather;

    /** 温度，单位摄氏度，不带单位。 */
    private String temperature;

    /** 湿度百分比，不带单位。 */
    private String humidity;

    /** PM2.5 数值；没有空气质量 Token 时返回 --。 */
    private String pm25;

    /** 第三方天气数据发布时间。 */
    private String reportTime;
}
