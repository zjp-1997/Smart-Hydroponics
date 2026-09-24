package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.SystemSetting;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 在本地smart_plant实际结构上验证V6迁移及系统设置Mapper。 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class SystemSettingMapperIntegrationTest {

    @Autowired
    private SystemSettingMapper systemSettingMapper;

    @Test
    void readsSingletonSettingCreatedByMigration() {
        SystemSetting setting = systemSettingMapper.selectSingleton();

        assertNotNull(setting);
        assertEquals(1, setting.getId());
        assertTrue(setting.getDefaultPageSize() >= 5);
        assertTrue(setting.getMaxUploadSizeMb() >= 1);
    }
}
