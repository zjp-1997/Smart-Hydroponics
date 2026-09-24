package com.smart_plant.smart_plant.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOauthAccount {

    private Long id;

    private Long userId;

    private Integer provider;

    private String openId;

    private String unionId;

    private String nickname;

    private String avatar;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String accessToken;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String refreshToken;

    private LocalDateTime tokenExpireTime;

    private LocalDateTime bindTime;

    private LocalDateTime lastLoginTime;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
