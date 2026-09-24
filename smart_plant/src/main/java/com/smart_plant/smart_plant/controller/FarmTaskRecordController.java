package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.FarmTaskRecord;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.FarmTaskRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/farm-task-record")
@RequiredArgsConstructor
@RequirePermission("farm_task_record:manage")
public class FarmTaskRecordController {

    private final FarmTaskRecordService farmTaskRecordService;

    @PostMapping("/add")
    public R<FarmTaskRecord> addFarmTaskRecord(@RequestBody FarmTaskRecord record) {
        return R.success(farmTaskRecordService.addFarmTaskRecord(record));
    }

    @GetMapping("/{id}")
    public R<FarmTaskRecord> getFarmTaskRecordById(@PathVariable Long id) {
        return R.success(farmTaskRecordService.getFarmTaskRecordById(id));
    }

    @GetMapping("/list")
    public R<PageInfo<FarmTaskRecord>> listFarmTaskRecords(@RequestParam(required = false) Long taskId,
                                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(farmTaskRecordService.listFarmTaskRecords(taskId, pageNum, pageSize));
    }
}
