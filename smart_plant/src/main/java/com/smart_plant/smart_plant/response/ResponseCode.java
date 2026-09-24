package com.smart_plant.smart_plant.response;

import lombok.Getter;

@Getter
public enum ResponseCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    MODEL_BALANCE_INSUFFICIENT(402, "模型账户余额不足"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用");

    private final Integer code;

    private final String message;

    ResponseCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
