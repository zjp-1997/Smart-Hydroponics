package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.UserOauthAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserOauthAccountMapper {

    int insert(UserOauthAccount oauthAccount);

    int deleteById(Long id);

    int deleteBatchByIds(@Param("ids") List<Long> ids);

    int updateById(UserOauthAccount oauthAccount);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    UserOauthAccount selectById(Long id);

    List<UserOauthAccount> selectList(@Param("userId") Long userId,
                                      @Param("provider") Integer provider,
                                      @Param("openId") String openId,
                                      @Param("nickname") String nickname,
                                      @Param("status") Integer status);

    int countByProviderAndOpenId(@Param("provider") Integer provider,
                                 @Param("openId") String openId,
                                 @Param("excludeId") Long excludeId);
}
