package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 模拟短信发码响应。
 *
 * <p>code 仅用于当前无短信供应商的联调环境；接入真实短信后必须从响应中移除。</p>
 */
@Data
@AllArgsConstructor
public class ClientSmsCodeResponse {

    /** 本次生成的六位数字验证码。 */
    private String code;

    /** 验证码剩余有效秒数。 */
    private long expiresIn;
}
