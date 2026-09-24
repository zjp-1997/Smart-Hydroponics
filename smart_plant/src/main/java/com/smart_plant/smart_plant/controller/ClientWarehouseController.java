package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientWarehouseOverviewResponse;
import com.smart_plant.smart_plant.dto.ClientWarehouseRecordPage;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * farm 用户端仓库查询接口。
 * 用户身份统一从 JWT 获取，接口不接收 userId，避免越权读取其他用户的库存。
 */
@RestController
@RequestMapping({"/client/warehouse", "/api/client/warehouse"})
@RequiredArgsConstructor
public class ClientWarehouseController {

    /** 复用仓库领域服务中的数据权限、分页和库存统计能力。 */
    private final WarehouseService warehouseService;

    /**
     * 聚合仓库首页所需数据，农场主按令牌身份查看自己的物资。
     */
    @GetMapping("/overview")
    public R<ClientWarehouseOverviewResponse> getOverview(@RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Integer category,
                                                           @RequestParam(required = false) Integer pageNum,
                                                           @RequestParam(required = false) Integer pageSize) {
        // 普通用户沿用已分配物资的共享视图，农场主由服务层限定归属。
        return R.success(warehouseService.getClientOverview(keyword, category, pageNum, pageSize));
    }

    /** 我的－仓库记录分页接口，身份及数据归属均由服务层按当前令牌限定。 */
    @GetMapping("/records")
    public R<ClientWarehouseRecordPage> listRecords(@RequestParam(required = false) Integer recordType,
                                                    @RequestParam(required = false) Integer pageNum,
                                                    @RequestParam(required = false) Integer pageSize) {
        return R.success(warehouseService.listClientRecords(recordType, pageNum, pageSize));
    }
}
