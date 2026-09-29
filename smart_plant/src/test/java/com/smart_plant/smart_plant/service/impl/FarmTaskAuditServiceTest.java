package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.FarmTask;
import com.smart_plant.smart_plant.entity.FarmTaskRecord;
import com.smart_plant.smart_plant.mapper.FarmTaskRecordMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FarmTaskAuditServiceTest {

    @Test
    void recordsSystemCreatedTaskWithoutForgingAnOperator() throws IOException {
        FarmTaskRecordMapper mapper = mock(FarmTaskRecordMapper.class);
        when(mapper.insert(any(FarmTaskRecord.class))).thenReturn(1);
        FarmTaskAuditService service = new FarmTaskAuditService(mapper);
        FarmTask task = new FarmTask();
        task.setId(88L);
        task.setStatus(1);

        service.record(task, null, FarmTaskAuditService.ACTION_CREATE,
                "传感器异常自动生成任务", null, 1, "SYSTEM");

        ArgumentCaptor<FarmTaskRecord> captor = ArgumentCaptor.forClass(FarmTaskRecord.class);
        verify(mapper).insert(captor.capture());
        FarmTaskRecord record = captor.getValue();
        assertEquals(88L, record.getTaskId());
        assertNull(record.getOperatorId());
        assertEquals("系统自动处理", record.getOperatorNameSnapshot());
        assertEquals(FarmTaskAuditService.ACTION_CREATE, record.getActionType());
        assertEquals("SYSTEM", record.getSourceClient());

        String migration = new ClassPathResource("db/migration/V43__complete_farm_task_audit_trail.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        String foreignKeyMigration = new ClassPathResource("db/migration/V44__preserve_farm_task_audit_trail.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        assertTrue(migration.contains("MODIFY COLUMN `operator_id` bigint DEFAULT NULL"));
        assertTrue(migration.contains("`action_type` IN (1, 2, 3, 4, 5, 6, 7)"));
        assertTrue(migration.contains("AND r.action_type = 5"));
        assertTrue(foreignKeyMigration.contains("ON DELETE RESTRICT ON UPDATE RESTRICT"));
    }
}
