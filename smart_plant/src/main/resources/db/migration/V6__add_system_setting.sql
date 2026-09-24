-- 系统设置采用单行表保存，固定主键可避免同一站点出现多套互相冲突的配置。
CREATE TABLE IF NOT EXISTS `system_setting` (
    `id` tinyint NOT NULL DEFAULT 1 COMMENT '固定主键，系统仅保留一条配置',
    `system_name` varchar(100) NOT NULL COMMENT '系统名称',
    `logo_url` varchar(255) NULL COMMENT '系统Logo地址',
    `favicon_url` varchar(255) NULL COMMENT '网站图标地址',
    `system_description` varchar(500) NULL COMMENT '系统描述',
    `copyright_info` varchar(255) NULL COMMENT '版权信息',
    `home_title` varchar(100) NOT NULL COMMENT '首页标题',
    `login_background_url` varchar(255) NULL COMMENT '登录页背景地址',
    `theme_color` char(7) NOT NULL COMMENT '系统主题色，格式为#RRGGBB',
    `timezone` varchar(64) NOT NULL COMMENT 'IANA时区名称',
    `datetime_format` varchar(64) NOT NULL COMMENT '日期时间格式',
    `default_page_size` int NOT NULL COMMENT '分页默认条数',
    `max_upload_size_mb` int NOT NULL COMMENT '文件上传大小上限，单位MB',
    `allowed_upload_types` varchar(255) NOT NULL COMMENT '允许上传的文件扩展名，逗号分隔',
    `record_number` varchar(100) NULL COMMENT '网站备案号',
    `official_website` varchar(255) NULL COMMENT '官网地址',
    `contact_email` varchar(100) NULL COMMENT '联系邮箱',
    `service_phone` varchar(30) NULL COMMENT '客服电话',
    `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `update_by` bigint NULL COMMENT '最后修改用户ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `chk_system_setting_singleton` CHECK (`id` = 1),
    CONSTRAINT `chk_system_setting_page_size` CHECK (`default_page_size` BETWEEN 5 AND 200),
    CONSTRAINT `chk_system_setting_upload_size` CHECK (`max_upload_size_mb` BETWEEN 1 AND 50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台系统全局设置';

-- 首次迁移写入与当前 smart_farm 展示一致的默认值，升级后页面视觉不会突变。
INSERT IGNORE INTO `system_setting` (
    `id`, `system_name`, `system_description`, `copyright_info`, `home_title`,
    `theme_color`, `timezone`, `datetime_format`, `default_page_size`,
    `max_upload_size_mb`, `allowed_upload_types`
) VALUES (
    1, 'Smart Plant', '家庭小型水培后台管理系统', '© 2026 Smart Plant', '智慧农业工作台',
    '#239aaa', 'Asia/Shanghai', 'yyyy-MM-dd HH:mm:ss', 10,
    5, 'jpg,jpeg,png,webp,pdf,xlsx,xls,csv,doc,docx'
);
