package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ErrorLogHandleRequest;
import com.smart_plant.smart_plant.entity.ErrorLog;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.ErrorLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** smart_farm 错误日志页面的一一对应后端接口。 */
@RestController
@RequestMapping("/error-log")
@RequiredArgsConstructor
@RequirePermission("error_log:manage")
public class ErrorLogController {

    private final ErrorLogService errorLogService;

    @GetMapping("/list")
    public R<PageInfo<ErrorLog>> list(@RequestParam(required = false) String traceId,
                                      @RequestParam(required = false) String operatorName,
                                      @RequestParam(required = false) String requestUri,
                                      @RequestParam(required = false) Integer handleStatus,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(errorLogService.list(
                traceId, operatorName, requestUri, handleStatus, startTime, endTime, pageNum, pageSize));
    }

    /** 详情接口单独返回异常堆栈，列表接口不携带大字段。 */
    @GetMapping("/{id}")
    public R<ErrorLog> detail(@PathVariable Long id) {
        return R.success(errorLogService.getDetail(id));
    }

    /** 仅修改运维处理字段，原始异常现场保持不可变。 */
    @PutMapping("/{id}/handle")
    public R<ErrorLog> updateHandle(@PathVariable Long id,
                                    @Valid @RequestBody ErrorLogHandleRequest request) {
        return R.success(errorLogService.updateHandle(id, request));
    }
}
