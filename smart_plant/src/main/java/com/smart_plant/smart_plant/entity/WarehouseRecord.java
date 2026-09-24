package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseRecord {

    private Long id;

    /** 客户端为一次业务提交生成的唯一键，用于安全重试。 */
    private String requestId;

    private Long itemId;

    private String itemName;
    /** 记录所对应物资的计量单位，供 farm 流水页显示数量。 */
    private String itemUnit;

    private String itemCode;

    private Long userId;

    private String username;

    private String nickname;

    private Long operatorId;

    private String operatorName;

    private String recipient;

    private Integer recordType;

    private BigDecimal quantity;

    private BigDecimal beforeQty;

    private BigDecimal afterQty;

    private Long relatedPlotId;

    private String relatedPlotName;

    private Long relatedTaskId;

    private String relatedTaskTitle;

    private Long relatedFaultId;

    private String relatedFaultCode;

    private String relatedFaultName;

    private Integer relatedFaultStatus;

    private Long relatedDeviceId;

    private String relatedDeviceName;

    private Integer sourceType;

    private String supplier;

    private BigDecimal price;

    private BigDecimal totalAmount;

    private LocalDateTime recordTime;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
