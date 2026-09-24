package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientFarmListResponse;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.FarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * farm 用户端农场接口。
 *
 * <p>该接口不使用后台管理的 farm:manage 权限，而是依赖 JWT 登录态识别当前用户；
 * 具体数据隔离在 FarmService 中完成，农场主看本人农场，普通用户看绑定农场。</p>
 */
@RestController
@RequestMapping({"/client/farms", "/api/client/farms"})
@RequiredArgsConstructor
public class ClientFarmController {

    private final FarmService farmService;

    @GetMapping("/list")
    public R<List<ClientFarmListResponse>> listCurrentUserFarms() {
        // 不接收 userId 参数，避免客户端伪造其他用户 ID 查询农场。
        return R.success(farmService.listCurrentClientFarms());
    }
}
