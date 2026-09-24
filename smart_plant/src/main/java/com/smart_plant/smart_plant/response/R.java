package com.smart_plant.smart_plant.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;

    private String message;

    private T data;

    public static <T> R<T> success() {
        return result(ResponseCode.SUCCESS, null);
    }

    public static <T> R<T> success(T data) {
        return result(ResponseCode.SUCCESS, data);
    }

    public static <T> R<T> success(String message, T data) {
        return new R<>(ResponseCode.SUCCESS.getCode(), message, data);
    }

    public static <T> R<T> fail() {
        return result(ResponseCode.FAIL, null);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(ResponseCode.FAIL.getCode(), message, null);
    }

    public static <T> R<T> fail(ResponseCode responseCode) {
        return result(responseCode, null);
    }

    public static <T> R<T> fail(ResponseCode responseCode, String message) {
        return new R<>(responseCode.getCode(), message, null);
    }

    public static <T> R<T> result(ResponseCode responseCode, T data) {
        return new R<>(responseCode.getCode(), responseCode.getMessage(), data);
    }
}
