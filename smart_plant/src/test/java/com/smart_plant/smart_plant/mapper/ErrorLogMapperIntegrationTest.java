package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.ErrorLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 V7 表结构与 MyBatis 映射可在本地 smart_plant 数据库完整工作。 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class ErrorLogMapperIntegrationTest {

    @Autowired
    private ErrorLogMapper mapper;

    @Test
    void insertListDetailAndOptimisticUpdateWorkTogether() {
        ErrorLog errorLog = new ErrorLog();
        errorLog.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        errorLog.setLevel("ERROR");
        errorLog.setExceptionType("java.lang.IllegalStateException");
        errorLog.setErrorMessage("integration error");
        errorLog.setRequestMethod("GET");
        errorLog.setRequestUri("/__error_log_mapper_test__");
        errorLog.setStackTrace("stack-content");
        errorLog.setCreateTime(LocalDateTime.now());
        assertEquals(1, mapper.insert(errorLog));

        ErrorLog listRow = mapper.selectList(
                errorLog.getTraceId(), null, null, 0, null, null).getFirst();
        assertNull(listRow.getStackTrace(), "列表不应加载大字段堆栈");
        assertEquals("stack-content", mapper.selectById(errorLog.getId()).getStackTrace());

        assertEquals(1, mapper.updateHandle(errorLog.getId(), 1, "resolved", 2L, "admin", 0));
        assertEquals(0, mapper.updateHandle(errorLog.getId(), 2, "stale", 2L, "admin", 0));
        ErrorLog updated = mapper.selectById(errorLog.getId());
        assertEquals(1, updated.getHandleStatus());
        assertEquals(1, updated.getVersion());
        assertTrue(updated.getHandleTime() != null);
    }
}
