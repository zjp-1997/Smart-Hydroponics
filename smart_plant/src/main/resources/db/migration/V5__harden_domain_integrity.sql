-- Domain integrity hardening for the legacy schema.
-- This migration is deliberately data preserving and keeps the public API compatible.

-- 1. Coordinates: longitude/latitude are the only persisted facts. The legacy text
-- column remains available as a generated compatibility projection for all clients.
UPDATE `farm`
SET `longitude` = CAST(TRIM(SUBSTRING_INDEX(REPLACE(`coordinate`, '，', ','), ',', 1)) AS DECIMAL(10,6)),
    `latitude` = CAST(TRIM(SUBSTRING_INDEX(REPLACE(`coordinate`, '，', ','), ',', -1)) AS DECIMAL(10,6))
WHERE (`longitude` IS NULL OR `latitude` IS NULL)
  AND REPLACE(`coordinate`, '，', ',') REGEXP '^[[:space:]]*-?[0-9]+([.][0-9]+)?[[:space:]]*,[[:space:]]*-?[0-9]+([.][0-9]+)?[[:space:]]*$';

UPDATE `plot`
SET `longitude` = CAST(TRIM(SUBSTRING_INDEX(REPLACE(`coordinate`, '，', ','), ',', 1)) AS DECIMAL(10,6)),
    `latitude` = CAST(TRIM(SUBSTRING_INDEX(REPLACE(`coordinate`, '，', ','), ',', -1)) AS DECIMAL(10,6))
WHERE (`longitude` IS NULL OR `latitude` IS NULL)
  AND REPLACE(`coordinate`, '，', ',') REGEXP '^[[:space:]]*-?[0-9]+([.][0-9]+)?[[:space:]]*,[[:space:]]*-?[0-9]+([.][0-9]+)?[[:space:]]*$';

ALTER TABLE `farm`
    DROP COLUMN `coordinate`,
    ADD COLUMN `coordinate` varchar(64)
        GENERATED ALWAYS AS (
            CASE WHEN `longitude` IS NULL OR `latitude` IS NULL THEN NULL
                 ELSE CONCAT(CAST(`longitude` AS CHAR), ',', CAST(`latitude` AS CHAR)) END
        ) STORED COMMENT '只读兼容字段，由 longitude/latitude 生成' AFTER `address`,
    ADD CONSTRAINT `chk_farm_coordinate_pair`
        CHECK ((`longitude` IS NULL AND `latitude` IS NULL)
            OR (`longitude` IS NOT NULL AND `latitude` IS NOT NULL));

ALTER TABLE `plot`
    DROP COLUMN `coordinate`,
    ADD COLUMN `coordinate` varchar(64)
        GENERATED ALWAYS AS (
            CASE WHEN `longitude` IS NULL OR `latitude` IS NULL THEN NULL
                 ELSE CONCAT(CAST(`longitude` AS CHAR), ',', CAST(`latitude` AS CHAR)) END
        ) STORED COMMENT '只读兼容字段，由 longitude/latitude 生成' AFTER `address`,
    ADD CONSTRAINT `chk_plot_coordinate_pair`
        CHECK ((`longitude` IS NULL AND `latitude` IS NULL)
            OR (`longitude` IS NOT NULL AND `latitude` IS NOT NULL));

-- 2. Notification references: ref_type/ref_id are delivery grouping keys, while
-- business entities use explicit foreign-key columns. This removes the previous
-- ambiguity without breaking the API contract used by the web and mobile clients.
ALTER TABLE `notification`
    MODIFY COLUMN `ref_type` varchar(50) NULL COMMENT '投递分组类型，不作为业务外键',
    MODIFY COLUMN `ref_id` bigint NULL COMMENT '投递分组ID，不作为业务外键',
    ADD COLUMN `maintenance_message_id` bigint NULL COMMENT '关联设备维护消息ID' AFTER `alert_id`;

UPDATE `notification`
SET `maintenance_message_id` = `ref_id`
WHERE `ref_type` = 'maintenance_message'
  AND `ref_id` IS NOT NULL;

