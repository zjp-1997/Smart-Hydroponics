package com.smart_plant.smart_plant.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 申请本身不参与农场数据权限计算。 */
@Data
public class FarmJoinRequest {
    private Long id;
    private Long userId;
    private Long ownerUserId;
    private String requestedRole;
    /** 0 待审核，1 已批准，2 已拒绝，3 已撤销。 */
    private Integer status;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
    private String reviewReason;
    private Long revokedBy;
    private LocalDateTime revokedAt;
    private String revokeReason;
    private LocalDateTime createdAt;
    private String username;
    private String nickname;
    private String ownerName;
}
