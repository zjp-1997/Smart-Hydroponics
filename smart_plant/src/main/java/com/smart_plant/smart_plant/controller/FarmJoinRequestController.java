package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.FarmJoinRequest;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.FarmJoinRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/farm-join-requests")
@RequirePermission("user:manage")
@RequiredArgsConstructor
public class FarmJoinRequestController {
    private final FarmJoinRequestService service;

    public record Decision(Integer status, String reason) { }

    /** 返回当前审核人可见的待审核数量，供管理端入场申请按钮展示。 */
    @GetMapping("/count")
    public R<Long> countPending() {
        return R.success(service.countPending());
    }

    @GetMapping
    public R<PageInfo<FarmJoinRequest>> list(@RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize) {
        return R.success(service.list(status, pageNum, pageSize));
    }

    @PutMapping("/{userId}/decision")
    public R<Void> decide(@PathVariable Long userId, @RequestBody Decision decision) {
        service.decide(userId, decision == null ? null : decision.status(),
                decision == null ? null : decision.reason());
        return R.success();
    }
}
