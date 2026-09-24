package com.smart_plant.smart_plant.dto;

import lombok.Data;

/**
 * 用户端获取短信验证码请求。
 */
@Data
public class ClientSmsCodeRequest {

    /** 接收验证码的中国大陆手机号。 */
    private String phone;
}
