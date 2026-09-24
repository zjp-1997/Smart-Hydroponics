package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginLog {

    private Long id;

    private Long adminId;

    private String username;

    private Integer success;

    private String failureReason;

    private String ip;

    private String userAgent;

    private LocalDateTime loginTime;
}
