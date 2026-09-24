package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.UserOauthAccount;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.mapper.UserOauthAccountMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.UserOauthAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserOauthAccountServiceImpl implements UserOauthAccountService {

    private final UserOauthAccountMapper oauthAccountMapper;

    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserOauthAccount addOauthAccount(UserOauthAccount oauthAccount) {
        validateCreateOauthAccount(oauthAccount);
        checkUserExists(oauthAccount.getUserId());
        checkProviderAndOpenIdUnique(oauthAccount);
        oauthAccountMapper.insert(oauthAccount);
        return oauthAccountMapper.selectById(oauthAccount.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserOauthAccount updateOauthAccount(UserOauthAccount oauthAccount) {
        if (oauthAccount == null || oauthAccount.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "第三方账号ID不能为空");
        }
        UserOauthAccount oldOauthAccount = oauthAccountMapper.selectById(oauthAccount.getId());
        if (oldOauthAccount == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "第三方账号不存在");
        }
        if (oauthAccount.getUserId() != null) {
            checkUserExists(oauthAccount.getUserId());
        }
        checkProviderAndOpenIdUnique(oauthAccount);
        int rows = oauthAccountMapper.updateById(oauthAccount);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "第三方账号修改失败");
        }
        return oauthAccountMapper.selectById(oauthAccount.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOauthAccount(Long id) {
        requireId(id);
        int rows = oauthAccountMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "第三方账号不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteOauthAccounts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的第三方账号");
        }
        return oauthAccountMapper.deleteBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireId(id);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "第三方账号状态只能为0或1");
        }
        int rows = oauthAccountMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "第三方账号不存在");
        }
    }

    @Override
    public UserOauthAccount getOauthAccountById(Long id) {
        requireId(id);
        UserOauthAccount oauthAccount = oauthAccountMapper.selectById(id);
        if (oauthAccount == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "第三方账号不存在");
        }
        return oauthAccount;
    }

    @Override
    public PageInfo<UserOauthAccount> listOauthAccounts(Long userId, Integer provider, String openId, String nickname,
                                                        Integer status, Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(oauthAccountMapper.selectList(userId, provider, openId, nickname, status));
    }

    private void validateCreateOauthAccount(UserOauthAccount oauthAccount) {
        if (oauthAccount == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "第三方账号信息不能为空");
        }
        if (oauthAccount.getUserId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "关联用户ID不能为空");
        }
        if (oauthAccount.getProvider() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "第三方平台不能为空");
        }
        if (!StringUtils.hasText(oauthAccount.getOpenId())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "OpenID不能为空");
        }
    }

    private void checkProviderAndOpenIdUnique(UserOauthAccount oauthAccount) {
        if (oauthAccount.getProvider() == null || !StringUtils.hasText(oauthAccount.getOpenId())) {
            return;
        }
        int count = oauthAccountMapper.countByProviderAndOpenId(
                oauthAccount.getProvider(),
                oauthAccount.getOpenId(),
                oauthAccount.getId()
        );
        if (count > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该第三方平台账号已存在");
        }
    }

    private void checkUserExists(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "关联用户不存在");
        }
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "第三方账号ID不能为空");
        }
    }
}
