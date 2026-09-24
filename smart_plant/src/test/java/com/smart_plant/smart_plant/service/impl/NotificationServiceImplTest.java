package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.FarmMessageRequest;
import com.smart_plant.smart_plant.entity.FarmTask;
import com.smart_plant.smart_plant.entity.Notification;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.NotificationMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.FarmTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {

    private final NotificationMapper notificationMapper = mock(NotificationMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);
    private final FarmTaskService farmTaskService = mock(FarmTaskService.class);
    private final NotificationServiceImpl service = new NotificationServiceImpl(
            notificationMapper, userMapper, dataPermissionService, farmTaskService);

    @Test
    void clientAnnouncementReadUsesCurrentRecipientAndRejectsOtherUsersMessage() {
        User currentUser = new User();
        currentUser.setId(3L);
        when(dataPermissionService.currentUser()).thenReturn(currentUser);

        // 更新必须带当前登录用户 ID；更新不到的其他用户公告不能被当作已读成功。
        Notification otherUsersAnnouncement = new Notification();
        otherUsersAnnouncement.setId(42L);
        otherUsersAnnouncement.setUserId(4L);
        otherUsersAnnouncement.setNoticeType(1);
        otherUsersAnnouncement.setStatus(1);
        when(notificationMapper.selectById(42L)).thenReturn(otherUsersAnnouncement);

        assertThatThrownBy(() -> service.markClientAnnouncementRead(42L))
                .isInstanceOf(BusinessException.class);
        verify(notificationMapper).markClientAnnouncementRead(org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.eq(3L), any());
    }

    @Test
    void clientAnnouncementStatisticsAreScopedToCurrentUser() {
        User currentUser = new User();
        currentUser.setId(3L);
        when(dataPermissionService.currentUser()).thenReturn(currentUser);
        when(notificationMapper.countClientUnreadAnnouncements(3L)).thenReturn(2L);

        // 未读总数由当前用户的明细计算，和后台公告全局统计分离。
        assertThat(service.countClientUnreadAnnouncements()).isEqualTo(2L);
        // 统计前先补历史公告，消息首页可直接得到正确未读数。
        verify(notificationMapper).insertMissingClientAnnouncementDeliveries(3L);
        verify(notificationMapper).countClientUnreadAnnouncements(3L);
    }

    @Test
    void existingAutomaticMessageIsNeverOverwrittenByRegeneration() {
        Notification edited = message(36L, 13L, "用户修改后的标题", "用户修改后的内容");
        edited.setRemark("用户已编辑：原自动农事消息");
        when(notificationMapper.selectByRef(3L, 2, "farm_task_message", 13L)).thenReturn(edited);

        Notification regenerated = message(null, 13L, "系统默认标题", "系统默认内容");
        regenerated.setUserId(3L);
        regenerated.setNoticeType(2);
        regenerated.setRefType("farm_task_message");
        regenerated.setRemark("农事任务产生后自动生成");

        Notification result = service.createIfAbsent(regenerated);

        assertThat(result.getTitle()).isEqualTo("用户修改后的标题");
        assertThat(result.getContent()).isEqualTo("用户修改后的内容");
        verify(notificationMapper, never()).insert(regenerated);
    }

    @Test
    void concurrentAutomaticMessageReturnsTheRowThatWonTheUniqueKeyRace() {
        Notification request = message(null, 13L, "系统默认标题", "系统默认内容");
        request.setUserId(3L);
        Notification winner = message(36L, 13L, "已创建", "已创建内容");
        winner.setUserId(3L);
        when(notificationMapper.selectByRef(3L, 2, "farm_task_message", 13L)).thenReturn(null);
        when(notificationMapper.selectByDedupKeyForUpdate("notification:3:2:farm_task_message:13"))
                .thenReturn(winner);
        doThrow(new DuplicateKeyException("duplicate")).when(notificationMapper).insert(request);

        assertThat(service.createIfAbsent(request)).isSameAs(winner);
        assertThat(request.getDedupKey())
                .isEqualTo("notification:3:2:farm_task_message:13");
        verify(notificationMapper).insert(request);
    }

    @Test
    void editsTitleAndContentForTheWholeMessageGroup() {
        Notification oldMessage = message(36L, 13L, "旧标题", "旧内容");
        Notification updatedMessage = message(36L, 13L, "自定义设备异常标题", "自定义处理内容");
        when(notificationMapper.selectFarmMessageGroupById(36L)).thenReturn(oldMessage);
        when(notificationMapper.selectFarmMessageGroupById(13L)).thenReturn(updatedMessage);
        when(notificationMapper.updateFarmMessageGroup(
                13L, 17L, "自定义设备异常标题", "自定义处理内容", 2)).thenReturn(2);
        when(farmTaskService.getFarmTaskById(17L)).thenReturn(new FarmTask());

        FarmMessageRequest request = new FarmMessageRequest();
        request.setTaskId(17L);
        request.setTitle("  自定义设备异常标题  ");
        request.setContent("  自定义处理内容  ");
        request.setLevel(2);

        Notification result = service.updateFarmMessage(36L, request);

        verify(notificationMapper).updateFarmMessageGroup(
                13L, 17L, "自定义设备异常标题", "自定义处理内容", 2);
        assertThat(result.getTitle()).isEqualTo("自定义设备异常标题");
        assertThat(result.getContent()).isEqualTo("自定义处理内容");
    }

    private Notification message(Long id, Long refId, String title, String content) {
        Notification message = new Notification();
        message.setId(id);
        message.setRefId(refId);
        message.setTaskId(17L);
        message.setTitle(title);
        message.setContent(content);
        message.setNoticeType(2);
        message.setRefType("farm_task_message");
        message.setLevel(2);
        message.setStatus(1);
        return message;
    }
}
