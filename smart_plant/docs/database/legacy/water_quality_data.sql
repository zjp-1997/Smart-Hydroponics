/*
 water_quality_data 水质监测表 & crop 表新增字段

 说明：
 1. 为 crop 表新增 suitable_ec、suitable_dissolved_oxygen 字段，用于记录作物适宜的 EC 值和溶解氧范围。
    水温判定复用 crop 表已有的 suitable_temperature 字段。
 2. 新建 water_quality_data 表，用于存储水质检测设备采集的水质数据。
 3. data_status 字段自动判定规则：
    当水温、PH值、EC值、溶解氧任意一项低于或高于对应作物的适宜范围时，标记为异常（2），否则为正常（1）。
*/

SET NAMES utf8mb4;

-- ----------------------------
-- 1. crop 表新增 suitable_ec、suitable_dissolved_oxygen 字段
-- ----------------------------
ALTER TABLE `crop`
    ADD COLUMN `suitable_ec` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '适宜EC值范围，如1.0-2.5mS/cm' AFTER `suitable_pm25`,
    ADD COLUMN `suitable_dissolved_oxygen` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '适宜溶解氧范围，如5.0-8.0mg/L' AFTER `suitable_ec`;

-- ----------------------------
-- 2. 新建 water_quality_data 水质监测表
-- ----------------------------
DROP TABLE IF EXISTS `water_quality_data`;
CREATE TABLE `water_quality_data` (
    `id`                  bigint         NOT NULL AUTO_INCREMENT COMMENT '水质监测ID',
    `device_id`           bigint         NOT NULL COMMENT '水质监测设备ID，关联 iot_device.id',
    `water_temperature`   decimal(6, 2)  NULL DEFAULT NULL COMMENT '水温（℃）',
    `ph`                  decimal(5, 2)  NULL DEFAULT NULL COMMENT 'PH值',
    `ec_value`            decimal(8, 2)  NULL DEFAULT NULL COMMENT 'EC值（mS/cm）',
    `dissolved_oxygen`    decimal(6, 2)  NULL DEFAULT NULL COMMENT '溶解氧（mg/L）',
    `data_status`         tinyint        NULL DEFAULT 1 COMMENT '数据状态（1正常 2异常），根据作物适宜范围自动判定',
    `abnormal_detail`     varchar(500)   NULL DEFAULT NULL COMMENT '异常详情，记录哪些指标超出范围，如：水温偏高、EC值偏低',
    `cumulative_runtime`  decimal(10, 2) NULL DEFAULT NULL COMMENT '累计运行时长（小时）',
    `collect_time`        datetime       NOT NULL COMMENT '数据采集时间',
    `remark`              varchar(255)   NULL DEFAULT NULL COMMENT '备注',
    `create_time`         datetime       NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_wq_data_device_id` (`device_id` ASC) USING BTREE,
    INDEX `idx_wq_data_data_status` (`data_status` ASC) USING BTREE,
    INDEX `idx_wq_data_collect_time` (`collect_time` ASC) USING BTREE,
    CONSTRAINT `fk_wq_data_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '水质监测数据表' ROW_FORMAT = Dynamic;
