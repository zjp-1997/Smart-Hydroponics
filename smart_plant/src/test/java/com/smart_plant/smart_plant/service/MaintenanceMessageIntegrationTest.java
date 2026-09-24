package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.entity.IotDeviceFault;
import com.smart_plant.smart_plant.dto.MaintenanceMessageRequest;
import com.smart_plant.smart_plant.entity.MaintenanceMessage;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** 在本地MySQL事务内验证故障到通知闭环，所有测试业务数据自动回滚。 */
@SpringBootTest
@Transactional
class MaintenanceMessageIntegrationTest {
    @Autowired MaintenanceMessageService messages;
    @Autowired IotDeviceFaultService faults;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserMapper users;
    @Autowired PlatformTransactionManager transactionManager;

    @AfterEach
    void clearContext() { CurrentUserContext.clear(); }

    /** 使用真实有效绑定作为测试前置，避免复制生产SQL作为期望结果。 */
    private IotDeviceFault createFault() {
        Long deviceId = jdbc.queryForObject("""
                SELECT d.id FROM iot_device d JOIN `user` u ON u.id=d.user_id
                JOIN role r ON r.id=u.role_id WHERE r.role_code='farm_owner'
                AND EXISTS(SELECT 1 FROM farm_owner_technician b WHERE b.owner_user_id=d.user_id AND b.status=1)
                ORDER BY d.id LIMIT 1
                """, Long.class);
        Long owner = jdbc.queryForObject("SELECT user_id FROM iot_device WHERE id=?", Long.class, deviceId);
        CurrentUserContext.set(users.selectById(owner));
        IotDeviceFault fault = new IotDeviceFault();
        fault.setDeviceId(deviceId);
        fault.setFaultCode("MAINT_TEST_" + System.nanoTime());
        fault.setFaultName("维护通知集成测试故障");
        fault.setFaultType(4);
        fault.setSeverity(3);
        fault.setFaultDesc("事务内测试：中文正文与地块查询");
        return faults.addFault(fault);
    }

