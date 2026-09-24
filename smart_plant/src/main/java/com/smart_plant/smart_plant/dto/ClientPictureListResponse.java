package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * farm 用户端图片列表响应对象。
 *
 * <p>图片管理页需要同时展示手机拍摄图片和摄像头采集图片，
 * 这里按来源分组返回，便于移动端生成“手机图片 / 摄像头图片”两个相册入口。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientPictureListResponse {

    /** 当前登录用户可见的全部图片，包含手机图片和摄像头采集图片。 */
    private List<PictureItem> pictures = new ArrayList<>();

    /** 当前登录用户上传或拍摄的手机图片。 */
    private List<PictureItem> phoneImages = new ArrayList<>();

    /** 当前登录用户名下摄像头采集的图片。 */
    private List<PictureItem> cameraImages = new ArrayList<>();

    /**
     * 用户端图片基础信息。
     *
     * <p>只返回页面需要的时间和图片地址，避免暴露后台图片管理的冗余字段。</p>
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PictureItem {
        /** 图片主键。 */
        private Long id;

        /** 图片来源，phone 表示手机图片，camera 表示摄像头图片。 */
        private String sourceType;

        /** 图片访问地址。 */
        private String imageUrl;

        /** 图片产生时间，手机图片取创建时间，摄像头图片优先取采集时间。 */
        private LocalDateTime imageTime;
    }
}
