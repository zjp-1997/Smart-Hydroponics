package com.smart_plant.smart_plant.dto;

import lombok.Data;

/**
 * 手机号与验证码组合请求，供手机号绑定使用。
 */
@Data
public class ClientPhoneCodeRequest {

    /** 待绑定的手机号。 */
    private String phone;

    /** 用户输入的六位短信验证码。 */
    private String code;
}
