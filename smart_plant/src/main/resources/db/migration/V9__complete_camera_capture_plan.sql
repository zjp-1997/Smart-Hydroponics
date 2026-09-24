-- 摄像头抓拍计划在原表基础上补齐企业级管理字段；硬件未接入前统一保持“待下发”。
ALTER TABLE `camera_capture_plan`
    ADD COLUMN `plan_name` varchar(100) NOT NULL DEFAULT '作物定时采集' COMMENT '计划名称' AFTER `plot_id`,
    ADD COLUMN `weekdays_mask` tinyint unsigned NOT NULL DEFAULT 127 COMMENT '星期位掩码：周一bit0至周日bit6' AFTER `interval_minutes`,
    ADD COLUMN `timezone` varchar(64) NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '计划解释时区，IANA格式' AFTER `end_time`,
    ADD COLUMN `apply_status` tinyint NOT NULL DEFAULT 0 COMMENT '硬件应用状态：0待下发 1下发中 2已应用 3失败' AFTER `enabled`,
    ADD COLUMN `last_apply_error` varchar(500) DEFAULT NULL COMMENT '最近下发失败原因' AFTER `next_capture_time`,
    ADD COLUMN `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁及设备配置版本' AFTER `last_apply_error`,
    ADD COLUMN `create_by` bigint DEFAULT NULL COMMENT '创建用户ID' AFTER `remark`,
    ADD COLUMN `update_by` bigint DEFAULT NULL COMMENT '最后修改用户ID' AFTER `create_by`,
    ADD KEY `idx_camera_capture_plan_dispatch` (`enabled`, `apply_status`, `next_capture_time`),
    ADD CONSTRAINT `chk_camera_capture_plan_weekdays` CHECK (`weekdays_mask` BETWEEN 1 AND 127),
    ADD CONSTRAINT `chk_camera_capture_plan_apply_status` CHECK (`apply_status` IN (0, 1, 2, 3));

-- 新权限与既有设备计划权限保持相同授权边界：管理员和资源所属农场主可管理。
INSERT INTO `permission` (`parent_id`, `permission_name`, `permission_code`, `type`, `path`, `component`, `status`, `sort`)
SELECT 0, '摄像头采集计划', 'camera_capture_plan:manage', 1,
       '/monitor/capture-plan', 'cameraCapturePlan/ListView', 1, 76
WHERE NOT EXISTS (
    SELECT 1 FROM `permission` WHERE `permission_code` = 'camera_capture_plan:manage'
);

INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `role` r
JOIN `permission` p ON p.permission_code = 'camera_capture_plan:manage'
WHERE r.role_code IN ('admin', 'farm_owner');
