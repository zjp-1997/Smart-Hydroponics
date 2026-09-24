package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.ConsultMessage;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.ConsultationService;
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
 * 专家咨询管理控制器。
 *
 * <p>该控制器面向 smart_farm 后台，提供聊天内容查询、详情、删除、批量删除和统计能力。</p>
 */
@RestController
@RequestMapping("/consultation")
@RequiredArgsConstructor
@RequirePermission("consultation:manage")
public class ConsultationController {

    /** 专家咨询服务，负责数据权限、消息管理和统计查询。 */
    private final ConsultationService consultationService;

    @DeleteMapping("/{id}")
    public R<Void> deleteMessage(@PathVariable Long id) {
        consultationService.deleteMessage(id);
        return R.success();
    }

    @DeleteMapping("/batch")
    public R<Integer> deleteMessages(@RequestBody List<Long> ids) {
        return R.success(consultationService.deleteMessages(ids));
    }

    @GetMapping("/{id}")
    public R<ConsultMessage> getMessageById(@PathVariable Long id) {
        return R.success(consultationService.getMessageById(id));
    }

    @GetMapping("/list")
    public R<PageInfo<ConsultMessage>> listMessages(@RequestParam(required = false) String username,
                                                    @RequestParam(required = false) String expertName,
                                                    @RequestParam(required = false) String sessionNo,
                                                    @RequestParam(required = false) Integer messageType,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                    LocalDate startDate,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                    LocalDate endDate,
                                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(consultationService.listMessages(
                username, expertName, sessionNo, messageType, startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/statistics")
    public R<Map<String, Object>> statisticsMessages(@RequestParam(required = false) String username,
                                                     @RequestParam(required = false) String expertName,
                                                     @RequestParam(required = false) String sessionNo,
                                                     @RequestParam(required = false) Integer messageType,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                     LocalDate startDate,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                     LocalDate endDate) {
        return R.success(consultationService.statisticsMessages(
                username, expertName, sessionNo, messageType, startDate, endDate));
    }
}
