package com.smart_plant.smart_plant.dto;

import lombok.Data;

@Data
public class AdminLoginRequest {

    private String username;

    private String password;

    private String captcha;

    private String captchaKey;

    private Boolean rememberMe;
}
