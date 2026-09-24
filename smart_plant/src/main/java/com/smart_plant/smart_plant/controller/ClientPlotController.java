package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientPlotDetailResponse;
import com.smart_plant.smart_plant.dto.ClientPlotListResponse;
import com.smart_plant.smart_plant.dto.ClientPlotUpdateRequest;
import com.smart_plant.smart_plant.entity.Crop;
import com.smart_plant.smart_plant.dto.PlotHarvestRequest;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.PlotService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * farm 用户端地块列表接口。
 *
 * <p>该 Controller 不接收 userId 参数，所有数据范围都由登录身份和普通用户绑定关系决定；
 * 这样可以避免移动端伪造用户 ID 查询其他农场主的地块数据。</p>
 */
@RestController
@RequestMapping({"/client", "/api/client"})
@RequiredArgsConstructor
public class ClientPlotController {

    private final PlotService plotService;

    @GetMapping("/plots/list")
    public R<List<ClientPlotListResponse>> listCurrentUserPlots() {
        // 返回当前农场主或普通用户所属农场主名下的全部启用地块。
        return R.success(plotService.listCurrentClientPlots());
    }

    @GetMapping("/farms/{farmId}/plots")
    public R<List<ClientPlotListResponse>> listCurrentUserFarmPlots(@PathVariable Long farmId) {
        // 返回可见农场下的启用地块，并在服务层校验农场归属。
        return R.success(plotService.listCurrentClientPlotsByFarmId(farmId));
    }

    @GetMapping("/plots/{id}")
    public R<ClientPlotDetailResponse> getCurrentUserPlotDetail(@PathVariable Long id) {
        // 根据地块ID返回详情聚合数据，服务层会统一校验地块是否属于当前登录用户。
        return R.success(plotService.getCurrentClientPlotDetail(id));
    }

    /** 编辑选项与提交入口均由服务层再次校验农场主身份。 */
    @GetMapping("/plots/crops")
    public R<List<Crop>> listEditableCrops() {
        return R.success(plotService.listCurrentClientCrops());
    }

    @PutMapping("/plots/{id}")
    public R<Void> updateCurrentUserPlot(@PathVariable Long id,
                                         @Valid @RequestBody ClientPlotUpdateRequest request) {
        plotService.updateCurrentClientPlot(id, request);
        return R.success();
    }

    @PutMapping("/plots/{id}/harvest")
    public R<Void> harvestCurrentUserPlot(@PathVariable Long id,
                                          @Valid @RequestBody PlotHarvestRequest request) {
        plotService.harvestPlot(id, request);
        return R.success();
    }
}
