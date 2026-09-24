package com.smart_plant.smart_plant.dto;

import lombok.Data;

/**
 * farm 专家身份认证资料。
 *
 * <p>只开放专家本人可维护的材料字段，审核状态、服务状态等平台字段由服务端控制。</p>
 */
@Data
public class ExpertCertificationRequest {

    private String realName;
    private String organization;
    private String jobTitle;
    private String phone;
    private String email;
    private String specialty;
    private String introduction;
    private String certificateName;
    private String certificateUrl;
}
