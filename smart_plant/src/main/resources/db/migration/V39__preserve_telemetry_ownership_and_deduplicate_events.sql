-- Preserve ownership at collection time. Legacy rows intentionally remain NULL and
-- continue to fall back to the device's current ownership in application queries.
ALTER TABLE `environment_data`
    ADD COLUMN `message_key` VARCHAR(128) NULL COMMENT '设备或协议消息唯一标识；历史/人工数据可为空' AFTER `device_id`,
    ADD COLUMN `owner_user_id` BIGINT NULL COMMENT '采集时设备所属用户ID快照' AFTER `message_key`,
    ADD COLUMN `plot_id_at_collection` BIGINT NULL COMMENT '采集时设备所属地块ID快照' AFTER `owner_user_id`,
    ADD UNIQUE KEY `uk_environment_device_message` (`device_id`, `message_key`),
    ADD KEY `idx_environment_owner_collect` (`owner_user_id`, `collect_time`),
    ADD KEY `idx_environment_plot_collect` (`plot_id_at_collection`, `collect_time`);

ALTER TABLE `water_quality_data`
    ADD COLUMN `message_key` VARCHAR(128) NULL COMMENT '设备或协议消息唯一标识；历史/人工数据可为空' AFTER `device_id`,
    ADD COLUMN `owner_user_id` BIGINT NULL COMMENT '采集时设备所属用户ID快照' AFTER `message_key`,
    ADD COLUMN `plot_id_at_collection` BIGINT NULL COMMENT '采集时设备所属地块ID快照' AFTER `owner_user_id`,
    ADD UNIQUE KEY `uk_water_quality_device_message` (`device_id`, `message_key`),
    ADD KEY `idx_water_quality_owner_collect` (`owner_user_id`, `collect_time`),
    ADD KEY `idx_water_quality_plot_collect` (`plot_id_at_collection`, `collect_time`);

ALTER TABLE `light_data`
    ADD COLUMN `message_key` VARCHAR(128) NULL COMMENT '设备或协议消息唯一标识；历史/人工数据可为空' AFTER `device_id`,
    ADD COLUMN `owner_user_id` BIGINT NULL COMMENT '采集时设备所属用户ID快照' AFTER `message_key`,
    ADD COLUMN `plot_id_at_collection` BIGINT NULL COMMENT '采集时设备所属地块ID快照' AFTER `owner_user_id`,
    ADD UNIQUE KEY `uk_light_device_message` (`device_id`, `message_key`),
    ADD KEY `idx_light_owner_collect` (`owner_user_id`, `collect_time`),
    ADD KEY `idx_light_plot_collect` (`plot_id_at_collection`, `collect_time`);

ALTER TABLE `pump_data`
    ADD COLUMN `message_key` VARCHAR(128) NULL COMMENT '设备或协议消息唯一标识；历史/人工数据可为空' AFTER `device_id`,
    ADD COLUMN `owner_user_id` BIGINT NULL COMMENT '采集时设备所属用户ID快照' AFTER `message_key`,
    ADD COLUMN `plot_id_at_collection` BIGINT NULL COMMENT '采集时设备所属地块ID快照' AFTER `owner_user_id`,
    ADD UNIQUE KEY `uk_pump_device_message` (`device_id`, `message_key`),
    ADD KEY `idx_pump_owner_collect` (`owner_user_id`, `collect_time`),
    ADD KEY `idx_pump_plot_collect` (`plot_id_at_collection`, `collect_time`);

-- Only active records participate in idempotency. Keeping the generated value NULL
-- for soft-deleted rows permits a later event with the same business identity.
ALTER TABLE `alert_event`
    ADD COLUMN `dedup_key` VARCHAR(191) NULL COMMENT '告警业务幂等键' AFTER `source_id`,
    ADD COLUMN `active_dedup_key` VARCHAR(191)
        GENERATED ALWAYS AS (CASE WHEN `status` = 1 THEN `dedup_key` ELSE NULL END) STORED,
    ADD UNIQUE KEY `uk_alert_event_active_dedup` (`active_dedup_key`);

ALTER TABLE `notification`
    ADD COLUMN `dedup_key` VARCHAR(191) NULL COMMENT '通知投递业务幂等键' AFTER `ref_id`,
    ADD COLUMN `active_dedup_key` VARCHAR(191)
        GENERATED ALWAYS AS (CASE WHEN `status` = 1 THEN `dedup_key` ELSE NULL END) STORED,
    ADD UNIQUE KEY `uk_notification_active_dedup` (`active_dedup_key`);