ALTER TABLE `notification`
    ADD KEY `idx_notification_maintenance_message` (`maintenance_message_id`),
    ADD CONSTRAINT `fk_notification_task`
        FOREIGN KEY (`task_id`) REFERENCES `farm_task` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT `fk_notification_maintenance_message`
        FOREIGN KEY (`maintenance_message_id`) REFERENCES `maintenance_message` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT `chk_notification_ref_pair`
        CHECK ((`ref_type` IS NULL AND `ref_id` IS NULL)
            OR (`ref_type` IS NOT NULL AND `ref_id` IS NOT NULL)),
    ADD CONSTRAINT `chk_notification_ref_type`
        CHECK (`ref_type` IS NULL OR `ref_type` IN
            ('system_announcement', 'farm_task_message', 'farm_task', 'alert_event', 'maintenance_message')),
    ADD CONSTRAINT `chk_notification_task_ref`
        CHECK (`ref_type` NOT IN ('farm_task_message', 'farm_task') OR `task_id` IS NOT NULL),
    ADD CONSTRAINT `chk_notification_alert_ref`
        CHECK (`ref_type` <> 'alert_event' OR `alert_id` IS NOT NULL),
    ADD CONSTRAINT `chk_notification_maintenance_ref`
        CHECK (`ref_type` <> 'maintenance_message' OR `maintenance_message_id` IS NOT NULL);

-- 3. Telemetry facts: reject null/unknown states and physically impossible values
-- before they can enter the operational tables. A unified high-volume telemetry
-- store remains an independent, backwards-compatible evolution step.
UPDATE `environment_data` SET `data_status` = 1 WHERE `data_status` IS NULL;
UPDATE `water_quality_data` SET `data_status` = 1 WHERE `data_status` IS NULL;
UPDATE `light_data` SET `data_status` = 1 WHERE `data_status` IS NULL;
UPDATE `pump_data` SET `data_status` = 1 WHERE `data_status` IS NULL;

ALTER TABLE `environment_data`
    MODIFY COLUMN `data_status` tinyint NOT NULL DEFAULT 1 COMMENT '数据状态（1正常 2异常）',
    ADD CONSTRAINT `chk_environment_data_status` CHECK (`data_status` IN (1,2)),
    ADD CONSTRAINT `chk_environment_humidity` CHECK (`air_humidity` IS NULL OR (`air_humidity` >= 0 AND `air_humidity` <= 100)),
    ADD CONSTRAINT `chk_environment_nonnegative` CHECK (
        (`wind_speed` IS NULL OR `wind_speed` >= 0)
        AND (`air_pressure` IS NULL OR `air_pressure` > 0)
        AND (`co2_concentration` IS NULL OR `co2_concentration` >= 0)
        AND (`pm25` IS NULL OR `pm25` >= 0)
        AND (`cumulative_runtime` IS NULL OR `cumulative_runtime` >= 0));

ALTER TABLE `water_quality_data`
    MODIFY COLUMN `data_status` tinyint NOT NULL DEFAULT 1 COMMENT '数据状态（1正常 2异常）',
    ADD CONSTRAINT `chk_water_quality_data_status` CHECK (`data_status` IN (1,2)),
    ADD CONSTRAINT `chk_water_quality_ph` CHECK (`ph` IS NULL OR (`ph` >= 0 AND `ph` <= 14)),
    ADD CONSTRAINT `chk_water_quality_nonnegative` CHECK (
        (`ec_value` IS NULL OR `ec_value` >= 0)
        AND (`dissolved_oxygen` IS NULL OR `dissolved_oxygen` >= 0)
        AND (`cumulative_runtime` IS NULL OR `cumulative_runtime` >= 0));

ALTER TABLE `light_data`
    MODIFY COLUMN `data_status` tinyint NOT NULL DEFAULT 1 COMMENT '数据状态（1正常 2异常）',
    ADD CONSTRAINT `chk_light_data_status` CHECK (`data_status` IN (1,2)),
    ADD CONSTRAINT `chk_light_nonnegative` CHECK (
        (`light_intensity` IS NULL OR `light_intensity` >= 0)
        AND (`cumulative_runtime` IS NULL OR `cumulative_runtime` >= 0));

ALTER TABLE `pump_data`
    MODIFY COLUMN `data_status` tinyint NOT NULL DEFAULT 1 COMMENT '数据状态（1正常 2异常）',
    ADD CONSTRAINT `chk_pump_data_status` CHECK (`data_status` IN (1,2)),
    ADD CONSTRAINT `chk_pump_nonnegative` CHECK (
        (`water_flow` IS NULL OR `water_flow` >= 0)
        AND (`water_pressure` IS NULL OR `water_pressure` >= 0)
        AND (`cumulative_runtime` IS NULL OR `cumulative_runtime` >= 0));
