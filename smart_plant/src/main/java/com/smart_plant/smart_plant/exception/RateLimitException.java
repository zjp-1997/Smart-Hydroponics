package com.smart_plant.smart_plant.exception;

import com.smart_plant.smart_plant.response.ResponseCode;

public class RateLimitException extends BusinessException {

    public RateLimitException(String message) {
        super(ResponseCode.TOO_MANY_REQUESTS, message);
    }
}
