CREATE TABLE IF NOT EXISTS `consult_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '咨询会话ID',
  `user_id` bigint NOT NULL COMMENT '咨询用户ID',
  `expert_id` bigint NOT NULL COMMENT '专家信息ID',
  `session_no` varchar(50) NOT NULL COMMENT '会话编号',
  `session_title` varchar(100) DEFAULT NULL COMMENT '咨询标题',
  `status` tinyint DEFAULT 1 COMMENT '会话状态（1进行中 2已结束 0删除）',
  `last_message_content` varchar(255) DEFAULT NULL COMMENT '最后一条消息内容',
  `last_message_time` datetime DEFAULT NULL COMMENT '最后消息时间',
  `user_unread_count` int DEFAULT 0 COMMENT '用户未读数量',
  `expert_unread_count` int DEFAULT 0 COMMENT '专家未读数量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consult_session_no` (`session_no`),
  KEY `idx_consult_session_user_id` (`user_id`),
  KEY `idx_consult_session_expert_id` (`expert_id`),
  KEY `idx_consult_session_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='咨询会话表';

CREATE TABLE IF NOT EXISTS `consult_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '咨询消息ID',
  `session_id` bigint NOT NULL COMMENT '咨询会话ID',
  `sender_id` bigint NOT NULL COMMENT '发送人用户ID',
  `receiver_id` bigint NOT NULL COMMENT '接收人用户ID',
  `message_type` tinyint DEFAULT 1 COMMENT '消息类型（1文本 2图片 3语音 4文件）',
  `content` text COMMENT '消息内容',
  `media_url` varchar(500) DEFAULT NULL COMMENT '图片、语音或文件地址',
  `is_read` tinyint DEFAULT 0 COMMENT '是否已读（0未读 1已读）',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `status` tinyint DEFAULT 1 COMMENT '状态（1正常 0删除）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_consult_message_session_time` (`session_id`, `create_time`),
  KEY `idx_consult_message_sender_id` (`sender_id`),
  KEY `idx_consult_message_receiver_read` (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='咨询消息表';
