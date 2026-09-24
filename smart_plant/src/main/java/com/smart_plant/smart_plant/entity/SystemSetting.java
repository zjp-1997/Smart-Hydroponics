package com.smart_plant.smart_plant.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 后台站点唯一一条全局系统配置。 */
@Data
public class SystemSetting {

    /** 固定为1的单例主键。 */
    private Integer id;
    /** 系统名称。 */
    private String systemName;
    /** 系统Logo相对地址或HTTP(S)地址。 */
    private String logoUrl;
    /** 浏览器网站图标地址。 */
    private String faviconUrl;
    /** 系统用途描述。 */
    private String systemDescription;
    /** 页面展示的版权信息。 */
    private String copyrightInfo;
    /** 首页及浏览器标题。 */
    private String homeTitle;
    /** 登录页背景图地址。 */
    private String loginBackgroundUrl;
    /** 十六进制系统主题色。 */
    private String themeColor;
    /** IANA时区名称，例如Asia/Shanghai。 */
    private String timezone;
    /** Java日期时间格式。 */
    private String datetimeFormat;
    /** 列表分页默认条数。 */
    private Integer defaultPageSize;
    /** 文件上传大小限制，单位MB。 */
    private Integer maxUploadSizeMb;
    /** 允许上传的扩展名，统一使用逗号分隔。 */
    private String allowedUploadTypes;
    /** 网站备案号。 */
    private String recordNumber;
    /** 官网HTTP(S)地址。 */
    private String officialWebsite;
    /** 站点联系邮箱。 */
    private String contactEmail;
    /** 站点客服电话。 */
    private String servicePhone;
    /** 用于防止并发覆盖的乐观锁版本号。 */
    private Integer version;
    /** 最后修改用户ID。 */
    private Long updateBy;
    /** 配置创建时间。 */
    private LocalDateTime createTime;
    /** 配置最后修改时间。 */
    private LocalDateTime updateTime;
}
