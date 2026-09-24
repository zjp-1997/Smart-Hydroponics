package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.CameraCapturePlan;
import com.smart_plant.smart_plant.entity.IotDevicePlan;
import com.smart_plant.smart_plant.entity.IotDeviceSchedule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 设备计划 Mapper 的真实 MySQL 回归测试。
 * 测试运行在事务中，结束后自动回滚，不会向本地业务库遗留调试数据。
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class DevicePlanMapperIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IotDevicePlanMapper iotPlanMapper;

    @Autowired
    private CameraCapturePlanMapper cameraPlanMapper;

    /** 验证 IoT 计划、运行时段、乐观锁更新以及外键级联删除的完整 SQL 链路。 */
    @Test
    void iotPlanCrudRoundTrip() {
        Long deviceId = jdbcTemplate.queryForObject("""
                SELECT d.id FROM iot_device d
                JOIN device_type dt ON dt.id = d.type_id
                LEFT JOIN iot_device_plan dp ON dp.device_id = d.id
                WHERE dt.type_code IN ('GROW_LIGHT', 'WATER_PUMP') AND dp.id IS NULL
                ORDER BY d.id LIMIT 1
                """, Long.class);
        assertNotNull(deviceId, "本地测试库至少需要一台尚未配置计划的补光灯或水泵");

        IotDevicePlan plan = new IotDevicePlan();
        plan.setDeviceId(deviceId);
        plan.setCollectionEnabled(1);
        plan.setCollectionIntervalMinutes(30);
        plan.setControlEnabled(1);
        plan.setTimezone("Asia/Shanghai");
        plan.setCreateBy(1L);
        plan.setUpdateBy(1L);
        assertEquals(1, iotPlanMapper.insertPlan(plan));
        assertNotNull(plan.getId());

        IotDeviceSchedule schedule = new IotDeviceSchedule();
        schedule.setPlanId(plan.getId());
        schedule.setScheduleName("集成测试时段");
        schedule.setWeekdaysMask(127);
        schedule.setStartTime(LocalTime.of(6, 0));
        schedule.setEndTime(LocalTime.of(18, 0));
        schedule.setEnabled(1);
        schedule.setSortOrder(0);
        assertEquals(1, iotPlanMapper.insertSchedules(List.of(schedule)));

        IotDevicePlan saved = iotPlanMapper.selectByDeviceId(deviceId);
        assertEquals(30, saved.getCollectionIntervalMinutes());
        assertEquals(0, saved.getApplyStatus());
        assertEquals(1, iotPlanMapper.selectSchedulesByPlanIds(List.of(plan.getId())).size());

        plan.setCollectionIntervalMinutes(45);
        plan.setVersion(0);
        assertEquals(1, iotPlanMapper.updatePlan(plan));
        IotDevicePlan updated = iotPlanMapper.selectByDeviceId(deviceId);
        assertEquals(45, updated.getCollectionIntervalMinutes());
        assertEquals(1, updated.getVersion());

        assertEquals(1, iotPlanMapper.deletePlanByDeviceId(deviceId));
        assertNull(iotPlanMapper.selectByDeviceId(deviceId).getId());
        assertTrue(iotPlanMapper.selectSchedulesByPlanIds(List.of(plan.getId())).isEmpty());
    }

    /** 验证摄像头计划新增、联表展示、待下发重置、版本更新与删除链路。 */
    @Test
    void cameraPlanCrudRoundTrip() {
        CameraOwner camera = jdbcTemplate.queryForObject("""
                SELECT c.id, p.user_id, c.plot_id
                FROM camera_device c JOIN plot p ON p.id = c.plot_id
                ORDER BY c.id LIMIT 1
                """, (resultSet, rowNum) -> new CameraOwner(
                resultSet.getLong("id"), resultSet.getLong("user_id"), resultSet.getLong("plot_id")));
        assertNotNull(camera, "本地测试库至少需要一台摄像头");

        CameraCapturePlan plan = new CameraCapturePlan();
        plan.setDeviceId(camera.cameraId());
        plan.setUserId(camera.userId());
        plan.setPlotId(camera.plotId());
        plan.setPlanName("集成测试图像采集");
        plan.setIntervalMinutes(60);
        plan.setWeekdaysMask(127);
        plan.setStartTime(LocalTime.of(6, 0));
        plan.setEndTime(LocalTime.of(18, 0));
        plan.setTimezone("Asia/Shanghai");
        plan.setEnabled(1);
        plan.setNextCaptureTime(LocalDateTime.now().plusHours(1).withSecond(0).withNano(0));
        plan.setRemark("事务回滚测试");
        plan.setCreateBy(1L);
        plan.setUpdateBy(1L);
        assertEquals(1, cameraPlanMapper.insert(plan));
        assertNotNull(plan.getId());

        CameraCapturePlan saved = cameraPlanMapper.selectById(plan.getId());
        assertEquals("集成测试图像采集", saved.getPlanName());
        assertNotNull(saved.getCameraName());
        assertNotNull(saved.getPlotName());
        assertEquals(0, saved.getApplyStatus());

        plan.setPlanName("集成测试图像采集-已更新");
        plan.setIntervalMinutes(120);
        plan.setVersion(0);
        assertEquals(1, cameraPlanMapper.update(plan));
        CameraCapturePlan updated = cameraPlanMapper.selectById(plan.getId());
        assertEquals(120, updated.getIntervalMinutes());
        assertEquals(1, updated.getVersion());
        assertEquals(1, cameraPlanMapper.selectList(null, camera.plotId(), "已更新", 1).size());

        assertEquals(1, cameraPlanMapper.deleteById(plan.getId()));
        assertNull(cameraPlanMapper.selectById(plan.getId()));
    }

    /** 承载测试所需的摄像头归属字段，避免测试依赖完整业务实体。 */
    private record CameraOwner(Long cameraId, Long userId, Long plotId) { }
}
