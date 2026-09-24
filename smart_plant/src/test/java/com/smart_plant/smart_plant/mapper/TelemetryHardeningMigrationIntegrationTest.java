package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.SmartPlantApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = SmartPlantApplication.class)
@ActiveProfiles("local")
class TelemetryHardeningMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void telemetrySnapshotsDedupKeysAndInsertTriggersExist() {
        Integer telemetryColumns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name IN ('environment_data', 'water_quality_data', 'light_data', 'pump_data')
                  AND column_name IN ('message_key', 'owner_user_id', 'plot_id_at_collection')
                """, Integer.class);
        Integer eventDedupColumns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name IN ('alert_event', 'notification')
                  AND column_name IN ('dedup_key', 'active_dedup_key')
                """, Integer.class);
        Integer snapshotTriggers = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.triggers
                WHERE trigger_schema = DATABASE()
                  AND trigger_name IN ('trg_ed_check_device_type_bi', 'trg_wq_check_device_type_bi',
                                       'trg_ld_check_device_type_bi', 'trg_pd_check_device_type_bi')
                  AND action_statement LIKE '%owner_user_id%'
                  AND action_statement LIKE '%plot_id_at_collection%'
                """, Integer.class);

        assertEquals(12, telemetryColumns);
        assertEquals(4, eventDedupColumns);
        assertEquals(4, snapshotTriggers);
    }
}
