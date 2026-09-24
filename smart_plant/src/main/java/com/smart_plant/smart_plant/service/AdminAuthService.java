package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.AdminLoginRequest;
import com.smart_plant.smart_plant.dto.AdminLoginResponse;
import com.smart_plant.smart_plant.dto.AdminUserInfoResponse;
import com.smart_plant.smart_plant.dto.AdminProfileUpdateRequest;
import com.smart_plant.smart_plant.dto.AdminPasswordChangeRequest;
import com.smart_plant.smart_plant.dto.AdminAvatarUploadResponse;
import com.smart_plant.smart_plant.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface AdminAuthService {

    AdminLoginResponse login(AdminLoginRequest loginRequest, String ip, String userAgent);

    AdminLoginResponse refresh(String refreshToken);

    AdminUserInfoResponse currentUserInfo();

    /** 返回当前后台登录用户的最新个人资料。 */
    User currentProfile();

    /** 更新当前后台登录用户允许自行维护的资料字段。 */
    User updateCurrentProfile(AdminProfileUpdateRequest request);

    /** 上传当前后台用户的新头像并返回可持久化地址。 */
    AdminAvatarUploadResponse uploadCurrentAvatar(MultipartFile file);

    /** 校验原密码后修改当前后台用户密码。 */
    void changeCurrentPassword(AdminPasswordChangeRequest request);

    void logout(String token);
}
