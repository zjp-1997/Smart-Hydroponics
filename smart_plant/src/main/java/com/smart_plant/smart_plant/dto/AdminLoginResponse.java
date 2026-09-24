package com.smart_plant.smart_plant.dto;

import com.smart_plant.smart_plant.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResponse {

    private String token;

    private String refreshToken;

    private String tokenType;

    private LocalDateTime expireTime;

    private LocalDateTime refreshExpireTime;

    private User admin;

    private String roleCode;

    private String dataScope;

    private List<String> permissions;
}
