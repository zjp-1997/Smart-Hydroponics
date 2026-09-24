-- 设备计划是“期望配置”，硬件接入前保持待下发状态；不得用 iot_device.control_status 代替计划执行结果。
CREATE TABLE IF NOT EXISTS `iot_device_plan` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备计划ID',
    `device_id` bigint NOT NULL COMMENT '设备ID，一台设备仅保留一份当前计划',
    `collection_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用周期采集：0否 1是',
    `collection_interval_minutes` int NOT NULL DEFAULT 60 COMMENT '采集间隔，单位分钟',
    `control_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否启用自动控制时段：0否 1是',
    `timezone` varchar(64) NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '计划解释时区，IANA格式',
    `effective_from` date DEFAULT NULL COMMENT '计划生效日期，为空表示立即生效',
    `effective_to` date DEFAULT NULL COMMENT '计划失效日期，为空表示长期有效',
    `apply_status` tinyint NOT NULL DEFAULT 0 COMMENT '硬件应用状态：0待下发 1下发中 2已应用 3失败',
    `last_applied_at` datetime DEFAULT NULL COMMENT '最近硬件确认时间',
    `last_apply_error` varchar(500) DEFAULT NULL COMMENT '最近下发失败原因',
    `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁及设备配置版本',
    `create_by` bigint DEFAULT NULL COMMENT '创建用户ID',
    `update_by` bigint DEFAULT NULL COMMENT '最后修改用户ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_iot_device_plan_device` (`device_id`),
    KEY `idx_iot_device_plan_apply_status` (`apply_status`),
    CONSTRAINT `fk_iot_device_plan_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `chk_iot_device_plan_collection_enabled` CHECK (`collection_enabled` IN (0, 1)),
    CONSTRAINT `chk_iot_device_plan_collection_interval` CHECK (`collection_interval_minutes` BETWEEN 1 AND 1440),
    CONSTRAINT `chk_iot_device_plan_control_enabled` CHECK (`control_enabled` IN (0, 1)),
    CONSTRAINT `chk_iot_device_plan_apply_status` CHECK (`apply_status` IN (0, 1, 2, 3)),
    CONSTRAINT `chk_iot_device_plan_effective_date` CHECK (`effective_to` IS NULL OR `effective_from` IS NULL OR `effective_to` >= `effective_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IoT设备采集与控制计划';

CREATE TABLE IF NOT EXISTS `iot_device_schedule` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备运行时段ID',
    `plan_id` bigint NOT NULL COMMENT '所属设备计划ID',
    `schedule_name` varchar(100) NOT NULL COMMENT '时段名称',
    `weekdays_mask` tinyint unsigned NOT NULL DEFAULT 127 COMMENT '星期位掩码：周一bit0至周日bit6',
    `start_time` time NOT NULL COMMENT '开启时间',
    `end_time` time NOT NULL COMMENT '关闭时间，早于开始时间表示跨日',
    `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：0否 1是',
    `sort_order` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_iot_device_schedule_plan` (`plan_id`, `enabled`, `sort_order`),
    CONSTRAINT `fk_iot_device_schedule_plan` FOREIGN KEY (`plan_id`) REFERENCES `iot_device_plan` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `chk_iot_device_schedule_weekdays` CHECK (`weekdays_mask` BETWEEN 1 AND 127),
    CONSTRAINT `chk_iot_device_schedule_enabled` CHECK (`enabled` IN (0, 1)),
    CONSTRAINT `chk_iot_device_schedule_time` CHECK (`start_time` <> `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='补光灯和水泵周期运行时段';

CREATE TABLE IF NOT EXISTS `iot_device_command_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备指令记录ID',
    `device_id` bigint NOT NULL COMMENT '目标设备ID',
    `plan_id` bigint DEFAULT NULL COMMENT '来源计划ID',
    `schedule_id` bigint DEFAULT NULL COMMENT '来源时段ID',
    `idempotency_key` varchar(100) NOT NULL COMMENT '指令幂等键',
    `command_type` varchar(32) NOT NULL COMMENT '指令类型：SET_INTERVAL、TURN_ON、TURN_OFF、SYNC_PLAN',
    `command_payload` json DEFAULT NULL COMMENT '设备协议适配前的标准指令载荷',
    `planned_at` datetime NOT NULL COMMENT '计划执行时间',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0待发送 1已发送 2已确认 3失败 4已取消',
    `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
    `sent_at` datetime DEFAULT NULL COMMENT '发送时间',
    `ack_at` datetime DEFAULT NULL COMMENT '设备确认时间',
    `error_message` varchar(500) DEFAULT NULL COMMENT '失败原因',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_iot_device_command_idempotency` (`idempotency_key`),
    KEY `idx_iot_device_command_dispatch` (`status`, `planned_at`),
    KEY `idx_iot_device_command_device` (`device_id`, `planned_at`),
    CONSTRAINT `fk_iot_device_command_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_iot_device_command_plan` FOREIGN KEY (`plan_id`) REFERENCES `iot_device_plan` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT `fk_iot_device_command_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `iot_device_schedule` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT `chk_iot_device_command_status` CHECK (`status` IN (0, 1, 2, 3, 4)),
    CONSTRAINT `chk_iot_device_command_retry` CHECK (`retry_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备指令下发与回执审计记录';

-- 权限通过业务编码幂等写入，避免依赖不同环境中的自增主键。
INSERT INTO `permission` (`parent_id`, `permission_name`, `permission_code`, `type`, `path`, `component`, `status`, `sort`)
SELECT 0, '设备计划管理', 'iot_device_plan:manage', 1, '/device/plan', 'devicePlan/ListView', 1, 73
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `permission_code` = 'iot_device_plan:manage');

INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `role` r
JOIN `permission` p ON p.permission_code = 'iot_device_plan:manage'
WHERE r.role_code IN ('admin', 'farm_owner');
