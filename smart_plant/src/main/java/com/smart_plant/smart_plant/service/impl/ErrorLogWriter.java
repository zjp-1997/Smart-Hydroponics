package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.ErrorLog;
import com.smart_plant.smart_plant.mapper.ErrorLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用独立事务写入错误日志，确保原业务事务回滚时异常记录仍可提交。
 * 该组件与采集服务分离，也让采集服务能够捕获事务提交阶段的失败。
 */
@Component
@RequiredArgsConstructor
public class ErrorLogWriter {

    private final ErrorLogMapper errorLogMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(ErrorLog errorLog) {
        errorLogMapper.insert(errorLog);
    }
}
