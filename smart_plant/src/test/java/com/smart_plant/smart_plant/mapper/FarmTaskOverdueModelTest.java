package com.smart_plant.smart_plant.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmTaskOverdueModelTest {

    @Test
    void overdueIsDerivedAndStatisticsAreMutuallyExclusive() throws IOException {
        String mapper = new ClassPathResource("mapper/FarmTaskMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8);
        String migration = new ClassPathResource("db/migration/V42__unify_farm_task_overdue_model.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertFalse(mapper.contains("ft.status IN (1, 2, 4)"));
        assertFalse(mapper.contains("status IN (1, 4)"));
        assertTrue(mapper.contains("ft.status = 1 AND ft.deadline_time &gt;= NOW()"));
        assertTrue(mapper.contains("ft.status IN (1, 2) AND ft.deadline_time &lt; NOW()"));
        assertTrue(migration.contains("WHERE `status` = 4"));
        assertTrue(migration.contains("CHECK (`status` IN (1, 2, 3, 5))"));
    }
}
