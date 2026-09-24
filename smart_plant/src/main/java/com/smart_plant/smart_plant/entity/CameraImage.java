package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraImage {

    private Long id;

    private Long cameraId;

    private String cameraName;

    private Long batchId;

    private Long plotId;

    private String plotName;

    private String plotCode;

    private Long userId;

    private String username;

    private String nickname;

    private String imageUrl;

    private String thumbnailUrl;

    private Long imageSize;

    private Integer captureType;

    private LocalDateTime captureTime;

    private Integer aiChecked;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
