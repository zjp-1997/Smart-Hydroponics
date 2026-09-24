package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.entity.SystemSetting;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 登录前读取站点名称、图标和外观所需的公开配置接口。 */
@RestController
@RequestMapping("/public/system-settings")
@RequiredArgsConstructor
public class PublicSystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    public R<SystemSetting> getSetting() {
        return R.success(systemSettingService.getPublicSetting());
    }
}
