package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.FarmChatSendRequest;
import com.smart_plant.smart_plant.dto.FarmChatDetailResponse;
import com.smart_plant.smart_plant.entity.FarmChatMessage;
import com.smart_plant.smart_plant.entity.FarmChatSession;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.FarmChatMessageMapper;
import com.smart_plant.smart_plant.mapper.FarmChatSessionMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.websocket.ConsultWebSocketPushService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证聊天权限由服务端绑定关系决定，前端联系人ID不能绕过农场范围。 */
class FarmChatServiceImplTest {

    @Test
    void initialDetailReturnsLatestPageInChronologicalOrder() {
        FarmChatSessionMapper sessionMapper = mock(FarmChatSessionMapper.class);
        FarmChatMessageMapper messageMapper = mock(FarmChatMessageMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        DataPermissionService permissions = mock(DataPermissionService.class);
        FarmChatServiceImpl service = new FarmChatServiceImpl(sessionMapper, messageMapper, userMapper,
                permissions, mock(ConsultAttachmentService.class), mock(ConsultWebSocketPushService.class));

        User owner = user(1L, "farm_owner");
        User member = user(9L, "user");
        FarmChatSession session = new FarmChatSession();
        session.setId(23L);
        session.setFarmOwnerId(1L);
        session.setMemberUserId(9L);
        session.setMemberRole("user");
        when(permissions.currentUser()).thenReturn(owner);
        when(sessionMapper.selectById(23L)).thenReturn(session);
        when(userMapper.selectById(9L)).thenReturn(member);
        // SQL 按 ID 倒序多取一条，服务层去掉探测行后恢复为页面展示顺序。
        when(messageMapper.selectCursor(23L, null, null, 3))
                .thenReturn(List.of(message(5L), message(4L), message(3L)));

        FarmChatDetailResponse result = service.getDetail(23L, null, null, null, 2);

        assertEquals(List.of(4L, 5L), result.getMessages().stream()
                .map(item -> item.getMessageId()).toList());
        assertTrue(result.isHasMoreBefore());
        verify(messageMapper).markReceivedMessagesRead(23L, 1L);
    }

    @Test
    void ownerCannotSendMessageToUnboundUser() {
        FarmChatSessionMapper sessionMapper = mock(FarmChatSessionMapper.class);
        FarmChatMessageMapper messageMapper = mock(FarmChatMessageMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        DataPermissionService permissions = mock(DataPermissionService.class);
        ConsultAttachmentService attachmentService = mock(ConsultAttachmentService.class);
        ConsultWebSocketPushService pushService = mock(ConsultWebSocketPushService.class);
        FarmChatServiceImpl service = new FarmChatServiceImpl(sessionMapper, messageMapper, userMapper,
                permissions, attachmentService, pushService);

        User owner = user(1L, "farm_owner");
        User unrelatedUser = user(9L, "user");
        when(permissions.currentUser()).thenReturn(owner);
        when(userMapper.selectById(9L)).thenReturn(unrelatedUser);
        when(userMapper.countBoundUserByOwnerId(1L, 9L)).thenReturn(0);
        FarmChatSendRequest request = new FarmChatSendRequest();
        request.setPeerUserId(9L);
        request.setContent("越权消息");

        assertThrows(BusinessException.class, () -> service.send(request));
        // 权限失败必须发生在创建会话和写消息之前。
        verify(sessionMapper, never()).insert(org.mockito.ArgumentMatchers.any());
        verify(messageMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    private User user(Long id, String roleCode) {
        User user = new User();
        user.setId(id);
        user.setRoleCode(roleCode);
        user.setStatus(1);
        user.setUsername(roleCode + id);
        return user;
    }

    private FarmChatMessage message(Long id) {
        FarmChatMessage message = new FarmChatMessage();
        message.setId(id);
        message.setSenderId(9L);
        message.setReceiverId(1L);
        message.setMessageType(1);
        message.setContent("消息" + id);
        return message;
    }
}
