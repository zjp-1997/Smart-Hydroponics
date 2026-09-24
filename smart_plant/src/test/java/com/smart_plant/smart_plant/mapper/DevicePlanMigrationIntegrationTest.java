package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.SmartPlantApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 只读校验本地 smart_plant 数据库的设备计划迁移结果。 */
@SpringBootTest(classes = SmartPlantApplication.class)
@ActiveProfiles("local")
class DevicePlanMigrationIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** V8应创建计划、运行时段和指令审计三张表。 */
    @Test
    void iotPlanTablesExist() {
        assertEquals(3, countTables("iot_device_plan", "iot_device_schedule", "iot_device_command_record"));
    }

    /** V9应补齐摄像头计划管理和硬件应用状态字段。 */
    @Test
    void cameraPlanColumnsAndPermissionsExist() {
        Integer columns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'camera_capture_plan'
                  AND column_name IN ('plan_name', 'weekdays_mask', 'timezone', 'apply_status', 'version')
                """, Integer.class);
        Integer permissions = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM permission
                WHERE permission_code IN ('iot_device_plan:manage', 'camera_capture_plan:manage')
                """, Integer.class);
        assertEquals(5, columns);
        assertEquals(2, permissions);
    }

    /** 使用参数占位符读取information_schema，避免测试依赖固定数据库名称。 */
    private int countTables(String first, String second, String third) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name IN (?, ?, ?)
                """, Integer.class, first, second, third);
        return count == null ? 0 : count;
    }
}
