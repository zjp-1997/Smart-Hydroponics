package com.smart_plant.smart_plant.dto;

import lombok.Data;

/** 系统设置更新白名单，拒绝接收主键、审计字段等数据库内部属性。 */
@Data
public class SystemSettingUpdateRequest {

    private String systemName;
    private String logoUrl;
    private String faviconUrl;
    private String systemDescription;
    private String copyrightInfo;
    private String homeTitle;
    private String loginBackgroundUrl;
    private String themeColor;
    private String timezone;
    private String datetimeFormat;
    private Integer defaultPageSize;
    private Integer maxUploadSizeMb;
    private String allowedUploadTypes;
    private String recordNumber;
    private String officialWebsite;
    private String contactEmail;
    private String servicePhone;
    /** 客户端读取配置时获得的版本号，保存时用于乐观锁校验。 */
    private Integer version;
}
