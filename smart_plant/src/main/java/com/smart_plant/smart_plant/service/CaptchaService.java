package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.AdminCaptchaResponse;

public interface CaptchaService {

    AdminCaptchaResponse create();

    void verify(String captchaKey, String captcha);
}
