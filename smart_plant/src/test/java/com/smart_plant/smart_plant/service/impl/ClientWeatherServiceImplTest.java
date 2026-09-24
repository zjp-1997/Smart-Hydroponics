package com.smart_plant.smart_plant.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientWeatherServiceImplTest {

    @Test
    void mapsOpenMeteoWeatherCodesToFrontendWeatherNames() {
        assertEquals("晴", ClientWeatherServiceImpl.weatherName(0));
        assertEquals("多云", ClientWeatherServiceImpl.weatherName(2));
        assertEquals("雾", ClientWeatherServiceImpl.weatherName(48));
        assertEquals("暴雨", ClientWeatherServiceImpl.weatherName(82));
        assertEquals("雷阵雨伴冰雹", ClientWeatherServiceImpl.weatherName(99));
        assertEquals("未知", ClientWeatherServiceImpl.weatherName(-1));
    }
}
