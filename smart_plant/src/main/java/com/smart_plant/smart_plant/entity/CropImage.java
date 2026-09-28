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

    /** 关联病虫害知识库记录；为空时表示普通手机图片。 */
    private Long diseasePestId;

    /** 列表查询时由关联表补充，不直接写入 crop_image。 */
    private String diseasePestName;

    private String username;

    private String nickname;

    private String imageUrl;

    private Long imageSize;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
