package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantingBatch {

    private Long id;

    private Long plotId;

    private String plotName;

    private Long cropId;

    private String cropName;

    private Long userId;

    private String username;

    private String nickname;

    private String batchNo;

    private BigDecimal plantingArea;

    private String areaUnit;

    private LocalDate plantedAt;

    private LocalDate expectedHarvestAt;

    private LocalDate actualHarvestAt;

    private String harvester;

    private Integer growthStage;

    private Long growthStageId;

    private String growthStageName;

    private BigDecimal expectedYieldAmount;

    private Integer grownDays;

    private String cropImage;

    private Integer status;

    private BigDecimal yieldAmount;

    private String yieldUnit;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
