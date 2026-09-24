package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AiChat;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * AI对话管理控制器。
 *
 * <p>该控制器面向 smart_farm 后台，提供对话记录查询、详情、删除、批量删除和统计接口。</p>
 */
@RestController
@RequestMapping("/ai-chat")
@RequiredArgsConstructor
@RequirePermission("ai_chat:manage")
public class AiChatController {

    /** AI对话服务，负责数据权限过滤和对话记录管理。 */
    private final AiChatService aiChatService;

    @DeleteMapping("/{id}")
    public R<Void> deleteChat(@PathVariable Long id) {
        aiChatService.deleteChat(id);
        return R.success();
    }

    @DeleteMapping("/batch")
    public R<Integer> deleteChats(@RequestBody List<Long> ids) {
        return R.success(aiChatService.deleteChats(ids));
    }

    @GetMapping("/{id}")
    public R<AiChat> getChatById(@PathVariable Long id) {
        return R.success(aiChatService.getChatById(id));
    }

    @GetMapping("/list")
    public R<PageInfo<AiChat>> listChats(@RequestParam(required = false) String username,
                                         @RequestParam(required = false) String modelName,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(pattern = "yyyy-MM-dd")
                                         LocalDate startDate,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(pattern = "yyyy-MM-dd")
                                         LocalDate endDate,
                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(aiChatService.listChats(username, modelName, status, startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/statistics")
    public R<Map<String, Object>> statisticsChats(@RequestParam(required = false) String username,
                                                  @RequestParam(required = false) String modelName,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false)
                                                  @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                  LocalDate startDate,
                                                  @RequestParam(required = false)
                                                  @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                  LocalDate endDate) {
        return R.success(aiChatService.statisticsChats(username, modelName, status, startDate, endDate));
    }
}
