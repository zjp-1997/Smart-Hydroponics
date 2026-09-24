/*
 environment_data 表新增风速、气压字段

 说明：
 1. 该脚本用于升级已经存在的 smart_plant 数据库，不会重建 environment_data 表。
 2. wind_speed 记录环境监测设备采集的风速，单位 m/s。
 3. air_pressure 记录环境监测设备采集的气压，单位 hPa。
*/

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_environment_wind_pressure_columns;

DELIMITER //
CREATE PROCEDURE add_environment_wind_pressure_columns()
BEGIN
    -- 字段不存在时再新增，便于本脚本在开发、测试环境重复执行。
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'environment_data'
          AND COLUMN_NAME = 'wind_speed'
    ) THEN
        ALTER TABLE `environment_data`
            ADD COLUMN `wind_speed` decimal(6, 2) NULL DEFAULT NULL COMMENT '风速（m/s）' AFTER `air_humidity`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'environment_data'
          AND COLUMN_NAME = 'air_pressure'
    ) THEN
        ALTER TABLE `environment_data`
            ADD COLUMN `air_pressure` decimal(8, 2) NULL DEFAULT NULL COMMENT '气压（hPa）' AFTER `wind_speed`;
    END IF;
END //
DELIMITER ;

CALL add_environment_wind_pressure_columns();
DROP PROCEDURE IF EXISTS add_environment_wind_pressure_columns;
