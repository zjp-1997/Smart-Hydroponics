package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientWeatherResponse;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.ClientWeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * farm 首页天气接口。
 *
 * <p>移动端只上传设备定位经纬度，第三方 Key 和服务调用全部由后端托管。</p>
 */
@RestController
@RequestMapping({"/client/weather", "/api/client/weather"})
@RequiredArgsConstructor
public class ClientWeatherController {

    private final ClientWeatherService clientWeatherService;

    @GetMapping("/current")
    public R<ClientWeatherResponse> getCurrentWeather(@RequestParam Double longitude,
                                                      @RequestParam Double latitude,
                                                      @RequestParam(defaultValue = "wgs84") String coordinateSystem) {
        // 根据当前位置返回首页天气条所需的城市、温度、湿度和 PM2.5。
        return R.success(clientWeatherService.getCurrentWeather(longitude, latitude, coordinateSystem));
    }
}
