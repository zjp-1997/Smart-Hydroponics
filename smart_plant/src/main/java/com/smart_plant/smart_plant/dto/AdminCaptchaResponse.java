package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCaptchaResponse {

    private String captchaKey;

    private String captchaImage;

    private Long expireSeconds;
}
