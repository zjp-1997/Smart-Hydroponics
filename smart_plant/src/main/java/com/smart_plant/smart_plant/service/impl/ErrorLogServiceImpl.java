package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ErrorLogHandleRequest;
import com.smart_plant.smart_plant.entity.ErrorLog;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.ErrorLogMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.ErrorLogService;
import com.smart_plant.smart_plant.service.OperationLogService;
import com.smart_plant.smart_plant.utils.ClientRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.UUID;

/** 错误日志服务实现，写日志失败时不得覆盖原始业务异常。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorLogServiceImpl implements ErrorLogService {

    private static final int MAX_STACK_TRACE_LENGTH = 32_000;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1_000;
    private static final int MAX_REQUEST_URI_LENGTH = 500;

    private final ErrorLogMapper errorLogMapper;
    private final ErrorLogWriter errorLogWriter;
    private final DataPermissionService dataPermissionService;
    private final OperationLogService operationLogService;

    @Override
    public String record(Throwable throwable, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        try {
            User user = CurrentUserContext.get();
            ErrorLog errorLog = new ErrorLog();
            errorLog.setTraceId(traceId);
            errorLog.setLevel("ERROR");
            errorLog.setExceptionType(throwable.getClass().getName());
            errorLog.setErrorMessage(truncate(resolveMessage(throwable), MAX_ERROR_MESSAGE_LENGTH));
            errorLog.setRequestMethod(request == null ? null : request.getMethod());
            // 只保存 URI，不保存 query string 和请求体，避免口令、令牌等敏感参数落库。
            errorLog.setRequestUri(request == null ? null : truncate(request.getRequestURI(), MAX_REQUEST_URI_LENGTH));
            errorLog.setOperatorId(user == null ? null : user.getId());
            errorLog.setOperatorName(user == null ? null : user.getUsername());
            errorLog.setIp(request == null ? null : truncate(ClientRequestUtils.getClientIp(request), 64));
            errorLog.setUserAgent(request == null ? null : ClientRequestUtils.getUserAgent(request));
            errorLog.setStackTrace(toStackTrace(throwable));
            errorLog.setCreateTime(LocalDateTime.now());
            errorLogWriter.persist(errorLog);
        } catch (Exception loggingException) {
            // 错误日志属于旁路能力，持久化失败不能改变原接口的异常响应。
            log.error("Failed to persist error log, traceId={}", traceId, loggingException);
        }
        log.error("Unhandled request exception, traceId={}", traceId, throwable);
        return traceId;
    }

    @Override
    public PageInfo<ErrorLog> list(String traceId, String operatorName, String requestUri,
                                   Integer handleStatus, LocalDateTime startTime, LocalDateTime endTime,
                                   Integer pageNum, Integer pageSize) {
        requireAdministrator();
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(errorLogMapper.selectList(
                trimToNull(traceId), trimToNull(operatorName), trimToNull(requestUri), handleStatus, startTime, endTime));
    }

    @Override
    public ErrorLog getDetail(Long id) {
        requireAdministrator();
        ErrorLog errorLog = errorLogMapper.selectById(id);
        if (errorLog == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "错误日志不存在");
        }
        return errorLog;
    }

    @Override
    @Transactional
    public ErrorLog updateHandle(Long id, ErrorLogHandleRequest request) {
        requireAdministrator();
        User handler = dataPermissionService.currentUser();
        String remark = trimToNull(request.getHandleRemark());
        int rows = errorLogMapper.updateHandle(
                id, request.getHandleStatus(), remark, handler.getId(), handler.getUsername(), request.getVersion());
        if (rows == 0) {
            if (errorLogMapper.selectById(id) == null) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "错误日志不存在");
            }
            throw new BusinessException(ResponseCode.FAIL, "错误日志已被其他管理员更新，请刷新后重试");
        }
        operationLogService.record("处理错误日志：" + id);
        return errorLogMapper.selectById(id);
    }

    /** 错误堆栈属于敏感运维数据，仅管理员可以查询和处理。 */
    private void requireAdministrator() {
        if (!dataPermissionService.isAdmin()) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "只有管理员可以访问错误日志");
        }
    }

    private String resolveMessage(Throwable throwable) {
        return StringUtils.hasText(throwable.getMessage()) ? throwable.getMessage() : throwable.getClass().getSimpleName();
    }

    private String toStackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return truncate(writer.toString(), MAX_STACK_TRACE_LENGTH);
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
