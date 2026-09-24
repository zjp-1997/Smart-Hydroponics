/*
 light_data 补光灯监测数据表 & crop 表新增字段

 说明：
 1. 为 crop 表新增 suitable_light 字段，用于记录作物适宜的光照强度范围。
 2. 新建 light_data 表，用于存储补光灯设备采集的光照强度数据。
 3. data_status 字段自动判定规则：
    当光照强度低于或高于对应作物的适宜范围时，标记为异常（2），否则为正常（1）。
*/

SET NAMES utf8mb4;

-- ----------------------------
-- 1. crop 表新增 suitable_light 字段
-- ----------------------------
ALTER TABLE `crop`
    ADD COLUMN `suitable_light` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '适宜光照强度范围，如10000-30000lux' AFTER `suitable_dissolved_oxygen`;

-- ----------------------------
-- 2. 新建 light_data 补光灯监测数据表
-- ----------------------------
DROP TABLE IF EXISTS `light_data`;
CREATE TABLE `light_data` (
    `id`                  bigint         NOT NULL AUTO_INCREMENT COMMENT '补光灯监测ID',
    `device_id`           bigint         NOT NULL COMMENT '补光灯设备ID，关联 iot_device.id',
    `light_intensity`     decimal(10, 2) NULL DEFAULT NULL COMMENT '光照强度（lux）',
    `data_status`         tinyint        NULL DEFAULT 1 COMMENT '数据状态（1正常 2异常），根据作物适宜光照范围判定',
    `abnormal_detail`     varchar(500)   NULL DEFAULT NULL COMMENT '异常详情，记录光照强度超出范围的情况，如：光照强度偏低',
    `cumulative_runtime`  decimal(10, 2) NULL DEFAULT NULL COMMENT '累计运行时长（小时）',
    `collect_time`        datetime       NOT NULL COMMENT '数据采集时间',
    `remark`              varchar(255)   NULL DEFAULT NULL COMMENT '备注',
    `create_time`         datetime       NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_light_data_device_id` (`device_id` ASC) USING BTREE,
    INDEX `idx_light_data_data_status` (`data_status` ASC) USING BTREE,
    INDEX `idx_light_data_collect_time` (`collect_time` ASC) USING BTREE,
    CONSTRAINT `fk_light_data_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '补光灯监测数据表' ROW_FORMAT = Dynamic;
