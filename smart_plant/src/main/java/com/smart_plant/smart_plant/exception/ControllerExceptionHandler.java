package com.smart_plant.smart_plant.exception;

import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.ErrorLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@RequiredArgsConstructor
public class ControllerExceptionHandler {

    private final ErrorLogService errorLogService;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusinessException(BusinessException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getCode());
        return ResponseEntity.status(status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status)
                .body(new R<>(exception.getCode(), exception.getMessage(), null));
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<R<Void>> handleRateLimitException(RateLimitException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(R.fail(ResponseCode.TOO_MANY_REQUESTS, exception.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<R<Void>> handleDataAccessException(DataAccessException exception,
                                                              HttpServletRequest request) {
        String traceId = errorLogService.record(exception, request);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(R.fail(ResponseCode.SERVICE_UNAVAILABLE, "服务暂不可用，请稍后再试（追踪编号：" + traceId + "）"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(R.fail(ResponseCode.PARAM_ERROR, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        // 将字段校验错误按400返回，它属于可预期的客户端错误，不写入系统错误日志。
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("参数错误");
        return ResponseEntity.badRequest().body(R.fail(ResponseCode.PARAM_ERROR, message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(R.fail(ResponseCode.PARAM_ERROR, "参数 " + exception.getName() + " 格式错误"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception exception, HttpServletRequest request) {
        // 只有非预期服务端错误进入错误日志，参数错误、限流等预期异常不会污染运维数据。
        String traceId = errorLogService.record(exception, request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail("系统异常，请联系管理员（追踪编号：" + traceId + "）"));
    }
}