    /** 新故障立即生成消息，重复发布不增加接收行；故障编辑不能改写已发布快照。 */
    @Test
    void createsOneSnapshotAndDeliversToOwnerAndBoundTechnician() {
        IotDeviceFault fault = createFault();
        Long id = jdbc.queryForObject("SELECT id FROM maintenance_message WHERE fault_id=?", Long.class, fault.getId());
        MaintenanceMessage message = messages.detail(id, true);
        assertEquals(fault.getId(), message.getFaultId());
        String expectedContent = message.getPlotName() + "的" + message.getDeviceName() + "发生故障，请技术人员或农场主及时处理";
        assertEquals(expectedContent, message.getContent());
        assertEquals("系统自动发布", message.getPublisherName());
        List<Long> recipients = jdbc.queryForList("SELECT user_id FROM notification WHERE ref_type='maintenance_message' AND ref_id=?", Long.class, id);
        assertTrue(recipients.contains(CurrentUserContext.get().getId()));
        assertTrue(recipients.contains(fault.getHandleUserId()));
        assertEquals(recipients.size(), recipients.stream().distinct().count());
        jdbc.update("UPDATE iot_device_fault SET fault_name='后续编辑' WHERE id=?", fault.getId());
        messages.publishForFault(fault.getId());
        assertEquals(message.getTitle(), messages.detail(id, true).getTitle());
        assertEquals(recipients.size(), jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE ref_type='maintenance_message' AND ref_id=?", Integer.class, id));
    }

    /** 技术人员可查询本人消息；已读统计只变更当前接收人，重复阅读保留第一次时间。 */
    @Test
    void readStatusAndStatisticsAreScopedToTheRecipient() {
        IotDeviceFault fault = createFault();
        Long owner = CurrentUserContext.get().getId();
        Long id = jdbc.queryForObject("SELECT id FROM maintenance_message WHERE fault_id=?", Long.class, fault.getId());
        CurrentUserContext.set(users.selectById(fault.getHandleUserId()));
        long before = ((Number) messages.statistics(true).get("readDeliveryCount")).longValue();
        messages.markRead(id);
        LocalDateTime first = jdbc.queryForObject("SELECT read_time FROM notification WHERE ref_type='maintenance_message' AND ref_id=? AND user_id=?", LocalDateTime.class, id, fault.getHandleUserId());
        messages.markRead(id);
        assertEquals(before + 1, ((Number) messages.statistics(true).get("readDeliveryCount")).longValue());
        assertEquals(first, jdbc.queryForObject("SELECT read_time FROM notification WHERE ref_type='maintenance_message' AND ref_id=? AND user_id=?", LocalDateTime.class, id, fault.getHandleUserId()));
        assertEquals(1, messages.detail(id, true).getReadCount());
        CurrentUserContext.set(users.selectById(owner));
        assertEquals(0, messages.detail(id, true).getReadCount());
        assertTrue(messages.list("维护通知集成测试", "发生故障，请技术人员或农场主及时处理", messages.detail(id, true).getPlotName(), 1, 10, true).getList().stream().anyMatch(m -> id.equals(m.getId())));
    }

    /** 任意其他用户即使知道消息ID，也不能查看详情或标记其他人的已读。 */
    @Test
    void rejectsCrossUserAccessAndMissingFault() {
        IotDeviceFault fault = createFault();
        Long id = jdbc.queryForObject("SELECT id FROM maintenance_message WHERE fault_id=?", Long.class, fault.getId());
        User stranger = new User(); stranger.setId(-1L); stranger.setRoleCode("farm_owner");
        CurrentUserContext.set(stranger);
        assertTrue(messages.list(null,null,null,1,10,true).getList().isEmpty());
        assertThrows(BusinessException.class, () -> messages.detail(id, true));
        assertThrows(BusinessException.class, () -> messages.markRead(id));
        assertThrows(BusinessException.class, () -> messages.publishForFault(-1L));
    }
    /** 发布环节失败必须回滚同一事务内新增的故障与消息，不能留下半成功的数据。 */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void publicationFailureRollsBackFaultAndDeliveryTogether() {
        long[] ids = new long[2];
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThrows(BusinessException.class, () -> tx.executeWithoutResult(status -> {
            IotDeviceFault fault = createFault();
            ids[0] = fault.getId();
            ids[1] = jdbc.queryForObject("SELECT id FROM maintenance_message WHERE fault_id=?", Long.class, fault.getId());
            messages.publishForFault(-1L);
        }));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_fault WHERE id=?", Integer.class, ids[0]));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM maintenance_message WHERE id=?", Integer.class, ids[1]));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE ref_type='maintenance_message' AND ref_id=?", Integer.class, ids[1]));
    }

    /** 表单提交标题、正文、设备和级别，发布人来自登录态。 */
    private MaintenanceMessageRequest request(Long deviceId, String title) {
        MaintenanceMessageRequest request = new MaintenanceMessageRequest();
        request.setDeviceId(deviceId);
        request.setTitle(title);
        request.setContent("人工输入的维护正文\n请保留换行及自定义说明。");
        request.setLevel(2);
        return request;
    }

    /** 人工消息不占用故障唯一键，保留自定义正文并同步通知两个接收角色。 */
    @Test
    void manuallyPublishesAndEditsAllDeliveriesWithoutLosingReadStatus() {
        IotDeviceFault fault = createFault();
        MaintenanceMessage manual = messages.publish(request(fault.getDeviceId(), "人工维护提醒"));
        assertNull(manual.getFaultId());
        assertEquals(CurrentUserContext.get().getId(), manual.getPublisherId());
        assertEquals("人工输入的维护正文\n请保留换行及自定义说明。", manual.getContent());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE ref_type='maintenance_message' AND ref_id=?", Integer.class, manual.getId()));
        messages.markRead(manual.getId());
        MaintenanceMessageRequest edit = request(manual.getDeviceId(), "编辑后的提醒");
        edit.setVersion(manual.getVersion());
        edit.setLevel(3);
        edit.setContent("已联系技术人员\n请明天上午检查设备。");
        MaintenanceMessage updated = messages.update(manual.getId(), edit);
        assertEquals(edit.getContent(), updated.getContent());
        assertEquals(edit.getContent(), messages.detail(manual.getId(), true).getContent());
        assertEquals(manual.getVersion() + 1, updated.getVersion());
        assertEquals(1, updated.getReadCount());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE ref_type='maintenance_message' AND ref_id=? AND title=? AND level=3 AND content=?", Integer.class, manual.getId(), edit.getTitle(), updated.getContent()));
        assertThrows(BusinessException.class, () -> messages.update(manual.getId(), edit));
    }

    /** 批量删除覆盖自动/人工消息，之后的重复发布不能复活已删除的自动通知。 */
    @Test
    void batchDeletionHidesMessagesAndDeliveriesAndDoesNotResurrect() {
        IotDeviceFault fault = createFault();
        Long autoId = jdbc.queryForObject("SELECT id FROM maintenance_message WHERE fault_id=?", Long.class, fault.getId());
        MaintenanceMessage manual = messages.publish(request(fault.getDeviceId(), "待批量删除提醒"));
        long before = ((Number) messages.statistics(false).get("totalCount")).longValue();
        assertEquals(2, messages.delete(List.of(autoId, manual.getId(), autoId)));
        assertEquals(before - 2, ((Number) messages.statistics(false).get("totalCount")).longValue());
        assertThrows(BusinessException.class, () -> messages.detail(autoId, true));
        assertThrows(BusinessException.class, () -> messages.markRead(manual.getId()));
        messages.publishForFault(fault.getId());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE ref_type='maintenance_message' AND ref_id IN (?,?) AND status=1", Integer.class, autoId, manual.getId()));
        assertEquals(0, jdbc.queryForObject("SELECT status FROM maintenance_message WHERE id=?", Integer.class, autoId));
    }

    /** 非接收人无法编辑、删除或向其他农场设备发布消息；错误批次不能部分删除。 */
    @Test
    void managementRejectsCrossUserAccessAndInvalidBatchBeforeMutation() {
        IotDeviceFault fault = createFault();
        Long id = jdbc.queryForObject("SELECT id FROM maintenance_message WHERE fault_id=?", Long.class, fault.getId());
        MaintenanceMessageRequest edit = request(fault.getDeviceId(), "越权编辑");
        edit.setVersion(0);
        assertThrows(BusinessException.class, () -> messages.delete(List.of(id, Long.MAX_VALUE)));
        assertNotNull(messages.detail(id, false));
        User stranger = new User(); stranger.setId(-1L); stranger.setRoleCode("farm_owner");
        CurrentUserContext.set(stranger);
        assertThrows(BusinessException.class, () -> messages.publish(edit));
        assertThrows(BusinessException.class, () -> messages.update(id, edit));
        assertThrows(BusinessException.class, () -> messages.delete(List.of(id)));
        assertEquals(1, jdbc.queryForObject("SELECT status FROM maintenance_message WHERE id=?", Integer.class, id));
    }

    /** 单条删除与批量删除共用事务服务，消息快照保留且不删除来源故障。 */
    @Test
    void singleDeletionPreservesOriginalFault() {
        IotDeviceFault fault = createFault();
        Long id = jdbc.queryForObject("SELECT id FROM maintenance_message WHERE fault_id=?", Long.class, fault.getId());
        assertEquals(1, messages.delete(List.of(id)));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_fault WHERE id=?", Integer.class, fault.getId()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE ref_type='maintenance_message' AND ref_id=? AND status=1", Integer.class, id));
    }

    /** 自动故障消息也能修改正文；重复自动发布不能覆盖人工修改和阅读状态。 */
    @Test
    void automaticMessageContentIsEditableAndSurvivesRepublish() {
        IotDeviceFault fault = createFault();
        Long id = jdbc.queryForObject("SELECT id FROM maintenance_message WHERE fault_id=?", Long.class, fault.getId());
        MaintenanceMessage original = messages.detail(id, false);
        MaintenanceMessageRequest edit = request(fault.getDeviceId(), original.getTitle());
        edit.setVersion(original.getVersion());
        edit.setContent("已报修，预计下午恢复。\n请勿重复启动设备。");
        messages.markRead(id);
        messages.update(id, edit);
        messages.publishForFault(fault.getId());
        assertEquals(edit.getContent(), messages.detail(id, false).getContent());
        assertEquals(1, messages.detail(id, false).getReadCount());
        assertTrue(jdbc.queryForList("SELECT content FROM notification WHERE ref_type='maintenance_message' AND ref_id=?", String.class, id).stream().allMatch(edit.getContent()::equals));
    }

    /** 空正文和超出TEXT安全范围的输入均在业务层拒绝，防止保存空消息或数据库截断。 */
    @Test
    void rejectsBlankAndOversizedContent() {
        IotDeviceFault fault = createFault();
        MaintenanceMessageRequest input = request(fault.getDeviceId(), "正文校验");
        input.setContent("  \n ");
        assertThrows(BusinessException.class, () -> messages.publish(input));
        input.setContent("字".repeat(10001));
        assertThrows(BusinessException.class, () -> messages.publish(input));
    }

}
