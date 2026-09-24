package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.JwtPayload;
import com.smart_plant.smart_plant.entity.User;

public interface JwtService {

    String generateToken(User admin, boolean rememberMe);

    String generateAccessToken(User admin, boolean rememberMe);

    String generateRefreshToken(User admin, boolean rememberMe);

    JwtPayload parseToken(String token);
}
