package com.smart_plant.smart_plant.entity;

import com.smart_plant.smart_plant.dto.PlotBoundaryPoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plot {

    private Long id;

    private Long farmId;

    private String farmName;

    private String farmCode;

    private Long userId;

    private String username;

    private String nickname;

    private String plotName;

    private String plotCode;

    private String cropImage;

    private Integer type;

    private BigDecimal area;

    private String areaUnit;

    private String address;

    /**
     * 兼容前端的只读坐标文本，由数据库根据 longitude/latitude 自动生成，禁止作为持久化事实源。
     */
    private String coordinate;

    private BigDecimal longitude;

    private BigDecimal latitude;

    /** 地块边界顶点列表，按顺序连接形成闭合多边形，坐标系为 WGS-84。 */
    private List<PlotBoundaryPoint> boundaryPoints;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long currentBatchId;

    private String currentBatchNo;

    private Long currentCropId;

    private String currentCropName;

    private String currentCropVariety;

    private String currentCropImage;

    private BigDecimal currentPlantingArea;

    private String currentPlantingAreaUnit;

    private LocalDate currentPlantedAt;

    private LocalDate currentExpectedHarvestAt;

    private LocalDate currentActualHarvestAt;

    private Long currentGrowthStageId;

    private String currentGrowthStageName;

    private Integer currentGrowthStageOrder;

    private Integer currentBatchStatus;

    private BigDecimal currentExpectedYieldAmount;

    private Integer currentGrownDays;

    private BigDecimal currentYieldAmount;

    private String currentYieldUnit;

    private String currentBatchRemark;
}
