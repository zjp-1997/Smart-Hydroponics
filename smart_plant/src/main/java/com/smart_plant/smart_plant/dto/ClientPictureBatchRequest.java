package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * farm 用户端图片批量操作请求对象。
 *
 * <p>图片管理页混合展示手机图片和摄像头图片，因此每个条目必须同时携带来源类型和图片ID。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientPictureBatchRequest {

    /** 需要操作的图片列表。 */
    private List<PictureRef> pictures = new ArrayList<>();

    /**
     * 图片引用对象。
     *
     * <p>sourceType 取值为 phone 或 camera，后端据此路由到对应数据表并校验当前用户权限。</p>
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PictureRef {
        /** 图片主键ID。 */
        private Long id;

        /** 图片来源：phone 表示手机图片，camera 表示摄像头图片。 */
        private String sourceType;
    }
}
