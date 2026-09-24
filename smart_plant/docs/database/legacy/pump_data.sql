/*
 pump_data 水泵监测数据表

 说明：
 1. 新建 pump_data 表，用于存储水泵设备采集的运行数据，包括水流量、水压等。
 2. data_status 字段判定规则：
    当水流量或水压低于或高于设备正常运行范围时，标记为异常（2），否则为正常（1）。
 3. 注意：水温、PH值、EC值、溶解氧的异常判定已在 water_quality_data 表中实现，
    本表专注于水泵运行状态监测（水流量、水压）。
*/

SET NAMES utf8mb4;

-- ----------------------------
-- 新建 pump_data 水泵监测数据表
-- ----------------------------
DROP TABLE IF EXISTS `pump_data`;
CREATE TABLE `pump_data` (
    `id`                  bigint         NOT NULL AUTO_INCREMENT COMMENT '水泵监测ID',
    `device_id`           bigint         NOT NULL COMMENT '水泵设备ID，关联 iot_device.id',
    `water_flow`          decimal(8, 2)  NULL DEFAULT NULL COMMENT '水流量（m³/h）',
    `water_pressure`      decimal(6, 2)  NULL DEFAULT NULL COMMENT '水压（MPa）',
    `data_status`         tinyint        NULL DEFAULT 1 COMMENT '数据状态（1正常 2异常），根据水流量和水压正常运行范围判定',
    `abnormal_detail`     varchar(500)   NULL DEFAULT NULL COMMENT '异常详情，记录哪些指标超出范围，如：水流量偏低、水压偏高',
    `cumulative_runtime`  decimal(10, 2) NULL DEFAULT NULL COMMENT '累计运行时长（小时）',
    `collect_time`        datetime       NOT NULL COMMENT '数据采集时间',
    `remark`              varchar(255)   NULL DEFAULT NULL COMMENT '备注',
    `create_time`         datetime       NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_pump_data_device_id` (`device_id` ASC) USING BTREE,
    INDEX `idx_pump_data_data_status` (`data_status` ASC) USING BTREE,
    INDEX `idx_pump_data_collect_time` (`collect_time` ASC) USING BTREE,
    CONSTRAINT `fk_pump_data_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '水泵监测数据表' ROW_FORMAT = Dynamic;
