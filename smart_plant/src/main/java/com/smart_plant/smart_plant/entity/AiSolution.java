package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiSolution {

    private Long id;

    private Long recordId;

    private Long resultId;

    private Long userId;

    private String username;

    private String nickname;

    private Long plotId;

    private String plotName;

    private String plotCode;

    private Long cameraId;

    private String cameraName;

    private String solutionTitle;

    private String solutionSummary;

    private String solutionDetail;

    private Integer priorityLevel;

    private Integer generateTask;

    private Integer taskGenerated;

    private Long farmTaskId;

    private String farmTaskTitle;

    private Integer farmTaskStatus;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
