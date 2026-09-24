package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientExpertListResponse;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.ClientExpertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * farm 用户端专家咨询接口。
 *
 * <p>该控制器只提供用户端展示接口，不复用后台管理权限，
 * 返回字段经过 DTO 收敛，避免移动端拿到不需要的专家管理信息。</p>
 */
@RestController
@RequestMapping({"/client", "/api/client"})
@RequiredArgsConstructor
public class ClientExpertController {

    /** 用户端专家咨询服务，负责专家字段和咨询状态转换。 */
    private final ClientExpertService clientExpertService;

    /**
     * 获取全部专家列表。
     *
     * @param keyword 专家姓名搜索关键字，可为空
     * @return 包含咨询状态的全部专家列表
     */
    @GetMapping("/experts/list")
    public R<List<ClientExpertListResponse>> listExperts(@RequestParam(required = false) String keyword) {
        return R.success(clientExpertService.listExperts(keyword));
    }
}
