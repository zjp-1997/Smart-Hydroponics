package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.ConsultMessage;
import com.smart_plant.smart_plant.dto.ClientExpertChatRequest;
import com.smart_plant.smart_plant.entity.ConsultSession;
import com.smart_plant.smart_plant.entity.ExpertProfile;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.ConsultMessageMapper;
import com.smart_plant.smart_plant.mapper.ConsultSessionMapper;
import com.smart_plant.smart_plant.mapper.ExpertProfileMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.websocket.ConsultWebSocketPushService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证专家工作台只处理绑定到本人专家档案的咨询。 */
class ExpertWorkspaceServiceTest {
    private final ConsultSessionMapper sessions = mock(ConsultSessionMapper.class);
    private final ConsultMessageMapper messages = mock(ConsultMessageMapper.class);
    private final ExpertProfileMapper profiles = mock(ExpertProfileMapper.class);
    private final DataPermissionService permission = mock(DataPermissionService.class);
    private final ConsultAttachmentService attachments = mock(ConsultAttachmentService.class);
    private final ConsultationServiceImpl service = new ConsultationServiceImpl(sessions, messages, profiles,
            permission, mock(ConsultWebSocketPushService.class), attachments);

    private void expertLogin() {
        User user = new User();
        user.setId(7L);
        user.setRoleCode("expert");
        ExpertProfile profile = new ExpertProfile();
        profile.setId(11L);
        profile.setUserId(7L);
        profile.setStatus(1);
        when(permission.currentUser()).thenReturn(user);
        when(profiles.selectByUserId(7L)).thenReturn(profile);
    }

    @Test
    void expertCannotCreateAClientSideConsultation() {
        expertLogin();
        // 专家发送必须走本人会话回复接口，不能冒用咨询发起人流程。
        assertThrows(BusinessException.class, () -> service.sendClientMessage(new ClientExpertChatRequest()));
        verify(messages, never()).insert(any());
    }

    @Test
    void cannotReadOrReplyToAnotherExpertsSession() {
        expertLogin();
        ConsultSession foreign = new ConsultSession();
        foreign.setId(23L);
        foreign.setExpertId(12L);
        foreign.setExpertUserId(8L);
        foreign.setStatus(1);
        when(sessions.selectById(23L)).thenReturn(foreign);

        // 详情和回复复用同一归属校验，均不得跨专家访问。
        assertThrows(BusinessException.class, () -> service.getExpertChatDetail(23L, null, null, 30));
        assertThrows(BusinessException.class, () -> service.replyAsExpert(23L, "诊断建议"));
        verify(messages, never()).insert(any());
    }

    @Test
    void replyUsesExpertAccountAndSessionOwner() {
        expertLogin();
        ConsultSession own = new ConsultSession();
        own.setId(23L);
        own.setExpertId(11L);
        own.setExpertUserId(7L);
        own.setUserId(31L);
        own.setStatus(1);
        when(sessions.selectById(23L)).thenReturn(own);

        service.replyAsExpert(23L, "  建议检查叶片背面  ");

        // 发件人是专家账号，收件人只能是该会话的咨询发起人。
        ArgumentCaptor<ConsultMessage> saved = ArgumentCaptor.forClass(ConsultMessage.class);
        verify(messages).insert(saved.capture());
        assertEquals(7L, saved.getValue().getSenderId());
        assertEquals(31L, saved.getValue().getReceiverId());
        assertEquals("建议检查叶片背面", saved.getValue().getContent());
        verify(sessions).updateLastMessage(23L, "建议检查叶片背面", false);
    }

    @Test
    void imageReplyUsesOwnUploadAndKeepsMessageType() {
        expertLogin();
        ConsultSession own = new ConsultSession();
        own.setId(23L);
        own.setExpertId(11L);
        own.setExpertUserId(7L);
        own.setUserId(31L);
        own.setStatus(1);
        when(sessions.selectById(23L)).thenReturn(own);

        String mediaUrl = "/uploads/consult-chat/7/image/example.jpg";
        service.replyAsExpert(23L, "[图片]", 2, mediaUrl);

        // 图片地址必须经过上传归属校验，且落库和会话未读更新仍走原有链路。
        verify(attachments).requireOwned(mediaUrl, 2);
        ArgumentCaptor<ConsultMessage> saved = ArgumentCaptor.forClass(ConsultMessage.class);
        verify(messages).insert(saved.capture());
        assertEquals(2, saved.getValue().getMessageType());
        assertEquals(mediaUrl, saved.getValue().getMediaUrl());
        assertEquals(31L, saved.getValue().getReceiverId());
    }

    @Test
    void openingOwnSessionMarksOnlyExpertReceivedMessagesRead() {
        expertLogin();
        ConsultSession own = new ConsultSession();
        own.setId(23L);
        own.setExpertId(11L);
        own.setExpertUserId(7L);
        own.setUserId(31L);
        own.setStatus(1);
        when(sessions.selectById(23L)).thenReturn(own);
        when(messages.selectExpertMessagesBySessionId(23L, 7L, null, null, 31)).thenReturn(List.of());

        service.getExpertChatDetail(23L, null, null, 30);

        // 已读操作同时受会话 ID 和专家账号 ID 约束，不触碰咨询用户侧状态。
        verify(messages).markExpertMessagesRead(23L, 7L);
        verify(sessions).resetExpertUnreadCount(23L, 7L);
    }
}
