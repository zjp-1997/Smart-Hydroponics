package com.smart_plant.smart_plant.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.ClientAiChatRequest;
import com.smart_plant.smart_plant.dto.ClientAiChatResponse;
import com.smart_plant.smart_plant.entity.AiChat;
import com.smart_plant.smart_plant.entity.AiModel;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.AiChatMapper;
import com.smart_plant.smart_plant.mapper.AiModelMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.AiChatService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

/**
 * AI对话业务实现。
 *
 * <p>后台管理复用同一套查询和删除逻辑；farm 用户端发送消息时会调用启用模型并保存审计记录。</p>
 */
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private static final int MODEL_STATUS_ENABLED = 1;
    private static final int CHAT_STATUS_SUCCESS = 1;
    private static final int CHAT_STATUS_FAILED = 2;
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int MAX_IMAGE_URL_LENGTH = 500;
    private static final int MAX_DOCUMENT_TEXT_LENGTH = 16000;
    private static final Duration MODEL_TIMEOUT = Duration.ofSeconds(60);

    private final AiChatMapper aiChatMapper;
    private final AiModelMapper aiModelMapper;
    private final DataPermissionService dataPermissionService;
    private final ObjectMapper objectMapper;
    private final ConsultAttachmentService attachmentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChat(Long id) {
        AiChat oldChat = getChatById(id);
        int rows = aiChatMapper.deleteById(oldChat.getId());
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "AI对话删除失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteChats(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的AI对话");
        }
        for (Long id : ids) {
            getChatById(id);
        }
        return aiChatMapper.deleteBatchByIds(ids);
    }

    @Override
    public AiChat getChatById(Long id) {
        requireId(id);
        AiChat chat = aiChatMapper.selectById(id);
        if (chat == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "AI对话不存在");
        }
        dataPermissionService.requireOwnedResource(chat.getUserId());
        return chat;
    }

    @Override
    public PageInfo<AiChat> listChats(String username, String modelName, Integer status,
                                      LocalDate startDate, LocalDate endDate,
                                      Integer pageNum, Integer pageSize) {
        QueryParams params = normalizeQueryParams(username, modelName, status, startDate, endDate);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(aiChatMapper.selectList(
                scopedUserId,
                params.username(),
                params.modelName(),
                params.status(),
                params.startTime(),
                params.endTime()));
    }

    @Override
    public Map<String, Object> statisticsChats(String username, String modelName, Integer status,
                                               LocalDate startDate, LocalDate endDate) {
        QueryParams params = normalizeQueryParams(username, modelName, status, startDate, endDate);
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        Map<String, Object> statistics = aiChatMapper.selectStatistics(
                scopedUserId,
                params.username(),
                params.modelName(),
                params.status(),
                params.startTime(),
                params.endTime());
        return statistics == null ? new HashMap<>() : statistics;
    }

    @Override
    public List<ClientAiChatResponse> listClientHistory(Long beforeId, Integer pageSize) {
        if (beforeId != null && beforeId <= 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "历史消息游标不合法");
        }
        int limit = pageSize == null ? 50 : Math.max(1, Math.min(pageSize, 100));
        // 用户ID只取当前 JWT 对应账号；数据库条件同时过滤失败记录，避免串读和假回复。
        Long userId = dataPermissionService.currentUser().getId();
        List<AiChat> newestFirst = aiChatMapper.selectClientHistory(userId, beforeId, limit);
        List<ClientAiChatResponse> history = new ArrayList<>(newestFirst.size());
        for (int index = newestFirst.size() - 1; index >= 0; index--) {
            AiChat chat = newestFirst.get(index);
            boolean file = chat.getImageUrl() != null && chat.getImageUrl().contains("/file/");
            history.add(new ClientAiChatResponse(chat.getId(), chat.getModelId(), chat.getModelName(),
                    chat.getUserContent(), file ? null : chat.getImageUrl(), file ? chat.getImageUrl() : null,
                    file ? (StringUtils.hasText(chat.getFileName()) ? chat.getFileName() : "文件附件") : null,
                    chat.getAiContent()));
        }
        return history;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public ClientAiChatResponse sendClientMessage(ClientAiChatRequest request) {
        String content = normalizeOptionalText(request == null ? null : request.getContent());
        String imageUrl = normalizeOptionalText(request == null ? null : request.getImageUrl());
        String fileUrl = normalizeOptionalText(request == null ? null : request.getFileUrl());
        if (imageUrl != null && fileUrl != null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "一次只能发送一张图片或一个文件");
        }
        if (content == null && imageUrl == null && fileUrl == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "咨询内容不能为空");
        }
        validateLength(content, MAX_CONTENT_LENGTH, "咨询内容不能超过2000个字符");
        validateLength(imageUrl, MAX_IMAGE_URL_LENGTH, "图片地址不能超过500个字符");
        validateLength(fileUrl, MAX_IMAGE_URL_LENGTH, "文件地址不能超过500个字符");

        // 图片与文档都必须先由当前用户上传；模型请求只使用服务端校验后的文件内容。
        Path imagePath = imageUrl == null ? null : attachmentService.resolveOwnedPath(imageUrl, 2);
        Path filePath = fileUrl == null ? null : attachmentService.resolveOwnedPath(fileUrl, 4);
        String fileName = normalizeOptionalText(request.getFileName());
        if (fileName != null) {
            fileName = fileName.replace('\\', '/').replaceAll("^.*/", "").replaceAll("[\\p{Cntrl}]", "");
            validateLength(fileName, 180, "文件名称过长");
        }
        String displayContent = content != null ? content : imagePath != null
                ? "请分析这张图片并给出农业建议" : "请阅读附件并给出农业建议";
        String prompt = displayContent;
        String documentText = filePath == null ? null : readDocument(filePath);
        if (documentText != null) {
            prompt += "\n\n附件名称：" + (fileName == null ? filePath.getFileName() : fileName)
                    + "\n附件内容：\n" + documentText;
        }

        User currentUser = dataPermissionService.currentUser();
        AiModel model = resolveEnabledModel(request.getModelId());

        AiChat chat = new AiChat();
        chat.setUserId(currentUser.getId());
        chat.setModelId(model.getId());
        // 兼容既有 ai_chat 表：image_url 字段存放本次附件路径，文件类型由 /file/ 路径区分。
        chat.setImageUrl(imageUrl == null ? fileUrl : imageUrl);
        // file_name 单独持久化原始名称，避免历史记录只能从随机存储路径推断名称。
        chat.setFileName(fileUrl == null ? null : fileName);
        chat.setUserContent(displayContent);

        try {
            String reply = requestModel(model, prompt, imagePath);
            chat.setAiContent(reply);
            chat.setStatus(CHAT_STATUS_SUCCESS);
            aiChatMapper.insert(chat);
            return new ClientAiChatResponse(chat.getId(), model.getId(), model.getModelName(),
                    chat.getUserContent(), imageUrl, fileUrl, fileName, reply);
        } catch (BusinessException exception) {
            chat.setAiContent("AI咨询暂时不可用，请稍后重试。");
            chat.setStatus(CHAT_STATUS_FAILED);
            chat.setFailReason(exception.getMessage());
            aiChatMapper.insert(chat);
            throw exception;
        } catch (Exception exception) {
            chat.setAiContent("AI咨询暂时不可用，请稍后重试。");
            chat.setStatus(CHAT_STATUS_FAILED);
            chat.setFailReason(truncateReason("模型调用失败：" + safeMessage(exception)));
            aiChatMapper.insert(chat);
            throw new BusinessException(ResponseCode.SERVICE_UNAVAILABLE, "模型调用失败，请稍后重试");
        }
    }

    /** 根据用户选择或默认策略获取启用模型，避免调用已停用配置。 */
    private AiModel resolveEnabledModel(Long modelId) {
        AiModel model = modelId == null ? aiModelMapper.selectFirstEnabledDeepSeek() : aiModelMapper.selectById(modelId);
        if (model == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "请先在模型管理中配置并启用 DeepSeek 模型");
        }
        if (!Integer.valueOf(MODEL_STATUS_ENABLED).equals(model.getStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "模型已停用");
        }
        if (!StringUtils.hasText(model.getBaseUrl()) || !StringUtils.hasText(model.getApiKey()) || !StringUtils.hasText(model.getModel())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "模型配置不完整");
        }
        if (!model.getModel().toLowerCase(Locale.ROOT).startsWith("deepseek-")) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "AI咨询仅支持 DeepSeek 模型");
        }
        return model;
    }

    /** 调用 OpenAI 兼容 chat/completions 接口，返回第一条 assistant 回复。 */
    private String requestModel(AiModel model, String content, Path imagePath) throws Exception {
        String body = objectMapper.writeValueAsString(buildChatCompletionBody(model, content, imagePath));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildChatCompletionsUrl(model.getBaseUrl())))
                .timeout(MODEL_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + model.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw buildProviderException(response);
        }

        JsonNode root = objectMapper.readTree(response.body());
        String reply = root.path("choices").path(0).path("message").path("content").asText("");
        if (!StringUtils.hasText(reply)) {
            throw new BusinessException(ResponseCode.SERVICE_UNAVAILABLE, "模型服务未返回有效内容");
        }
        return reply.trim();
    }

    /**
     * 将模型供应商 HTTP 错误转换为业务可读错误。
     *
     * <p>4xx 通常是模型地址、模型标识或 API Key 配置问题，不再笼统返回 503。</p>
     */
    private BusinessException buildProviderException(HttpResponse<String> response) {
        String providerMessage = extractProviderMessage(response.body());
        String detail = StringUtils.hasText(providerMessage) ? "：" + providerMessage : "";
        int statusCode = response.statusCode();

        if (statusCode == 401 || statusCode == 403) {
            return new BusinessException(ResponseCode.PARAM_ERROR, "模型认证失败，请检查 API Key" + detail);
        }
        if (statusCode == 400 || statusCode == 404) {
            return new BusinessException(ResponseCode.PARAM_ERROR, "模型请求配置异常，请检查模型地址和模型标识" + detail);
        }
        if (statusCode == 402) {
            return new BusinessException(ResponseCode.MODEL_BALANCE_INSUFFICIENT, "模型账户余额不足，请充值或更换 API Key" + detail);
        }
        if (statusCode == 429) {
            return new BusinessException(ResponseCode.TOO_MANY_REQUESTS, "模型服务请求过于频繁，请稍后再试" + detail);
        }
        return new BusinessException(ResponseCode.SERVICE_UNAVAILABLE, "模型服务暂不可用，请稍后重试" + detail);
    }

    /** 尽量从供应商 JSON 响应中提取错误信息，避免只看到 HTTP 状态码。 */
    private String extractProviderMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = root.path("error").path("message").asText("");
            if (!StringUtils.hasText(message)) {
                message = root.path("message").asText("");
            }
            return truncateReason(message);
        } catch (Exception exception) {
            return truncateReason(responseBody);
        }
    }

    /** 构造 DeepSeek Chat Completions 请求体，私有图片转 data URL，避免模型访问不到本地地址。 */
    private Map<String, Object> buildChatCompletionBody(AiModel model, String content, Path imagePath) throws Exception {
        Object userContent = content;
        if (imagePath != null) {
            String extension = extensionOf(imagePath);
            String mimeType = "jpg".equals(extension) ? "image/jpeg" : "image/" + extension;
            String imageData = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(imagePath));
            userContent = List.of(
                    Map.of("type", "text", "text", content),
                    Map.of("type", "image_url", "image_url", Map.of("url", imageData))
            );
        }
        return Map.of(
                // 视觉请求由 flash 处理；旧 deepseek-chat/reasoner 配置沿用原凭据并映射到现行模型名称。
                "model", imagePath != null ? "deepseek-flash"
                        : "deepseek-chat".equalsIgnoreCase(model.getModel()) ? "deepseek-flash"
                        : "deepseek-reasoner".equalsIgnoreCase(model.getModel()) ? "deepseek-v4-pro"
                        : model.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你是智慧农业AI助手，请结合水培、农事管理、病虫害和设备监测场景给出简洁可靠的建议。"),
                        Map.of("role", "user", "content", userContent)
                )
        );
    }

    /** 将 base_url 规范化为 chat/completions 地址，兼容用户填写到 /v1 或完整接口路径两种情况。 */
    private String buildChatCompletionsUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/chat/completions")) {
            return trimmed;
        }
        return trimmed.replaceAll("/+$", "") + "/chat/completions";
    }

    /** 只提取可安全转为文本的文档，扫描版 PDF 和旧格式文件给出明确反馈。 */
    private String readDocument(Path path) {
        try {
            String text;
            switch (extensionOf(path)) {
                case "txt", "csv", "md", "json" -> text = Files.readString(path, StandardCharsets.UTF_8);
                case "pdf" -> {
                    try (PDDocument document = Loader.loadPDF(path.toFile())) {
                        text = new PDFTextStripper().getText(document);
                    }
                }
                case "docx" -> {
                    try (XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
                         XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                        text = extractor.getText();
                    }
                }
                case "xlsx" -> {
                    StringBuilder builder = new StringBuilder();
                    DataFormatter formatter = new DataFormatter();
                    try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(path))) {
                        workbook.forEach(sheet -> sheet.forEach(row -> {
                            row.forEach(cell -> builder.append(formatter.formatCellValue(cell)).append('\t'));
                            builder.append('\n');
                        }));
                    }
                    text = builder.toString();
                }
                case "pptx" -> {
                    StringBuilder builder = new StringBuilder();
                    try (XMLSlideShow slides = new XMLSlideShow(Files.newInputStream(path))) {
                        slides.getSlides().forEach(slide -> slide.getShapes().forEach(shape -> {
                            if (shape instanceof XSLFTextShape textShape) builder.append(textShape.getText()).append('\n');
                        }));
                    }
                    text = builder.toString();
                }
                default -> throw new BusinessException(ResponseCode.PARAM_ERROR,
                        "AI文件咨询支持 TXT、CSV、MD、JSON、PDF、DOCX、XLSX、PPTX");
            }
            if (!StringUtils.hasText(text)) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "文件没有可读取的文字，请换用文字版文件");
            }
            // 控制模型输入长度，避免大文档超出上下文或产生不可预期的费用。
            return text.length() > MAX_DOCUMENT_TEXT_LENGTH ? text.substring(0, MAX_DOCUMENT_TEXT_LENGTH) : text;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "文件读取失败，请检查文件格式");
        }
    }

    private String extensionOf(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.substring(name.lastIndexOf('.') + 1);
    }

    private QueryParams normalizeQueryParams(String username, String modelName, Integer status,
                                             LocalDate startDate, LocalDate endDate) {
        validateStatus(status);
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }
        return new QueryParams(
                normalizeOptionalText(username),
                normalizeOptionalText(modelName),
                status,
                startDate == null ? null : startDate.atStartOfDay(),
                endDate == null ? null : endDate.atTime(LocalTime.MAX));
    }

    private void validateStatus(Integer status) {
        if (status != null && status != CHAT_STATUS_SUCCESS && status != CHAT_STATUS_FAILED) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "对话状态只能为1或2");
        }
    }

    private String normalizeRequiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, message);
        }
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "AI对话ID不能为空");
        }
    }

    private String safeMessage(Exception exception) {
        return StringUtils.hasText(exception.getMessage()) ? exception.getMessage() : exception.getClass().getSimpleName();
    }

    private String truncateReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return "";
        }
        String normalized = reason.trim();
        return normalized.length() <= MAX_IMAGE_URL_LENGTH ? normalized : normalized.substring(0, MAX_IMAGE_URL_LENGTH);
    }

    private record QueryParams(String username,
                               String modelName,
                               Integer status,
                               LocalDateTime startTime,
                               LocalDateTime endTime) {
    }
}
