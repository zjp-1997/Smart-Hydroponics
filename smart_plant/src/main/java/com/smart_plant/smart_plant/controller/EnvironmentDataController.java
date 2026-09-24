package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.EnvironmentData;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.EnvironmentDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 环境监测数据管理控制器。
 *
 * <p>该控制器提供环境监测数据的删除、批量删除、分页查询和详情查询接口。
 * 由于数据由传感器自动上传，不提供新增和修改接口。</p>
 */
@RestController
@RequestMapping("/environment-data")
@RequiredArgsConstructor
@RequirePermission("sensor_data:manage")
public class EnvironmentDataController {

    /** 注入环境监测数据业务服务。 */
    private final EnvironmentDataService environmentDataService;

    /** 根据ID删除单条环境监测数据。 */
    @DeleteMapping("/{id}")
    public R<Void> deleteEnvironmentData(@PathVariable Long id) {
        environmentDataService.deleteEnvironmentData(id);
        return R.success();
    }

    /** 根据ID集合批量删除环境监测数据。 */
    @DeleteMapping("/batch")
    public R<Integer> deleteEnvironmentDataBatch(@RequestBody List<Long> ids) {
        return R.success(environmentDataService.deleteEnvironmentDataBatch(ids));
    }

    /** 根据ID查询环境监测数据详情，用于弹框展示。 */
    @GetMapping("/{id}")
    public R<EnvironmentData> getEnvironmentDataById(@PathVariable Long id) {
        return R.success(environmentDataService.getEnvironmentDataById(id));
    }

    /** 分页条件查询环境监测数据列表，支持设备ID、数据状态和采集时间范围筛选。 */
    @GetMapping("/list")
    public R<PageInfo<EnvironmentData>> listEnvironmentData(
            @RequestParam(required = false) Long plotId,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) Integer dataStatus,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(environmentDataService.listEnvironmentData(
                plotId, deviceId, deviceCode, dataStatus, startTime, endTime, pageNum, pageSize));
    }

    /** 统计环境监测数据，返回总数、正常数、异常数、设备数和平均指标。 */
    @GetMapping("/statistics")
    public R<Map<String, Object>> statisticsEnvironmentData(
            @RequestParam(required = false) Long plotId,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) Integer dataStatus,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.success(environmentDataService.statisticsEnvironmentData(
                plotId, deviceId, deviceCode, dataStatus, startTime, endTime));
    }
}
