package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ErrorLogHandleRequest;
import com.smart_plant.smart_plant.entity.ErrorLog;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

/** 错误采集与后台运维查询服务。 */
public interface ErrorLogService {

    String record(Throwable throwable, HttpServletRequest request);

    PageInfo<ErrorLog> list(String traceId, String operatorName, String requestUri,
                            Integer handleStatus, LocalDateTime startTime, LocalDateTime endTime,
                            Integer pageNum, Integer pageSize);

    ErrorLog getDetail(Long id);

    ErrorLog updateHandle(Long id, ErrorLogHandleRequest request);
}
