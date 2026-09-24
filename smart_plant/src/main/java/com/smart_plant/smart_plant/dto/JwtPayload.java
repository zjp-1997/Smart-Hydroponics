package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtPayload {

    private Long adminId;

    private String username;

    private String roleCode;

    private String tokenType;

    private Boolean rememberMe;

    private Long iat;

    private Long exp;

    private String jti;
}
