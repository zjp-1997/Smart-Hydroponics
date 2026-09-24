package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.SmartPlantApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = SmartPlantApplication.class)
@ActiveProfiles("local")
class MapOverviewPermissionMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void mapOverviewMenuExistsAndPreservesPlotRoleAccess() {
        Map<String, Object> menu = jdbcTemplate.queryForMap("""
                SELECT permission_name, path, component, status
                FROM permission
                WHERE permission_code = 'map:view'
                """);
        Long rolesMissingMapPermission = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM role_permission plot_rp
                JOIN permission plot_permission
                  ON plot_permission.id = plot_rp.permission_id
                 AND plot_permission.permission_code = 'plot:manage'
                LEFT JOIN role_permission map_rp
                  ON map_rp.role_id = plot_rp.role_id
                 AND map_rp.permission_id = (SELECT id FROM permission WHERE permission_code = 'map:view')
                WHERE map_rp.role_id IS NULL
                """, Long.class);

        assertEquals("地图总览", menu.get("permission_name"));
        assertEquals("/map/overview", menu.get("path"));
        assertEquals("map/MapOverview", menu.get("component"));
        assertEquals(1, ((Number) menu.get("status")).intValue());
        assertEquals(0L, rolesMissingMapPermission);
    }
}
