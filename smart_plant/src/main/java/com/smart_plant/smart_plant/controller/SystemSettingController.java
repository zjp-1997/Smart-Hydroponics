package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.SystemAssetUploadResponse;
import com.smart_plant.smart_plant.dto.SystemSettingUpdateRequest;
import com.smart_plant.smart_plant.entity.SystemSetting;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** smart_farm 系统设置弹框对应的管理员接口。 */
@RestController
@RequestMapping("/admin/system-settings")
@RequiredArgsConstructor
@RequirePermission("system_config:manage")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    /** 读取当前完整设置及乐观锁版本号。 */
    @GetMapping
    public R<SystemSetting> getSetting() {
        return R.success(systemSettingService.getAdminSetting());
    }

    /** 整体保存设置，确保五个菜单分区的数据处于同一事务。 */
    @PutMapping
    public R<SystemSetting> updateSetting(@RequestBody SystemSettingUpdateRequest request) {
        return R.success(systemSettingService.updateSetting(request));
    }

    /** 上传Logo、网站图标或登录页背景，返回可保存的相对地址。 */
    @PostMapping("/assets")
    public R<SystemAssetUploadResponse> uploadAsset(@RequestParam String type,
                                                     @RequestParam("file") MultipartFile file) {
        return R.success(systemSettingService.uploadAsset(type, file));
    }
}
