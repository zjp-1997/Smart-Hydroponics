package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRecognitionRecord {

    private Long id;

    private Long userId;

    private String username;

    private String nickname;

    private Integer sourceType;

    private Long cropImageId;

    private Long cameraId;

    private String cameraName;

    private Long plotId;

    private String plotName;

    private String plotCode;

    private Long captureId;

    private String imageUrl;

    private String thumbnailUrl;

    private Long imageSize;

    private Integer status;

    private LocalDateTime recognitionStartTime;

    private LocalDateTime recognitionEndTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long resultId;

    private Long recognitionType;

    private String recognitionTypeName;

    private Long cropId;

    private String cropName;

    private String resultName;

    private String resultSummary;

    private String resultDetail;

    private Long diseasePestId;

    private String diseasePestName;

    private BigDecimal confidence;

    private Integer severityLevel;

    private String suggestion;

    private String modelName;

    private String modelVersion;

    private Integer resultStatus;

    private String failReason;

    private LocalDateTime recognizeTime;

    private String resultRemark;

    private LocalDateTime resultCreateTime;

    private Integer resultCount;
}
