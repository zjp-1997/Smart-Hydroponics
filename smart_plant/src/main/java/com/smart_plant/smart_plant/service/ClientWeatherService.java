package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.ClientWeatherResponse;

/**
 * farm 首页天气服务。
 */
public interface ClientWeatherService {

    /**
     * 根据经纬度查询当前位置天气和空气质量信息。
     *
     * @param longitude 经度
     * @param latitude 纬度
     * @param coordinateSystem 坐标系，wgs84 或 gcj02
     * @return 首页天气展示数据
     */
    ClientWeatherResponse getCurrentWeather(Double longitude, Double latitude, String coordinateSystem);
}
