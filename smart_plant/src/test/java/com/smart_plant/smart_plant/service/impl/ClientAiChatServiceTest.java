package com.smart_plant.smart_plant.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_plant.smart_plant.dto.ClientAiChatRequest;
import com.smart_plant.smart_plant.dto.ClientAiChatResponse;
import com.smart_plant.smart_plant.entity.AiChat;
import com.smart_plant.smart_plant.entity.AiModel;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.mapper.AiChatMapper;
import com.smart_plant.smart_plant.mapper.AiModelMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证 farm AI 咨询实际向模型发送图片内容和文档文本，而不只传本地附件地址。 */
class ClientAiChatServiceTest {
    @TempDir Path tempDir;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<JsonNode> providerRequest = new AtomicReference<>();
    private HttpServer server;
    private AiChatServiceImpl service;
    private AiChatMapper chatMapper;
    private ConsultAttachmentService attachmentService;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            providerRequest.set(mapper.readTree(exchange.getRequestBody()));
            byte[] response = "{\"choices\":[{\"message\":{\"content\":\"农业建议\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        chatMapper = mock(AiChatMapper.class);
        AiModelMapper modelMapper = mock(AiModelMapper.class);
        DataPermissionService permissions = mock(DataPermissionService.class);
        attachmentService = mock(ConsultAttachmentService.class);
        AiModel model = new AiModel();
        model.setId(7L);
        model.setModelName("DeepSeek");
        model.setModel("deepseek-chat");
        model.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        model.setApiKey("test-key");
        model.setStatus(1);
        when(modelMapper.selectFirstEnabledDeepSeek()).thenReturn(model);
        User user = new User();
        user.setId(3L);
        when(permissions.currentUser()).thenReturn(user);
        when(chatMapper.insert(any(AiChat.class))).thenAnswer(call -> {
            call.getArgument(0, AiChat.class).setId(11L);
            return 1;
        });
        service = new AiChatServiceImpl(chatMapper, modelMapper, permissions, mapper, attachmentService);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void imageIsSentAsDeepSeekVisionContent() throws Exception {
        Path image = tempDir.resolve("photo.jpg");
        Files.write(image, new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
        when(attachmentService.resolveOwnedPath(eq("/uploads/consult-chat/3/image/photo.jpg"), eq(2))).thenReturn(image);
        ClientAiChatRequest request = new ClientAiChatRequest();
        request.setImageUrl("/uploads/consult-chat/3/image/photo.jpg");

        ClientAiChatResponse response = service.sendClientMessage(request);

        JsonNode body = providerRequest.get();
        assertEquals("deepseek-flash", body.path("model").asText());
        assertTrue(body.path("messages").get(1).path("content").get(1)
                .path("image_url").path("url").asText().startsWith("data:image/jpeg;base64,"));
        assertEquals("农业建议", response.getAiContent());
    }

    @Test
    void documentTextReachesDeepSeekAndFileRemainsInResponse() throws Exception {
        Path document = tempDir.resolve("farm.txt");
        Files.writeString(document, "番茄叶片发黄，温室湿度偏高。", StandardCharsets.UTF_8);
        String url = "/uploads/consult-chat/3/file/farm.txt";
        when(attachmentService.resolveOwnedPath(eq(url), eq(4))).thenReturn(document);
        ClientAiChatRequest request = new ClientAiChatRequest();
        request.setFileUrl(url);
        request.setFileName("农场记录.txt");

        ClientAiChatResponse response = service.sendClientMessage(request);

        assertTrue(providerRequest.get().path("messages").get(1).path("content").asText().contains("番茄叶片发黄"));
        assertEquals(url, response.getFileUrl());
        assertEquals("农场记录.txt", response.getFileName());
        verify(chatMapper).insert(argThat(chat -> "农场记录.txt".equals(chat.getFileName())));
    }

    @Test
    void historyIsScopedToCurrentUserAndRestoresAttachmentsInChronologicalOrder() {
        // Mapper 返回新到旧的记录；服务层应恢复为聊天时间顺序并区分文件和图片。
        AiChat newerFile = new AiChat();
        newerFile.setId(12L);
        newerFile.setImageUrl("/uploads/consult-chat/3/file/record.txt");
        newerFile.setFileName("农场原始记录.txt");
        newerFile.setUserContent("请阅读附件");
        newerFile.setAiContent("文件建议");
        AiChat olderImage = new AiChat();
        olderImage.setId(11L);
        olderImage.setImageUrl("/uploads/consult-chat/3/image/photo.jpg");
        olderImage.setUserContent("请分析图片");
        olderImage.setAiContent("图片建议");
        when(chatMapper.selectClientHistory(3L, 13L, 50)).thenReturn(List.of(newerFile, olderImage));

        List<ClientAiChatResponse> history = service.listClientHistory(13L, null);

        verify(chatMapper).selectClientHistory(3L, 13L, 50);
        assertEquals(List.of(11L, 12L), history.stream().map(ClientAiChatResponse::getChatId).toList());
        assertEquals(olderImage.getImageUrl(), history.get(0).getImageUrl());
        assertEquals(newerFile.getImageUrl(), history.get(1).getFileUrl());
        assertEquals("农场原始记录.txt", history.get(1).getFileName());
    }
}
