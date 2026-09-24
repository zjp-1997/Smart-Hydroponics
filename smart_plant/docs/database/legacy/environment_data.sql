/*
 environment_data 环境监测表 & crop 表新增字段

 说明：
 1. 为 crop 表新增 suitable_co2、suitable_pm25 字段，用于记录作物适宜的 CO2 浓度和 PM2.5 范围。
 2. 新建 environment_data 表，用于存储环境监测设备采集的环境数据。
 3. data_status 字段自动判定规则：
    当空气温度、空气湿度、风速、气压、CO2 浓度、PM2.5 任意一项低于或高于对应作物的适宜范围时，标记为异常（2），否则为正常（1）。
*/

SET NAMES utf8mb4;

-- ----------------------------
-- 1. crop 表新增 suitable_co2、suitable_pm25 字段
-- ----------------------------
ALTER TABLE `crop`
    ADD COLUMN `suitable_co2` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '适宜CO2浓度范围，如400-1000ppm' AFTER `suitable_ph`,
    ADD COLUMN `suitable_pm25` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '适宜PM2.5范围，如0-75μg/m³' AFTER `suitable_co2`;

-- ----------------------------
-- 2. 新建 environment_data 环境监测表
-- ----------------------------
DROP TABLE IF EXISTS `environment_data`;
CREATE TABLE `environment_data` (
    `id`                 bigint        NOT NULL AUTO_INCREMENT COMMENT '环境监测ID',
    `device_id`          bigint        NOT NULL COMMENT '环境监测设备ID，关联 iot_device.id',
    `air_temperature`    decimal(6, 2) NULL DEFAULT NULL COMMENT '空气温度（℃）',
    `air_humidity`       decimal(6, 2) NULL DEFAULT NULL COMMENT '空气湿度（%）',
    `wind_speed`         decimal(6, 2) NULL DEFAULT NULL COMMENT '风速（m/s）',
    `air_pressure`       decimal(8, 2) NULL DEFAULT NULL COMMENT '气压（hPa）',
    `co2_concentration`  decimal(8, 2) NULL DEFAULT NULL COMMENT '二氧化碳浓度（ppm）',
    `pm25`               decimal(8, 2) NULL DEFAULT NULL COMMENT 'PM2.5（μg/m³）',
    `data_status`        tinyint       NULL DEFAULT 1 COMMENT '数据状态（1正常 2异常），根据作物适宜范围自动判定',
    `abnormal_detail`    varchar(500)  NULL DEFAULT NULL COMMENT '异常详情，记录哪些指标超出范围，如：温度偏高、PM2.5超标',
    `cumulative_runtime` decimal(10, 2) NULL DEFAULT NULL COMMENT '累计运行时长（小时）',
    `collect_time`       datetime      NOT NULL COMMENT '数据采集时间',
    `remark`             varchar(255)  NULL DEFAULT NULL COMMENT '备注',
    `create_time`        datetime      NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_env_data_device_id` (`device_id` ASC) USING BTREE,
    INDEX `idx_env_data_data_status` (`data_status` ASC) USING BTREE,
    INDEX `idx_env_data_collect_time` (`collect_time` ASC) USING BTREE,
    CONSTRAINT `fk_env_data_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '环境监测数据表' ROW_FORMAT = Dynamic;
