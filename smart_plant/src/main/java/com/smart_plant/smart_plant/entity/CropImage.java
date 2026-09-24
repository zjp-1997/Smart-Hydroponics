package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CropImage {

    private Long id;

    private Long userId;

    private String username;

    private String nickname;

    private String imageUrl;

    private Long imageSize;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
