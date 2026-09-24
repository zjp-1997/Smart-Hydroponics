/*
 Navicat migration script for farm management.

 Table relationship:
 user 1 -> N farm 1 -> N plot
 Existing plot rows are kept by using nullable plot.farm_id.
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE IF NOT EXISTS `farm` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '农场ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `farm_name` varchar(100) NOT NULL COMMENT '农场名称',
  `farm_code` varchar(50) DEFAULT NULL COMMENT '农场编号',
  `img_url` varchar(500) DEFAULT NULL COMMENT '农场图片地址',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `coordinate` varchar(64) DEFAULT NULL COMMENT '经纬度（经度,纬度）',
  `total_area` decimal(10,2) DEFAULT NULL COMMENT '农场总面积',
  `area_unit` varchar(20) DEFAULT '亩' COMMENT '面积单位',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '纬度',
  `status` tinyint DEFAULT 1 COMMENT '状态（1正常 0停用）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_farm_code` (`farm_code`) USING BTREE,
  UNIQUE KEY `uk_farm_user_code` (`user_id`, `farm_code`) USING BTREE,
  UNIQUE KEY `uk_farm_id_user` (`id`, `user_id`) USING BTREE,
  KEY `idx_farm_user_id` (`user_id`) USING BTREE,
  KEY `idx_farm_status` (`status`) USING BTREE,
  KEY `idx_farm_name` (`farm_name`) USING BTREE,
  CONSTRAINT `fk_farm_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_farm_total_area` CHECK (`total_area` IS NULL OR `total_area` >= 0),
  CONSTRAINT `chk_farm_status` CHECK (`status` IN (0, 1)),
  CONSTRAINT `chk_farm_longitude` CHECK (`longitude` IS NULL OR (`longitude` >= -180 AND `longitude` <= 180)),
  CONSTRAINT `chk_farm_latitude` CHECK (`latitude` IS NULL OR (`latitude` >= -90 AND `latitude` <= 90))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农场信息表' ROW_FORMAT=Dynamic;

ALTER TABLE `plot`
  ADD COLUMN `farm_id` bigint NULL DEFAULT NULL COMMENT '所属农场ID' AFTER `id`,
  ADD COLUMN `address` varchar(255) NULL DEFAULT NULL COMMENT '地址' AFTER `area_unit`,
  ADD COLUMN `coordinate` varchar(64) NULL DEFAULT NULL COMMENT '经纬度（经度,纬度）' AFTER `address`,
  ADD INDEX `idx_plot_farm_id` (`farm_id`) USING BTREE,
  ADD INDEX `idx_plot_farm_user` (`farm_id`, `user_id`) USING BTREE,
  ADD CONSTRAINT `fk_plot_farm` FOREIGN KEY (`farm_id`) REFERENCES `farm` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  ADD CONSTRAINT `fk_plot_farm_user` FOREIGN KEY (`farm_id`, `user_id`) REFERENCES `farm` (`id`, `user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
