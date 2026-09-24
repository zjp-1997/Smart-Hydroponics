package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.ConsultAttachmentResponse;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.ConsultMessageMapper;
import com.smart_plant.smart_plant.mapper.FarmChatMessageMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

/** 验证聊天附件上传后只可由上传账号引用，伪装的图片不得落盘。 */
class ConsultAttachmentServiceTest {
    @TempDir Path tempDirectory;

    @Test
    void imageUploadChecksSignatureAndOwner() {
        String previousDirectory = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDirectory.toString());
        try {
            DataPermissionService permission = mock(DataPermissionService.class);
            User owner = new User();
            owner.setId(7L);
            when(permission.currentUser()).thenReturn(owner);
            ConsultAttachmentService service = new ConsultAttachmentService(permission,
                    mock(ConsultMessageMapper.class), mock(FarmChatMessageMapper.class));

            // 最小 PNG 头用于验证上传地址、文件存在和用户归属，不依赖外部图片。
            byte[] pngHeader = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
            ConsultAttachmentResponse uploaded = service.upload(
                    new MockMultipartFile("file", "leaf.png", "image/png", pngHeader), "image");
            assertTrue(uploaded.mediaUrl().startsWith("/uploads/consult-chat/7/image/"));
            service.requireOwned(uploaded.mediaUrl(), 2);
            String fileName = uploaded.mediaUrl().substring(uploaded.mediaUrl().lastIndexOf('/') + 1);
            assertTrue(service.resolveForDownload(7L, "image", fileName).toFile().isFile());

            User other = new User();
            other.setId(8L);
            when(permission.currentUser()).thenReturn(other);
            assertThrows(BusinessException.class, () -> service.requireOwned(uploaded.mediaUrl(), 2));
            assertThrows(BusinessException.class, () -> service.upload(
                    new MockMultipartFile("file", "fake.png", "image/png", "<html>".getBytes()), "image"));
        } finally {
            System.setProperty("user.dir", previousDirectory);
        }
    }

    @Test
    void documentUploadKeepsDisplayNameAndCannotBeSentAsImage() {
        String previousDirectory = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDirectory.toString());
        try {
            DataPermissionService permission = mock(DataPermissionService.class);
            User owner = new User();
            owner.setId(7L);
            when(permission.currentUser()).thenReturn(owner);
            ConsultAttachmentService service = new ConsultAttachmentService(permission,
                    mock(ConsultMessageMapper.class), mock(FarmChatMessageMapper.class));

            // App 沙箱副本的文件名与原名不同，展示名仍应使用选择时的文件名。
            ConsultAttachmentResponse uploaded = service.upload(
                    new MockMultipartFile("file", "chat-copy.pdf", "application/pdf", "%PDF".getBytes()),
                    "file", "病虫害报告.pdf");
            assertEquals("病虫害报告.pdf", uploaded.fileName());
            service.requireOwned(uploaded.mediaUrl(), 4);
            assertThrows(BusinessException.class, () -> service.requireOwned(uploaded.mediaUrl(), 2));
        } finally {
            System.setProperty("user.dir", previousDirectory);
        }
    }

    @Test
    void downloadRequiresCurrentConversationAccessAndRevokesFormerMember() {
        String previousDirectory = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDirectory.toString());
        try {
            DataPermissionService permission = mock(DataPermissionService.class);
            ConsultMessageMapper consultMessages = mock(ConsultMessageMapper.class);
            FarmChatMessageMapper farmMessages = mock(FarmChatMessageMapper.class);
            ConsultAttachmentService service = new ConsultAttachmentService(
                    permission, consultMessages, farmMessages);
            User uploader = new User();
            uploader.setId(7L);
            when(permission.currentUser()).thenReturn(uploader);
            ConsultAttachmentResponse uploaded = service.upload(
                    new MockMultipartFile("file", "leaf.png", "image/png",
                            new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10}), "image");
            String fileName = uploaded.mediaUrl().substring(uploaded.mediaUrl().lastIndexOf('/') + 1);

            User participant = new User();
            participant.setId(8L);
            when(permission.currentUser()).thenReturn(participant);
            when(consultMessages.countMediaReferences(anyString())).thenReturn(1);
            when(consultMessages.countAccessibleMedia(uploaded.mediaUrl(), 8L, false)).thenReturn(1);
            assertTrue(service.resolveForDownload(7L, "image", fileName).toFile().isFile());

            when(consultMessages.countAccessibleMedia(uploaded.mediaUrl(), 8L, false)).thenReturn(0);
            assertThrows(BusinessException.class,
                    () -> service.resolveForDownload(7L, "image", fileName));

            // 文件一旦进入农场聊天，即使上传者仍可登录，也必须保有当前成员关系。
            when(permission.currentUser()).thenReturn(uploader);
            when(consultMessages.countMediaReferences(anyString())).thenReturn(0);
            when(farmMessages.countMediaReferences(uploaded.mediaUrl())).thenReturn(1);
            when(farmMessages.countAccessibleMedia(uploaded.mediaUrl(), 7L, false)).thenReturn(0);
            assertThrows(BusinessException.class,
                    () -> service.resolveForDownload(7L, "image", fileName));
        } finally {
            System.setProperty("user.dir", previousDirectory);
        }
    }
}
