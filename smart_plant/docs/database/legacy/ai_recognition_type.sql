/*
  AI识别类型表优化脚本。

  设计说明：
  1. type_code 用于系统和模型侧稳定识别，必须唯一且非空。
  2. type_name 用于页面展示，必须非空。
  3. status 统一使用 1启用、0禁用，并增加 CHECK 约束。
  4. update_time 用于记录最近一次维护时间，便于后台审计。
  5. 预置数据 ID 与 ai_recognition_result.recognition_type 的原枚举值 1-5 对齐。
*/

CREATE TABLE IF NOT EXISTS `ai_recognition_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '识别类型ID',
  `type_code` VARCHAR(50) NOT NULL COMMENT '类型编码',
  `type_name` VARCHAR(100) NOT NULL COMMENT '类型名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（1启用 0禁用）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_recognition_type_code` (`type_code`),
  KEY `idx_ai_recognition_type_name` (`type_name`),
  KEY `idx_ai_recognition_type_status` (`status`),
  CONSTRAINT `chk_ai_recognition_type_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI识别类型表';

INSERT INTO `ai_recognition_type` (`id`, `type_code`, `type_name`, `description`, `status`)
VALUES
  (1, 'CROP_RECOGNITION', '作物识别', '识别图片中的作物品种或作物类别', 1),
  (2, 'DISEASE_RECOGNITION', '病害识别', '识别作物病害类型、症状和风险程度', 1),
  (3, 'PEST_RECOGNITION', '虫害识别', '识别作物虫害类型、虫体或危害特征', 1),
  (4, 'GROWTH_RECOGNITION', '生长状态识别', '识别作物长势、生育阶段和异常生长状态', 1),
  (5, 'COMPREHENSIVE_RECOGNITION', '综合识别', '综合识别作物、病虫害、生长状态和处理建议', 1)
ON DUPLICATE KEY UPDATE
  `type_code` = VALUES(`type_code`),
  `type_name` = VALUES(`type_name`),
  `description` = VALUES(`description`),
  `status` = VALUES(`status`);
