CREATE TABLE IF NOT EXISTS `model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模型地址ID',
  `model_name` varchar(100) NOT NULL COMMENT '模型展示名称',
  `base_url` varchar(500) NOT NULL COMMENT '模型服务基础地址',
  `api_key` varchar(500) NOT NULL COMMENT '模型服务API Key',
  `model` varchar(100) NOT NULL COMMENT '模型标识',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1启用 0禁用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_name` (`model_name`),
  KEY `idx_model_model` (`model`),
  KEY `idx_model_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型配置表';

CREATE TABLE IF NOT EXISTS `ai_chat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI对话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `model_id` bigint DEFAULT NULL COMMENT '模型ID',
  `image_url` varchar(500) DEFAULT NULL COMMENT '用户上传图片地址',
  `user_content` text NOT NULL COMMENT '用户发送内容',
  `ai_content` text DEFAULT NULL COMMENT 'AI回复内容',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1成功 2失败）',
  `fail_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_chat_user_id` (`user_id`),
  KEY `idx_ai_chat_model_id` (`model_id`),
  KEY `idx_ai_chat_status_time` (`status`, `create_time`),
  CONSTRAINT `fk_ai_chat_user_model_chat` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_chat_model` FOREIGN KEY (`model_id`) REFERENCES `model` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话表';
