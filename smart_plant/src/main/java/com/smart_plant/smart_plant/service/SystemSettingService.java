package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.SystemAssetUploadResponse;
import com.smart_plant.smart_plant.dto.SystemSettingUpdateRequest;
import com.smart_plant.smart_plant.entity.SystemSetting;
import org.springframework.web.multipart.MultipartFile;

/** 系统设置查询、保存与品牌资源上传服务。 */
public interface SystemSettingService {

    /** 读取可公开展示的系统设置。 */
    SystemSetting getPublicSetting();

    /** 管理员读取完整系统设置。 */
    SystemSetting getAdminSetting();

    /** 管理员保存全局系统设置。 */
    SystemSetting updateSetting(SystemSettingUpdateRequest request);

    /** 上传系统品牌图片，type仅允许logo、favicon或loginBackground。 */
    SystemAssetUploadResponse uploadAsset(String type, MultipartFile file);
}
