package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.AdminLoginLog;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.AdminLoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/login-log")
@RequiredArgsConstructor
@RequirePermission("login_log:view")
public class AdminLoginLogController {

    private final AdminLoginLogService loginLogService;

    @GetMapping("/list")
    public R<PageInfo<AdminLoginLog>> listLoginLogs(@RequestParam(required = false) String username,
                                                    @RequestParam(required = false) String ip,
                                                    @RequestParam(required = false) Integer success,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                    LocalDateTime startTime,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                    LocalDateTime endTime,
                                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(loginLogService.listLoginLogs(username, ip, success, startTime, endTime, pageNum, pageSize));
    }
}
