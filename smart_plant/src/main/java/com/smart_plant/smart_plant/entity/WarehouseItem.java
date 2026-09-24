package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseItem {

    private Long id;

    private Long userId;

    /** 物资所属农场主；历史未分配物资允许为空。 */
    private Long farmOwnerId;

    private String farmOwnerName;

    private String username;

    private String nickname;

    private Long inboundOperatorId;

    private String inboundOperatorName;

    private String latestOutboundOperatorName;

    private String latestOutboundRecipient;

    private String itemName;

    private String itemCode;

    private String imageUrl;

    private Integer category;

    private String specification;

    private String unit;

    private BigDecimal stockQty;

    /** 首次入库流水单价，不写入物资主表。 */
    private BigDecimal initialUnitPrice;

    private BigDecimal warningQty;

    private String manufacturer;

    private Integer status;

    /** 逻辑删除标记：0正常，1已删除。 */
    private Integer isDeleted;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
