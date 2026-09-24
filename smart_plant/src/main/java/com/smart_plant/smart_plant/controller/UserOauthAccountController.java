package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.UserOauthAccount;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.UserOauthAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/oauth-account")
@RequiredArgsConstructor
@RequirePermission("user_oauth:manage")
public class UserOauthAccountController {

    private final UserOauthAccountService oauthAccountService;

    @PostMapping("/add")
    public R<UserOauthAccount> addOauthAccount(@RequestBody UserOauthAccount oauthAccount) {
        return R.success(oauthAccountService.addOauthAccount(oauthAccount));
    }

    @PutMapping
    public R<UserOauthAccount> updateOauthAccount(@RequestBody UserOauthAccount oauthAccount) {
        return R.success(oauthAccountService.updateOauthAccount(oauthAccount));
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteOauthAccount(@PathVariable Long id) {
        oauthAccountService.deleteOauthAccount(id);
        return R.success();
    }

    @DeleteMapping("/batch")
    public R<Integer> deleteOauthAccounts(@RequestBody List<Long> ids) {
        return R.success(oauthAccountService.deleteOauthAccounts(ids));
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        oauthAccountService.updateStatus(id, status);
        return R.success();
    }

    @GetMapping("/{id}")
    public R<UserOauthAccount> getOauthAccountById(@PathVariable Long id) {
        return R.success(oauthAccountService.getOauthAccountById(id));
    }

    @GetMapping("/list")
    public R<PageInfo<UserOauthAccount>> listOauthAccounts(@RequestParam(required = false) Long userId,
                                                           @RequestParam(required = false) Integer provider,
                                                           @RequestParam(required = false) String openId,
                                                           @RequestParam(required = false) String nickname,
                                                           @RequestParam(required = false) Integer status,
                                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(oauthAccountService.listOauthAccounts(userId, provider, openId, nickname, status, pageNum, pageSize));
    }
}
