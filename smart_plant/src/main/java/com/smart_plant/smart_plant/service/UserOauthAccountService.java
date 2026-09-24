package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.UserOauthAccount;

import java.util.List;

public interface UserOauthAccountService {

    UserOauthAccount addOauthAccount(UserOauthAccount oauthAccount);

    UserOauthAccount updateOauthAccount(UserOauthAccount oauthAccount);

    void deleteOauthAccount(Long id);

    int deleteOauthAccounts(List<Long> ids);

    void updateStatus(Long id, Integer status);

    UserOauthAccount getOauthAccountById(Long id);

    PageInfo<UserOauthAccount> listOauthAccounts(Long userId, Integer provider, String openId, String nickname,
                                                 Integer status, Integer pageNum, Integer pageSize);
}
