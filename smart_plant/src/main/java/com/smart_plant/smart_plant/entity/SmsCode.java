package com.smart_plant.smart_plant.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短信验证码记录，对应 sms_code 表。
 */
@Data
public class SmsCode {

    /** 验证码记录主键。 */
    private Long id;

    /** 接收验证码的中国大陆手机号。 */
    private String phone;

    /** 六位数字验证码；当前模拟接口会将其直接返回给 farm 端。 */
    private String code;

    /** 使用场景：1 登录，4 绑定手机号。 */
    private Integer scene;

    /** 验证码失效时间。 */
    private LocalDateTime expireTime;

    /** 是否已使用：0 未使用，1 已使用。 */
    private Integer used;

    /** 验证码实际使用时间。 */
    private LocalDateTime usedTime;

    /** 发码请求 IP，用于问题排查和后续风控。 */
    private String sendIp;

    /** 发码客户端标识，用于问题排查。 */
    private String userAgent;

    /** 状态：1 正常，0 已失效。 */
    private Integer status;

    /** 记录说明。 */
    private String remark;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
