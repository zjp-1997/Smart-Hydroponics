/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '登录日志ID',
  `admin_id` bigint DEFAULT NULL COMMENT '管理员ID',
  `username` varchar(50) NOT NULL COMMENT '登录用户名',
  `success` tinyint NOT NULL COMMENT '是否成功（1成功 0失败）',
  `failure_reason` varchar(100) DEFAULT NULL COMMENT '失败原因',
  `ip` varchar(50) DEFAULT NULL COMMENT '登录IP',
  `user_agent` varchar(255) DEFAULT NULL COMMENT 'User-Agent',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_login_log_admin_id` (`admin_id`),
  KEY `idx_admin_login_log_username` (`username`),
  KEY `idx_admin_login_log_ip` (`ip`),
  KEY `idx_admin_login_log_time` (`login_time`),
  CONSTRAINT `chk_admin_login_log_success` CHECK ((`success` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员登录日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI对话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `model_id` bigint DEFAULT NULL COMMENT '模型ID',
  `image_url` varchar(500) DEFAULT NULL COMMENT '用户上传图片地址',
  `user_content` text NOT NULL COMMENT '用户发送内容',
  `ai_content` text COMMENT 'AI回复内容',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1成功 2失败）',
  `fail_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_chat_user_id` (`user_id`),
  KEY `idx_ai_chat_model_id` (`model_id`),
  KEY `idx_ai_chat_status_time` (`status`,`create_time`),
  CONSTRAINT `fk_ai_chat_model` FOREIGN KEY (`model_id`) REFERENCES `model` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_chat_user_model_chat` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI消息ID',
  `session_id` bigint NOT NULL COMMENT 'AI会话ID',
  `role` tinyint NOT NULL COMMENT '消息角色（1用户 2AI助手 3系统）',
  `content` text NOT NULL COMMENT '消息内容',
  `message_type` tinyint DEFAULT '1' COMMENT '消息类型（1文本 2图片 3语音 4文件）',
  `media_url` varchar(255) DEFAULT NULL COMMENT '图片、语音或文件地址',
  `model_name` varchar(100) DEFAULT NULL COMMENT '模型名称',
  `model_version` varchar(50) DEFAULT NULL COMMENT '模型版本',
  `prompt_tokens` int DEFAULT '0' COMMENT '输入Token数量',
  `completion_tokens` int DEFAULT '0' COMMENT '输出Token数量',
  `total_tokens` int DEFAULT '0' COMMENT '总Token数量',
  `status` tinyint DEFAULT '1' COMMENT '消息状态（1正常 2生成失败 0删除）',
  `fail_reason` varchar(255) DEFAULT NULL COMMENT '生成失败原因',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_chat_message_session_id` (`session_id`),
  KEY `idx_ai_chat_message_role` (`role`),
  KEY `idx_ai_chat_message_create_time` (`create_time`),
  KEY `idx_ai_message_session_time` (`session_id`,`create_time`),
  CONSTRAINT `fk_ai_chat_message_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_ai_chat_message_role` CHECK ((`role` in (1,2,3))),
  CONSTRAINT `chk_ai_chat_message_status` CHECK ((`status` in (0,1,2))),
  CONSTRAINT `chk_ai_chat_message_tokens` CHECK (((`prompt_tokens` >= 0) and (`completion_tokens` >= 0) and (`total_tokens` >= 0))),
  CONSTRAINT `chk_ai_chat_message_type` CHECK ((`message_type` in (1,2,3,4)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI消息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI会话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_no` varchar(50) NOT NULL COMMENT 'AI会话编号',
  `session_title` varchar(100) DEFAULT NULL COMMENT '会话标题',
  `scene` tinyint DEFAULT '1' COMMENT '会话场景（1通用问答 2智能策略 3病虫害咨询 4识别结果追问 5农事建议）',
  `plot_id` bigint DEFAULT NULL COMMENT '关联地块ID，可为空',
  `batch_id` bigint DEFAULT NULL COMMENT '关联种植批次ID，可为空',
  `crop_id` bigint DEFAULT NULL COMMENT '关联作物ID，可为空',
  `recognition_result_id` bigint DEFAULT NULL COMMENT '关联智能识别结果ID，可为空',
  `last_message_content` varchar(255) DEFAULT NULL COMMENT '最后一条消息内容',
  `last_message_time` datetime DEFAULT NULL COMMENT '最后消息时间',
  `status` tinyint DEFAULT '1' COMMENT '会话状态（1进行中 2已结束 0删除）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `session_no` (`session_no`),
  KEY `idx_ai_chat_session_user_id` (`user_id`),
  KEY `idx_ai_chat_session_scene` (`scene`),
  KEY `idx_ai_chat_session_plot_id` (`plot_id`),
  KEY `idx_ai_chat_session_batch_id` (`batch_id`),
  KEY `idx_ai_chat_session_crop_id` (`crop_id`),
  KEY `idx_ai_chat_session_recognition_result_id` (`recognition_result_id`),
  KEY `idx_ai_chat_session_last_message_time` (`last_message_time`),
  CONSTRAINT `fk_ai_chat_session_batch` FOREIGN KEY (`batch_id`) REFERENCES `planting_batch` (`id`),
  CONSTRAINT `fk_ai_chat_session_crop` FOREIGN KEY (`crop_id`) REFERENCES `crop` (`id`),
  CONSTRAINT `fk_ai_chat_session_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`),
  CONSTRAINT `fk_ai_chat_session_recognition_result` FOREIGN KEY (`recognition_result_id`) REFERENCES `ai_recognition_result` (`id`),
  CONSTRAINT `fk_ai_chat_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_ai_chat_session_scene` CHECK ((`scene` in (1,2,3,4,5))),
  CONSTRAINT `chk_ai_chat_session_status` CHECK ((`status` in (0,1,2)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI会话表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_recognition_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI识别记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `source_type` tinyint NOT NULL DEFAULT '1' COMMENT '识别来源 1手动上传 2摄像头自动识别',
  `crop_image_id` bigint DEFAULT NULL COMMENT '手动上传图片ID，可为空',
  `camera_id` bigint DEFAULT NULL COMMENT '摄像头ID，自动识别时必填',
  `plot_id` bigint DEFAULT NULL COMMENT '地块ID，自动识别时必填',
  `capture_id` bigint DEFAULT NULL COMMENT '摄像头采集记录ID，自动识别时可关联',
  `image_url` varchar(500) NOT NULL COMMENT '识别图片地址',
  `thumbnail_url` varchar(500) DEFAULT NULL COMMENT '缩略图地址',
  `image_size` bigint DEFAULT NULL COMMENT '图片大小(字节)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '记录状态 1待识别 2识别中 3已完成 4识别失败',
  `recognition_start_time` datetime DEFAULT NULL COMMENT '识别开始时间',
  `recognition_end_time` datetime DEFAULT NULL COMMENT '识别结束时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_record_user_id` (`user_id`),
  KEY `idx_ai_record_source_type` (`source_type`),
  KEY `idx_ai_record_crop_image_id` (`crop_image_id`),
  KEY `idx_ai_record_camera_id` (`camera_id`),
  KEY `idx_ai_record_plot_id` (`plot_id`),
  KEY `idx_ai_record_capture_id` (`capture_id`),
  KEY `idx_ai_record_status_time` (`status`,`create_time`),
  CONSTRAINT `fk_ai_record_camera` FOREIGN KEY (`camera_id`) REFERENCES `camera_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_record_capture` FOREIGN KEY (`capture_id`) REFERENCES `camera_capture_record` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_record_crop_image` FOREIGN KEY (`crop_image_id`) REFERENCES `crop_image` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_record_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_ai_record_source_relation` CHECK ((((`source_type` = 1) and (`camera_id` is null) and (`plot_id` is null)) or ((`source_type` = 2) and (`camera_id` is not null) and (`plot_id` is not null)))),
  CONSTRAINT `chk_ai_record_source_type` CHECK ((`source_type` in (1,2))),
  CONSTRAINT `chk_ai_record_status` CHECK ((`status` in (1,2,3,4)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI识别记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_recognition_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '智能识别结果ID',
  `record_id` bigint NOT NULL COMMENT 'AI识别记录ID',
  `crop_id` bigint DEFAULT NULL COMMENT '作物ID，可为空，模型识别后可回填',
  `recognition_type` bigint NOT NULL DEFAULT '5' COMMENT '识别类型ID，关联ai_recognition_type.id',
  `result_name` varchar(100) DEFAULT NULL COMMENT '识别结果名称，如生菜、霜霉病',
  `result_summary` varchar(500) DEFAULT NULL COMMENT '识别结果摘要',
  `result_detail` text COMMENT '识别结果详情',
  `disease_pest_id` bigint DEFAULT NULL COMMENT '关联病虫害ID，识别出病虫害时使用',
  `confidence` decimal(5,2) DEFAULT NULL COMMENT '识别置信度，百分比',
  `severity_level` tinyint DEFAULT NULL COMMENT '严重程度（1轻微 2中等 3严重）',
  `suggestion` text COMMENT '处理建议',
  `model_name` varchar(100) DEFAULT NULL COMMENT '模型名称',
  `model_version` varchar(50) DEFAULT NULL COMMENT '模型版本',
  `status` tinyint DEFAULT '3' COMMENT '识别状态（1成功 2失败 3处理中）',
  `fail_reason` varchar(500) DEFAULT NULL COMMENT '识别失败原因',
  `recognize_time` datetime DEFAULT NULL COMMENT '识别完成时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_result_crop_id` (`crop_id`),
  KEY `idx_ai_result_type` (`recognition_type`),
  KEY `idx_ai_result_status` (`status`),
  KEY `idx_ai_result_time` (`recognize_time`),
  KEY `idx_ai_result_disease_pest_id` (`disease_pest_id`),
  KEY `idx_ai_result_record_id` (`record_id`),
  CONSTRAINT `fk_ai_result_crop` FOREIGN KEY (`crop_id`) REFERENCES `crop` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_result_disease_pest` FOREIGN KEY (`disease_pest_id`) REFERENCES `disease_pest` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_result_record` FOREIGN KEY (`record_id`) REFERENCES `ai_recognition_record` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_result_type` FOREIGN KEY (`recognition_type`) REFERENCES `ai_recognition_type` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_ai_result_confidence` CHECK (((`confidence` is null) or ((`confidence` >= 0) and (`confidence` <= 100)))),
  CONSTRAINT `chk_ai_result_severity` CHECK (((`severity_level` is null) or (`severity_level` in (1,2,3)))),
  CONSTRAINT `chk_ai_result_status` CHECK ((`status` in (1,2,3)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能识别结果表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_recognition_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '识别类型ID',
  `type_code` varchar(50) NOT NULL COMMENT '类型编码',
  `type_name` varchar(100) NOT NULL COMMENT '类型名称',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1启用 0禁用）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_recognition_type_code` (`type_code`),
  KEY `idx_ai_recognition_type_name` (`type_name`),
  KEY `idx_ai_recognition_type_status` (`status`),
  CONSTRAINT `chk_ai_recognition_type_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI识别类型表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_solution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI solution id',
  `record_id` bigint NOT NULL COMMENT 'AI recognition record id',
  `result_id` bigint NOT NULL COMMENT 'AI recognition result id',
  `user_id` bigint NOT NULL COMMENT 'User id',
  `plot_id` bigint DEFAULT NULL COMMENT 'Plot id',
  `camera_id` bigint DEFAULT NULL COMMENT 'Camera id',
  `solution_title` varchar(120) NOT NULL COMMENT 'Solution title',
  `solution_summary` varchar(500) DEFAULT NULL COMMENT 'Solution summary',
  `solution_detail` text COMMENT 'Solution detail',
  `priority_level` tinyint DEFAULT '2' COMMENT 'Priority: 1 low, 2 normal, 3 high, 4 urgent',
  `generate_task` tinyint DEFAULT '0' COMMENT 'Whether task generation is requested',
  `task_generated` tinyint DEFAULT '0' COMMENT 'Whether farm task was generated',
  `farm_task_id` bigint DEFAULT NULL COMMENT 'Generated farm task id',
  `status` tinyint DEFAULT '1' COMMENT 'Status: 1 active, 2 task generated, 3 archived',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  KEY `idx_ai_solution_record` (`record_id`),
  KEY `idx_ai_solution_result` (`result_id`),
  KEY `idx_ai_solution_user` (`user_id`),
  KEY `idx_ai_solution_plot` (`plot_id`),
  KEY `idx_ai_solution_camera` (`camera_id`),
  KEY `idx_ai_solution_task` (`farm_task_id`),
  KEY `idx_ai_solution_status_time` (`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI recognition solution table';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alert_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '预警事件ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `plot_id` bigint DEFAULT NULL COMMENT '所属地块ID',
  `device_id` bigint DEFAULT NULL COMMENT '关联设备ID',
  `batch_id` bigint DEFAULT NULL COMMENT '关联种植批次ID',
  `alert_type` tinyint NOT NULL COMMENT '预警类型（1环境异常 2设备异常 3农事任务逾期 4摄像头异常 5AI识别异常 6库存预警）',
  `alert_title` varchar(100) NOT NULL COMMENT '预警标题',
  `alert_content` text COMMENT '预警内容',
  `metric_code` varchar(50) DEFAULT NULL COMMENT '异常指标编码，如temperature/humidity/ph/ec/do',
  `metric_name` varchar(50) DEFAULT NULL COMMENT '异常指标名称，如温度、湿度、PH',
  `metric_value` decimal(10,3) DEFAULT NULL COMMENT '异常指标值',
  `metric_unit` varchar(20) DEFAULT NULL COMMENT '指标单位',
  `threshold_min` decimal(10,3) DEFAULT NULL COMMENT '阈值下限',
  `threshold_max` decimal(10,3) DEFAULT NULL COMMENT '阈值上限',
  `alert_level` tinyint DEFAULT '1' COMMENT '预警等级（1普通 2重要 3紧急）',
  `source_type` varchar(50) DEFAULT NULL COMMENT '来源类型，如iot_sensor_data/iot_device/farm_task/camera_capture_record/ai_recognition_record',
  `source_id` bigint DEFAULT NULL COMMENT '来源数据ID',
  `process_status` tinyint DEFAULT '1' COMMENT '处理状态（1未处理 2处理中 3已处理 4已忽略）',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人用户ID',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_result` varchar(255) DEFAULT NULL COMMENT '处理结果',
  `trigger_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
  `status` tinyint DEFAULT '1' COMMENT '状态（1正常 0删除）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_alert_user_id` (`user_id`),
  KEY `idx_alert_plot_id` (`plot_id`),
  KEY `idx_alert_device_id` (`device_id`),
  KEY `idx_alert_batch_id` (`batch_id`),
  KEY `idx_alert_type` (`alert_type`),
  KEY `idx_alert_level` (`alert_level`),
  KEY `idx_alert_process_status` (`process_status`),
  KEY `idx_alert_trigger_time` (`trigger_time`),
  KEY `idx_alert_source` (`source_type`,`source_id`),
  KEY `fk_alert_handler` (`handler_id`),
  KEY `idx_alert_user_process_time` (`user_id`,`process_status`,`trigger_time`),
  KEY `idx_alert_batch_user_plot` (`batch_id`,`user_id`,`plot_id`),
  CONSTRAINT `fk_alert_batch` FOREIGN KEY (`batch_id`) REFERENCES `planting_batch` (`id`),
  CONSTRAINT `fk_alert_batch_user_plot` FOREIGN KEY (`batch_id`, `user_id`, `plot_id`) REFERENCES `planting_batch` (`id`, `user_id`, `plot_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_alert_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_alert_handler` FOREIGN KEY (`handler_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_alert_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`),
  CONSTRAINT `fk_alert_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_alert_level` CHECK ((`alert_level` in (1,2,3))),
  CONSTRAINT `chk_alert_metric_value` CHECK (((`threshold_min` is null) or (`threshold_max` is null) or (`threshold_max` >= `threshold_min`))),
  CONSTRAINT `chk_alert_process_status` CHECK ((`process_status` in (1,2,3,4))),
  CONSTRAINT `chk_alert_status` CHECK ((`status` in (0,1))),
  CONSTRAINT `chk_alert_type` CHECK ((`alert_type` in (1,2,3,4,5,6)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预警事件表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `camera_capture_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '摄像头拍照计划ID',
  `device_id` bigint NOT NULL COMMENT '摄像头设备ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `plot_id` bigint NOT NULL COMMENT '所属地块ID',
  `interval_minutes` int DEFAULT '60' COMMENT '拍照间隔，单位分钟',
  `start_time` time DEFAULT NULL COMMENT '每日开始时间',
  `end_time` time DEFAULT NULL COMMENT '每日结束时间',
  `enabled` tinyint DEFAULT '1' COMMENT '是否启用（1启用 0停用）',
  `last_capture_time` datetime DEFAULT NULL COMMENT '上次拍照时间',
  `next_capture_time` datetime DEFAULT NULL COMMENT '下次拍照时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_capture_plan_device_id` (`device_id`),
  KEY `idx_capture_plan_enabled` (`enabled`),
  KEY `idx_capture_plan_next_time` (`next_capture_time`),
  KEY `fk_capture_plan_user` (`user_id`),
  KEY `fk_capture_plan_plot` (`plot_id`),
  KEY `idx_capture_plan_device_user_plot` (`device_id`,`user_id`,`plot_id`),
  CONSTRAINT `fk_capture_plan_camera_device` FOREIGN KEY (`device_id`) REFERENCES `camera_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_capture_plan_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`),
  CONSTRAINT `fk_capture_plan_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_capture_plan_enabled` CHECK ((`enabled` in (0,1))),
  CONSTRAINT `chk_capture_plan_interval` CHECK ((`interval_minutes` > 0)),
  CONSTRAINT `chk_capture_plan_time` CHECK (((`end_time` is null) or (`start_time` is null) or (`end_time` > `start_time`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='摄像头拍照计划表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `camera_capture_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '摄像头拍照记录ID',
  `device_id` bigint NOT NULL COMMENT '摄像头设备ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `plot_id` bigint NOT NULL COMMENT '所属地块ID',
  `batch_id` bigint DEFAULT NULL COMMENT '种植批次ID',
  `image_url` varchar(500) NOT NULL COMMENT '图片地址',
  `thumbnail_url` varchar(500) DEFAULT NULL COMMENT '缩略图地址',
  `image_size` bigint DEFAULT NULL COMMENT '图片大小(字节)',
  `capture_type` tinyint DEFAULT '1' COMMENT '拍照类型（1定时拍照 2手动拍照 3告警抓拍）',
  `capture_time` datetime NOT NULL COMMENT '拍照时间',
  `ai_checked` tinyint DEFAULT '0' COMMENT '是否已进行AI识别（1是 0否）',
  `status` tinyint DEFAULT '1' COMMENT '状态（1正常 0删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_camera_record_device_id` (`device_id`),
  KEY `idx_camera_record_plot_id` (`plot_id`),
  KEY `idx_camera_record_batch_id` (`batch_id`),
  KEY `idx_camera_record_capture_time` (`capture_time`),
  KEY `fk_camera_record_user` (`user_id`),
  KEY `idx_camera_record_device_time` (`device_id`,`capture_time`),
  KEY `idx_camera_record_device_user_plot` (`device_id`,`user_id`,`plot_id`),
  KEY `idx_camera_record_batch_user_plot` (`batch_id`,`user_id`,`plot_id`),
  CONSTRAINT `fk_camera_record_batch` FOREIGN KEY (`batch_id`) REFERENCES `planting_batch` (`id`),
  CONSTRAINT `fk_camera_record_batch_user_plot` FOREIGN KEY (`batch_id`, `user_id`, `plot_id`) REFERENCES `planting_batch` (`id`, `user_id`, `plot_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_camera_record_camera_device` FOREIGN KEY (`device_id`) REFERENCES `camera_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_camera_record_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`),
  CONSTRAINT `fk_camera_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_camera_record_ai_checked` CHECK ((`ai_checked` in (0,1))),
  CONSTRAINT `chk_camera_record_status` CHECK ((`status` in (0,1))),
  CONSTRAINT `chk_camera_record_type` CHECK ((`capture_type` in (1,2,3)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='摄像头拍照记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `camera_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '监控设备ID',
  `device_id` bigint NOT NULL COMMENT '原 iot_device.id，迁移后保留的来源设备编号',
  `plot_id` bigint NOT NULL COMMENT '所属地块',
  `name` varchar(100) DEFAULT NULL COMMENT '监控设备名称',
  `stream_protocol` varchar(20) DEFAULT NULL COMMENT 'RTSP/GB28181/HTTP',
  `stream_url` varchar(500) DEFAULT NULL COMMENT '视频流地址',
  `snapshot_url` varchar(500) DEFAULT NULL COMMENT '截图地址',
  `resolution` varchar(50) DEFAULT NULL COMMENT '1920x1080',
  `online_status` tinyint DEFAULT '0' COMMENT '在线状态 0离线 1在线',
  `direction` varchar(50) DEFAULT NULL COMMENT '监控方向',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_camera_device_device_id` (`device_id`),
  KEY `idx_camera_device_plot_id` (`plot_id`),
  KEY `idx_camera_device_online_status` (`online_status`),
  CONSTRAINT `fk_camera_device_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_camera_device_online_status` CHECK (((`online_status` is null) or (`online_status` in (0,1))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监控设备表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `consult_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '咨询消息ID',
  `session_id` bigint NOT NULL COMMENT '咨询会话ID',
  `sender_id` bigint NOT NULL COMMENT '发送人用户ID',
  `receiver_id` bigint NOT NULL COMMENT '接收人用户ID',
  `message_type` tinyint DEFAULT '1' COMMENT '消息类型（1文本 2图片 3语音 4文件）',
  `content` text COMMENT '消息内容',
  `media_url` varchar(500) DEFAULT NULL COMMENT '图片、语音或文件地址',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读（0未读 1已读）',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `status` tinyint DEFAULT '1' COMMENT '状态（1正常 0删除）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_consult_message_session_id` (`session_id`),
  KEY `idx_consult_message_sender_id` (`sender_id`),
  KEY `idx_consult_message_receiver_id` (`receiver_id`),
  KEY `idx_consult_message_is_read` (`is_read`),
  KEY `idx_consult_message_create_time` (`create_time`),
  KEY `idx_consult_message_session_time` (`session_id`,`create_time`),
  KEY `idx_consult_message_receiver_read` (`receiver_id`,`is_read`),
  CONSTRAINT `fk_consult_message_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_consult_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_consult_message_session` FOREIGN KEY (`session_id`) REFERENCES `consult_session` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_consult_message_read` CHECK ((`is_read` in (0,1))),
  CONSTRAINT `chk_consult_message_status` CHECK ((`status` in (0,1))),
  CONSTRAINT `chk_consult_message_type` CHECK ((`message_type` in (1,2,3,4)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='咨询消息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `consult_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '咨询会话ID',
  `user_id` bigint NOT NULL COMMENT '咨询用户ID',
  `expert_id` bigint NOT NULL COMMENT '专家信息ID',
  `session_no` varchar(50) NOT NULL COMMENT '会话编号',
  `session_title` varchar(100) DEFAULT NULL COMMENT '咨询标题',
  `status` tinyint DEFAULT '1' COMMENT '会话状态（1进行中 2已结束 3已取消）',
  `last_message_content` varchar(255) DEFAULT NULL COMMENT '最后一条消息内容',
  `last_message_time` datetime DEFAULT NULL COMMENT '最后消息时间',
  `user_unread_count` int DEFAULT '0' COMMENT '用户未读数量',
  `expert_unread_count` int DEFAULT '0' COMMENT '专家未读数量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `session_no` (`session_no`),
  KEY `idx_consult_session_user_id` (`user_id`),
  KEY `idx_consult_session_expert_id` (`expert_id`),
  KEY `idx_consult_session_status` (`status`),
  KEY `idx_consult_session_last_message_time` (`last_message_time`),
  CONSTRAINT `fk_consult_session_expert` FOREIGN KEY (`expert_id`) REFERENCES `expert_profile` (`id`),
  CONSTRAINT `fk_consult_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_consult_status` CHECK ((`status` in (1,2,3))),
  CONSTRAINT `chk_consult_unread_count` CHECK (((`user_unread_count` >= 0) and (`expert_unread_count` >= 0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='咨询会话表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `crop` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '作物ID',
  `user_id` bigint DEFAULT NULL COMMENT '所属用户ID，空值表示系统公共作物',
  `type_id` bigint DEFAULT NULL COMMENT '作物类型ID',
  `crop_name` varchar(50) NOT NULL COMMENT '作物名称，如生菜、上海青',
  `crop_code` varchar(32) DEFAULT NULL COMMENT '作物编码',
  `variety` varchar(100) DEFAULT NULL COMMENT '品种名称',
  `growth_days` int DEFAULT NULL COMMENT '推荐生长周期，单位天',
  `suitable_temperature` varchar(50) DEFAULT NULL COMMENT '适宜温度范围，如15-25℃',
  `suitable_humidity` varchar(50) DEFAULT NULL COMMENT '适宜湿度范围，如60%-80%',
  `suitable_ph` varchar(50) DEFAULT NULL COMMENT '适宜PH范围，如6.0-7.5',
  `suitable_co2` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '适宜CO2浓度范围，如400-1000ppm',
  `suitable_pm25` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '适宜PM2.5范围，如0-75μg/m³',
  `suitable_ec` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '适宜EC值范围，如1.0-2.5mS/cm',
  `suitable_dissolved_oxygen` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '适宜溶解氧范围，如5.0-8.0mg/L',
  `suitable_light` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '适宜光照强度范围，如10000-30000lux',
  `image_url` varchar(255) DEFAULT NULL COMMENT '作物图片',
  `description` text COMMENT '作物说明',
  `status` tinyint DEFAULT '1' COMMENT '状态（1启用 0停用）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crop_code` (`crop_code`),
  KEY `idx_crop_name` (`crop_name`),
  KEY `idx_crop_status` (`status`),
  KEY `idx_crop_type_id` (`type_id`) USING BTREE,
  KEY `idx_crop_user_id` (`user_id`),
  CONSTRAINT `fk_crop_type` FOREIGN KEY (`type_id`) REFERENCES `crop_type` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `chk_crop_growth_days` CHECK (((`growth_days` is null) or (`growth_days` > 0))),
  CONSTRAINT `chk_crop_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作物表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `crop_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `user_id` bigint NOT NULL COMMENT '上传用户ID',
  `image_url` varchar(500) NOT NULL COMMENT '图片地址',
  `image_size` bigint DEFAULT NULL COMMENT '图片大小(字节)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作物图片管理表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `crop_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '类型ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父类型ID',
  `type_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
  `status` tinyint DEFAULT '1' COMMENT '状态（1启用 0停用）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_crop_type_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_crop_type_name` (`type_name`) USING BTREE,
  KEY `idx_crop_type_status` (`status`) USING BTREE,
  CONSTRAINT `fk_crop_type_parent` FOREIGN KEY (`parent_id`) REFERENCES `crop_type` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='作物类型表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Device type id',
  `type_code` varchar(50) NOT NULL COMMENT 'Unique type code',
  `type_name` varchar(100) NOT NULL COMMENT 'Type name',
  `category` tinyint NOT NULL COMMENT 'Device category 1 sensor 2 actuator',
  `description` varchar(255) DEFAULT NULL COMMENT 'Description',
  `status` tinyint DEFAULT '1' COMMENT 'Status 1 enabled 0 disabled',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_code` (`type_code`),
  KEY `idx_device_type_category` (`category`),
  KEY `idx_device_type_status` (`status`),
  CONSTRAINT `chk_device_type_category` CHECK ((`category` in (1,2))),
  CONSTRAINT `chk_device_type_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备类型表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `disease_control` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '防治措施ID',
  `disease_id` bigint NOT NULL COMMENT '病虫害ID',
  `control_type` tinyint NOT NULL COMMENT '防治类型（1预防 2治疗）',
  `method` text COMMENT '措施内容',
  `drug_name` varchar(100) DEFAULT NULL COMMENT '药剂名称',
  `usage_method` text COMMENT '使用方法',
  `suitable_stage` varchar(100) DEFAULT NULL COMMENT '适用阶段',
  `status` tinyint DEFAULT '1' COMMENT '状态（1启用 0停用）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_disease_control_disease_id` (`disease_id`),
  KEY `idx_disease_control_type` (`control_type`),
  KEY `idx_disease_control_status` (`status`),
  CONSTRAINT `fk_disease_control_disease` FOREIGN KEY (`disease_id`) REFERENCES `disease_pest` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_disease_control_status` CHECK ((`status` in (0,1))),
  CONSTRAINT `chk_disease_control_type` CHECK ((`control_type` in (1,2)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='病虫害防治措施表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `disease_pest` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '病虫害ID',
  `crop_type_id` bigint DEFAULT NULL COMMENT '鍏宠仈浣滅墿绫诲瀷ID',
  `name` varchar(100) NOT NULL COMMENT '病虫害名称',
  `type` tinyint NOT NULL COMMENT '类型（1病害 2虫害 3生理性病害）',
  `symptom` text COMMENT '症状描述',
  `cause` text COMMENT '发生原因',
  `suitable_stage` varchar(100) DEFAULT NULL COMMENT '易发生生长阶段',
  `severity_level` tinyint DEFAULT '1' COMMENT '危害等级（1轻微 2中等 3严重）',
  `cover_image` varchar(255) DEFAULT NULL COMMENT '封面图片',
  `status` tinyint DEFAULT '1' COMMENT '状态（1启用 0停用）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_disease_pest_type` (`type`),
  KEY `idx_disease_pest_name` (`name`),
  KEY `idx_disease_pest_status` (`status`),
  KEY `idx_disease_pest_crop_type_id` (`crop_type_id`),
  CONSTRAINT `fk_disease_pest_crop_type` FOREIGN KEY (`crop_type_id`) REFERENCES `crop_type` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_disease_pest_severity` CHECK ((`severity_level` in (1,2,3))),
  CONSTRAINT `chk_disease_pest_status` CHECK ((`status` in (0,1))),
  CONSTRAINT `chk_disease_pest_type` CHECK ((`type` in (1,2,3)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='病虫害知识库表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `environment_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '环境监测ID',
  `device_id` bigint NOT NULL COMMENT '环境监测设备ID，关联 iot_device.id',
  `air_temperature` decimal(6,2) DEFAULT NULL COMMENT '空气温度（℃）',
  `air_humidity` decimal(6,2) DEFAULT NULL COMMENT '空气湿度（%）',
  `wind_speed` decimal(6,2) DEFAULT NULL COMMENT '风速（m/s）',
  `air_pressure` decimal(8,2) DEFAULT NULL COMMENT '气压（hPa）',
  `co2_concentration` decimal(8,2) DEFAULT NULL COMMENT '二氧化碳浓度（ppm）',
  `pm25` decimal(8,2) DEFAULT NULL COMMENT 'PM2.5（μg/m³）',
  `data_status` tinyint DEFAULT '1' COMMENT '数据状态（1正常 2异常），根据作物适宜范围自动判定',
  `abnormal_detail` varchar(500) DEFAULT NULL COMMENT '异常详情，记录哪些指标超出范围，如：温度偏高、PM2.5超标',
  `cumulative_runtime` decimal(10,2) DEFAULT NULL COMMENT '累计运行时长（小时）',
  `collect_time` datetime NOT NULL COMMENT '数据采集时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_env_data_device_id` (`device_id`) USING BTREE,
  KEY `idx_env_data_data_status` (`data_status`) USING BTREE,
  KEY `idx_env_data_collect_time` (`collect_time`) USING BTREE,
  KEY `idx_env_data_device_time` (`device_id`,`collect_time`),
  CONSTRAINT `fk_env_data_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='环境监测数据表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_ed_check_device_type_bi` BEFORE INSERT ON `environment_data` FOR EACH ROW BEGIN DECLARE v_count INT DEFAULT 0; SELECT COUNT(1) INTO v_count FROM `iot_device` d JOIN `device_type` dt ON dt.id = d.type_id WHERE d.id = NEW.device_id AND dt.type_code = 'ENV_SENSOR'; IF v_count = 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'environment_data device must be ENV_SENSOR'; END IF; END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_ed_check_device_type_bu` BEFORE UPDATE ON `environment_data` FOR EACH ROW BEGIN DECLARE v_count INT DEFAULT 0; SELECT COUNT(1) INTO v_count FROM `iot_device` d JOIN `device_type` dt ON dt.id = d.type_id WHERE d.id = NEW.device_id AND dt.type_code = 'ENV_SENSOR'; IF v_count = 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'environment_data device must be ENV_SENSOR'; END IF; END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `expert_audit_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '审核记录ID',
  `expert_id` bigint NOT NULL COMMENT '专家信息ID',
  `audit_status` tinyint NOT NULL COMMENT '审核结果（1待审核 2审核通过 3审核拒绝）',
  `audit_opinion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '审核意见',
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人用户ID',
  `audit_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_expert_audit_expert_id` (`expert_id`) USING BTREE,
  KEY `fk_expert_audit_auditor` (`auditor_id`),
  KEY `idx_expert_audit_status` (`audit_status`) USING BTREE,
  CONSTRAINT `fk_expert_audit_auditor` FOREIGN KEY (`auditor_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_expert_audit_profile` FOREIGN KEY (`expert_id`) REFERENCES `expert_profile` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='专家入驻审核记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `expert_certificate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '专家证书ID',
  `expert_id` bigint NOT NULL COMMENT '专家信息ID',
  `certificate_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '资质证书图片地址',
  `is_expired` tinyint DEFAULT '0' COMMENT '是否过期（1已过期 0未过期）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_expert_certificate_expert_id` (`expert_id`) USING BTREE,
  CONSTRAINT `fk_expert_certificate_profile` FOREIGN KEY (`expert_id`) REFERENCES `expert_profile` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='专家证书表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `expert_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '专家详情ID',
  `expert_id` bigint NOT NULL COMMENT '专家信息ID',
  `specialty` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '擅长方向，如病虫害防治、水培种植、蔬菜栽培',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '专家简介',
  `research_direction` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '研究方向',
  `work_years` int DEFAULT NULL COMMENT '从业年限',
  `service_scope` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '服务范围',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_expert_detail_expert_id` (`expert_id`) USING BTREE,
  CONSTRAINT `fk_expert_detail_profile` FOREIGN KEY (`expert_id`) REFERENCES `expert_profile` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='专家扩展详情表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `expert_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '专家信息ID',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `institution_id` bigint DEFAULT NULL COMMENT '所属学校或机构ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '专家登录用户名',
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '专家登录密码密文',
  `real_name` varchar(50) NOT NULL COMMENT '专家真实姓名',
  `job_title` varchar(50) DEFAULT NULL COMMENT '职称，如高级农艺师、教授',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '专家手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '专家邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '专家头像，可默认取用户头像',
  `rating` decimal(3,2) DEFAULT '5.00' COMMENT '评分',
  `review_count` int DEFAULT '0' COMMENT '评价数量',
  `consultation_count` int DEFAULT '0' COMMENT '咨询次数',
  `audit_status` tinyint DEFAULT '1' COMMENT '审核状态（1待审核 2审核通过 3审核拒绝）',
  `audit_pass_time` datetime DEFAULT NULL COMMENT '审核通过时间',
  `service_status` tinyint DEFAULT '1' COMMENT '服务状态（1可咨询 0暂停咨询）',
  `status` tinyint DEFAULT '1' COMMENT '状态（1正常 0禁用）',
  `consultation_status` tinyint DEFAULT '1' COMMENT '咨询状态（1可咨询 0暂停咨询）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_expert_profile_user_id` (`user_id`),
  UNIQUE KEY `uk_expert_profile_username` (`username`) USING BTREE,
  KEY `idx_expert_profile_real_name` (`real_name`),
  KEY `idx_expert_profile_audit_status` (`audit_status`),
  KEY `idx_expert_profile_service_status` (`service_status`),
  KEY `idx_expert_profile_status` (`status`),
  KEY `idx_expert_profile_institution_id` (`institution_id`) USING BTREE,
  KEY `idx_expert_profile_consultation_status` (`consultation_status`) USING BTREE,
  KEY `idx_expert_profile_audit_pass_time` (`audit_pass_time`),
  CONSTRAINT `fk_expert_profile_institution` FOREIGN KEY (`institution_id`) REFERENCES `institution` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_expert_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_expert_audit_status` CHECK ((`audit_status` in (1,2,3))),
  CONSTRAINT `chk_expert_consultation_count` CHECK ((`consultation_count` >= 0)),
  CONSTRAINT `chk_expert_rating` CHECK (((`rating` is null) or ((`rating` >= 0) and (`rating` <= 5)))),
  CONSTRAINT `chk_expert_service_status` CHECK ((`service_status` in (0,1))),
  CONSTRAINT `chk_expert_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专家基础信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `expert_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '专家评价ID',
  `expert_id` bigint NOT NULL COMMENT '专家信息ID',
  `user_id` bigint DEFAULT NULL COMMENT '评价用户ID',
  `rating` decimal(2,1) NOT NULL COMMENT '本次评价评分',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '评价内容',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_expert_review_expert_id` (`expert_id`) USING BTREE,
  KEY `idx_expert_review_user_id` (`user_id`) USING BTREE,
  CONSTRAINT `fk_expert_review_profile` FOREIGN KEY (`expert_id`) REFERENCES `expert_profile` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_expert_review_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='专家评价表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `farm` (
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
  `status` tinyint DEFAULT '1' COMMENT '状态（1正常 0停用）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_farm_id_user` (`id`,`user_id`) USING BTREE,
  UNIQUE KEY `uk_farm_user_code` (`user_id`,`farm_code`) USING BTREE,
  UNIQUE KEY `uk_farm_code` (`farm_code`),
  KEY `idx_farm_user_id` (`user_id`) USING BTREE,
  KEY `idx_farm_status` (`status`) USING BTREE,
  KEY `idx_farm_name` (`farm_name`) USING BTREE,
  CONSTRAINT `fk_farm_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_farm_latitude` CHECK (((`latitude` is null) or ((`latitude` >= -(90)) and (`latitude` <= 90)))),
  CONSTRAINT `chk_farm_longitude` CHECK (((`longitude` is null) or ((`longitude` >= -(180)) and (`longitude` <= 180)))),
  CONSTRAINT `chk_farm_status` CHECK ((`status` in (0,1))),
  CONSTRAINT `chk_farm_total_area` CHECK (((`total_area` is null) or (`total_area` >= 0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='农场信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `farm_owner_technician` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '绑定ID',
  `owner_user_id` bigint NOT NULL COMMENT '农场主用户ID',
  `technician_user_id` bigint NOT NULL COMMENT '技术人员用户ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1启用 0停用）',
  `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner_technician` (`owner_user_id`,`technician_user_id`),
  KEY `idx_fot_owner` (`owner_user_id`),
  KEY `idx_fot_technician` (`technician_user_id`),
  KEY `idx_fot_status` (`status`),
  CONSTRAINT `fk_fot_owner_user` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_fot_technician_user` FOREIGN KEY (`technician_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_fot_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农场主技术人员绑定表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `farm_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '农事任务ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `plot_id` bigint NOT NULL COMMENT '所属地块ID',
  `batch_id` bigint DEFAULT NULL COMMENT '种植批次ID',
  `task_title` varchar(100) NOT NULL COMMENT '任务标题',
  `task_type` tinyint NOT NULL COMMENT '任务类型（1浇水 2施肥 3打药 4采收 5巡检 6除草 7补光 8其他）',
  `task_content` text COMMENT '任务内容',
  `priority` tinyint DEFAULT '2' COMMENT '优先级（1低 2普通 3高 4紧急）',
  `planned_start_time` datetime DEFAULT NULL COMMENT '计划开始时间',
  `planned_end_time` datetime DEFAULT NULL COMMENT '计划结束时间',
  `actual_start_time` datetime DEFAULT NULL COMMENT '实际开始时间',
  `actual_end_time` datetime DEFAULT NULL COMMENT '实际完成时间',
  `status` tinyint DEFAULT '1' COMMENT '任务状态（1未开始 2进行中 3已完成 4已逾期 5已取消）',
  `executor_id` bigint DEFAULT NULL COMMENT '执行人用户ID',
  `complete_remark` varchar(255) DEFAULT NULL COMMENT '完成备注',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `source_type` varchar(50) DEFAULT NULL COMMENT 'Task source type',
  `source_id` bigint DEFAULT NULL COMMENT 'Task source id',
  `ai_solution_id` bigint DEFAULT NULL COMMENT 'AI solution id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_farm_task_user_id` (`user_id`),
  KEY `idx_farm_task_plot_id` (`plot_id`),
  KEY `idx_farm_task_batch_id` (`batch_id`),
  KEY `idx_farm_task_type` (`task_type`),
  KEY `idx_farm_task_status` (`status`),
  KEY `idx_farm_task_plan_time` (`planned_start_time`,`planned_end_time`),
  KEY `idx_farm_task_executor_id` (`executor_id`),
  KEY `idx_farm_task_user_status_time` (`user_id`,`status`,`planned_start_time`),
  KEY `idx_farm_task_batch_user_plot` (`batch_id`,`user_id`,`plot_id`),
  KEY `idx_farm_task_source` (`source_type`,`source_id`),
  KEY `idx_farm_task_ai_solution` (`ai_solution_id`),
  CONSTRAINT `fk_farm_task_batch` FOREIGN KEY (`batch_id`) REFERENCES `planting_batch` (`id`),
  CONSTRAINT `fk_farm_task_batch_user_plot` FOREIGN KEY (`batch_id`, `user_id`, `plot_id`) REFERENCES `planting_batch` (`id`, `user_id`, `plot_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_farm_task_executor` FOREIGN KEY (`executor_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_farm_task_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`),
  CONSTRAINT `fk_farm_task_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_task_actual_time` CHECK (((`actual_end_time` is null) or (`actual_start_time` is null) or (`actual_end_time` >= `actual_start_time`))),
  CONSTRAINT `chk_task_plan_time` CHECK (((`planned_end_time` is null) or (`planned_start_time` is null) or (`planned_end_time` >= `planned_start_time`))),
  CONSTRAINT `chk_task_priority` CHECK ((`priority` in (1,2,3,4))),
  CONSTRAINT `chk_task_status` CHECK ((`status` in (1,2,3,4,5))),
  CONSTRAINT `chk_task_type` CHECK ((`task_type` in (1,2,3,4,5,6,7,8)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农事任务表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `farm_task_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `task_id` bigint NOT NULL COMMENT '农事任务ID',
  `operator_id` bigint NOT NULL COMMENT '操作人用户ID',
  `action_type` tinyint NOT NULL COMMENT '操作类型（1创建 2开始 3完成 4取消 5修改 6逾期）',
  `action_content` varchar(255) DEFAULT NULL COMMENT '操作内容',
  `before_status` tinyint DEFAULT NULL COMMENT '操作前状态',
  `after_status` tinyint DEFAULT NULL COMMENT '操作后状态',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_log_task_id` (`task_id`),
  KEY `idx_task_log_operator_id` (`operator_id`),
  KEY `idx_task_log_action_type` (`action_type`),
  CONSTRAINT `fk_task_log_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_task_log_task` FOREIGN KEY (`task_id`) REFERENCES `farm_task` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_task_log_action_type` CHECK ((`action_type` in (1,2,3,4,5,6)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农事任务日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `farm_task_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Farm task execution record id',
  `task_id` bigint NOT NULL COMMENT 'Farm task id',
  `operator_id` bigint NOT NULL COMMENT 'Operator user id',
  `action_type` tinyint NOT NULL COMMENT 'Action type: 1 start, 2 complete, 3 feedback, 4 optimize',
  `action_content` varchar(500) DEFAULT NULL COMMENT 'Action content',
  `result_status` tinyint DEFAULT NULL COMMENT 'Result status: 1 valid, 2 partial, 3 invalid',
  `feedback_score` tinyint DEFAULT NULL COMMENT 'Feedback score 1-5',
  `feedback_detail` text COMMENT 'Feedback detail',
  `optimize_suggestion` text COMMENT 'Optimization suggestion',
  `before_status` tinyint DEFAULT NULL COMMENT 'Task status before action',
  `after_status` tinyint DEFAULT NULL COMMENT 'Task status after action',
  `execute_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Execution time',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (`id`),
  KEY `idx_farm_task_record_task` (`task_id`),
  KEY `idx_farm_task_record_operator` (`operator_id`),
  KEY `idx_farm_task_record_action` (`action_type`),
  KEY `idx_farm_task_record_time` (`execute_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农事任务记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `growth_stage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '阶段ID',
  `crop_id` bigint NOT NULL COMMENT '作物ID',
  `stage_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '阶段名称（发芽期/幼苗期等）',
  `stage_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '阶段编码',
  `stage_order` int NOT NULL COMMENT '阶段顺序（1,2,3...）',
  `start_day` int DEFAULT NULL COMMENT '开始天数，从播种或定植第几天开始',
  `end_day` int DEFAULT NULL COMMENT '结束天数，从播种或定植第几天结束',
  `duration` int DEFAULT NULL COMMENT '持续天数',
  `light_hours` decimal(4,1) DEFAULT NULL COMMENT '建议光照时长（小时/天）',
  `temp_min` decimal(4,1) DEFAULT NULL COMMENT '最低适宜温度',
  `temp_max` decimal(4,1) DEFAULT NULL COMMENT '最高适宜温度',
  `humidity_min` decimal(5,2) DEFAULT NULL COMMENT '最低适宜湿度（%）',
  `humidity_max` decimal(5,2) DEFAULT NULL COMMENT '最高适宜湿度（%）',
  `ph_min` decimal(3,1) DEFAULT NULL COMMENT '最低适宜PH',
  `ph_max` decimal(3,1) DEFAULT NULL COMMENT '最高适宜PH',
  `ec_min` decimal(4,2) DEFAULT NULL COMMENT '最低适宜EC值',
  `ec_max` decimal(4,2) DEFAULT NULL COMMENT '最高适宜EC值',
  `water_interval_days` int DEFAULT NULL COMMENT '建议浇水间隔天数',
  `fertilizer_interval_days` int DEFAULT NULL COMMENT '建议施肥间隔天数',
  `management_advice` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '管理建议',
  `status` tinyint DEFAULT '1' COMMENT '状态（1启用 0停用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_growth_stage_crop_order` (`crop_id`,`stage_order`) USING BTREE,
  KEY `idx_growth_stage_crop_id` (`crop_id`) USING BTREE,
  KEY `idx_growth_stage_name` (`stage_name`) USING BTREE,
  KEY `idx_growth_stage_status` (`status`) USING BTREE,
  CONSTRAINT `fk_growth_stage_crop` FOREIGN KEY (`crop_id`) REFERENCES `crop` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='作物生长期表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `institution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '机构ID',
  `institution_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '学校或机构名称',
  `institution_type` tinyint DEFAULT '1' COMMENT '机构类型（1学校 2科研机构 3企业 4其他）',
  `contact_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系电话',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '机构地址',
  `status` tinyint DEFAULT '1' COMMENT '状态（1正常 0禁用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_institution_name` (`institution_name`) USING BTREE,
  KEY `idx_institution_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='学校或机构表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  `plot_id` bigint NOT NULL COMMENT '关联地块ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `device_code` varchar(64) NOT NULL COMMENT '设备唯一编码',
  `name` varchar(100) NOT NULL COMMENT '设备名称',
  `type_id` bigint NOT NULL COMMENT '设备类型ID',
  `control_status` tinyint NOT NULL DEFAULT '0' COMMENT '控制状态 0关闭 1开启',
  `online_status` tinyint NOT NULL DEFAULT '0' COMMENT '在线状态 0离线 1在线',
  `health_status` tinyint NOT NULL DEFAULT '0' COMMENT '健康状态 0正常 1故障 2维护',
  `install_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '璁惧?瀹夎?鏃堕棿',
  `offline_time` datetime DEFAULT NULL COMMENT '鏈?繎涓??绂荤嚎鏃堕棿',
  `fault_time` datetime DEFAULT NULL COMMENT '鏈?繎涓??鏁呴殰鎴栫淮鎶ゆ椂闂',
  `last_heartbeat_time` datetime DEFAULT NULL COMMENT '最后心跳时间',
  `last_online_time` datetime DEFAULT NULL COMMENT '最后在线时间',
  `online_duration` bigint NOT NULL DEFAULT '0' COMMENT '绱??鍦ㄧ嚎鏃堕暱锛屽崟浣嶇?',
  `last_data_time` datetime DEFAULT NULL COMMENT '最后数据时间',
  `location` varchar(255) DEFAULT NULL COMMENT '设备安装或所在位置',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iot_device_code` (`device_code`),
  UNIQUE KEY `uk_iot_device_id_plot_user` (`id`,`plot_id`,`user_id`),
  KEY `idx_iot_device_heartbeat` (`last_heartbeat_time`),
  KEY `idx_iot_device_plot_id` (`plot_id`),
  KEY `idx_iot_device_control_status` (`control_status`),
  KEY `idx_iot_device_online_status` (`online_status`),
  KEY `idx_iot_device_health_status` (`health_status`),
  KEY `idx_iot_device_type_id` (`type_id`),
  KEY `idx_iot_device_user_id` (`user_id`),
  KEY `idx_iot_device_plot_user` (`plot_id`,`user_id`),
  KEY `idx_iot_device_install_time` (`install_time`),
  KEY `idx_iot_device_offline_time` (`offline_time`),
  KEY `idx_iot_device_fault_time` (`fault_time`),
  CONSTRAINT `fk_iot_device_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_iot_device_plot_user` FOREIGN KEY (`plot_id`, `user_id`) REFERENCES `plot` (`id`, `user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_iot_device_type` FOREIGN KEY (`type_id`) REFERENCES `device_type` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_iot_device_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_iot_device_control_status_num` CHECK ((`control_status` in (0,1))),
  CONSTRAINT `chk_iot_device_fault_time_after_install` CHECK (((`fault_time` is null) or (`fault_time` >= `install_time`))),
  CONSTRAINT `chk_iot_device_health_status_num` CHECK ((`health_status` in (0,1,2))),
  CONSTRAINT `chk_iot_device_offline_time_after_install` CHECK (((`offline_time` is null) or (`offline_time` >= `install_time`))),
  CONSTRAINT `chk_iot_device_online_duration_nonnegative` CHECK ((`online_duration` >= 0)),
  CONSTRAINT `chk_iot_device_online_status_num` CHECK ((`online_status` in (0,1))),
  CONSTRAINT `chk_iot_device_unhealthy_offline_closed` CHECK (((`health_status` not in (1,2)) or ((`control_status` = 0) and (`online_status` = 0))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_device_fault` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '故障ID',
  `device_id` bigint NOT NULL COMMENT '设备ID',
  `fault_code` varchar(64) NOT NULL COMMENT '故障编码',
  `fault_name` varchar(100) NOT NULL COMMENT '故障名称',
  `fault_type` tinyint NOT NULL COMMENT '故障类型 1硬件 2软件 3通信 4环境',
  `severity` tinyint NOT NULL DEFAULT '1' COMMENT '严重程度 1低 2中 3高',
  `fault_desc` varchar(255) DEFAULT NULL COMMENT '故障描述',
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '故障开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '故障结束时间',
  `duration` int DEFAULT NULL COMMENT '持续时间（秒）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0未处理 1处理中 2已恢复',
  `assign_status` tinyint NOT NULL DEFAULT '0' COMMENT '分配状态（0未分配 1已分配待接受 2已接受 3已拒绝）',
  `handle_user_id` bigint DEFAULT NULL COMMENT '处理人',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_result` varchar(255) DEFAULT NULL COMMENT '处理结果',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_status` (`status`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_fault_type` (`fault_type`),
  KEY `idx_severity` (`severity`),
  KEY `fk_fault_handle_user` (`handle_user_id`),
  KEY `idx_fault_assign_status` (`assign_status`),
  CONSTRAINT `fk_fault_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_fault_handle_user` FOREIGN KEY (`handle_user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `chk_fault_assign_status` CHECK ((`assign_status` in (0,1,2,3))),
  CONSTRAINT `chk_fault_duration` CHECK (((`duration` is null) or (`duration` >= 0))),
  CONSTRAINT `chk_fault_severity` CHECK ((`severity` in (1,2,3))),
  CONSTRAINT `chk_fault_status` CHECK ((`status` in (0,1,2))),
  CONSTRAINT `chk_fault_time` CHECK (((`end_time` is null) or (`end_time` >= `start_time`))),
  CONSTRAINT `chk_fault_type` CHECK ((`fault_type` in (1,2,3,4)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备故障记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_iot_device_fault_bi` BEFORE INSERT ON `iot_device_fault` FOR EACH ROW BEGIN
  IF NEW.start_time IS NULL THEN SET NEW.start_time = NOW(); END IF;
  IF NEW.status = 2 AND NEW.end_time IS NULL THEN SET NEW.end_time = NOW(); END IF;
  IF NEW.end_time IS NOT NULL THEN
    SET NEW.duration = TIMESTAMPDIFF(SECOND, NEW.start_time, NEW.end_time);
    IF NEW.status <> 2 THEN SET NEW.status = 2; END IF;
  END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_iot_device_fault_ai` AFTER INSERT ON `iot_device_fault` FOR EACH ROW BEGIN
  UPDATE `iot_device` d
  SET d.health_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN 1
        WHEN d.health_status = 1 THEN 0
        ELSE d.health_status
      END,
      d.control_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.control_status
      END,
      d.online_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.online_status
      END,
      d.offline_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN COALESCE(d.offline_time, NOW())
        ELSE d.offline_time
      END,
      d.fault_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN COALESCE(d.fault_time, NOW())
        ELSE d.fault_time
      END
  WHERE d.id = NEW.device_id;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_iot_device_fault_bu` BEFORE UPDATE ON `iot_device_fault` FOR EACH ROW BEGIN
  IF NEW.status = 2 AND NEW.end_time IS NULL THEN SET NEW.end_time = NOW(); END IF;
  IF NEW.end_time IS NOT NULL THEN
    SET NEW.duration = TIMESTAMPDIFF(SECOND, NEW.start_time, NEW.end_time);
    IF NEW.status <> 2 THEN SET NEW.status = 2; END IF;
  END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_iot_device_fault_au` AFTER UPDATE ON `iot_device_fault` FOR EACH ROW BEGIN
  UPDATE `iot_device` d
  SET d.health_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN 1
        WHEN d.health_status = 1 THEN 0
        ELSE d.health_status
      END,
      d.control_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.control_status
      END,
      d.online_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.online_status
      END,
      d.offline_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN COALESCE(d.offline_time, NOW())
        ELSE d.offline_time
      END,
      d.fault_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN COALESCE(d.fault_time, NOW())
        ELSE NULL
      END
  WHERE d.id = OLD.device_id;

  UPDATE `iot_device` d
  SET d.health_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN 1
        WHEN d.health_status = 1 THEN 0
        ELSE d.health_status
      END,
      d.control_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.control_status
      END,
      d.online_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.online_status
      END,
      d.offline_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN COALESCE(d.offline_time, NOW())
        ELSE d.offline_time
      END,
      d.fault_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN COALESCE(d.fault_time, NOW())
        ELSE NULL
      END
  WHERE d.id = NEW.device_id;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_iot_device_fault_ad` AFTER DELETE ON `iot_device_fault` FOR EACH ROW BEGIN
  UPDATE `iot_device` d
  SET d.health_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN 1
        WHEN d.health_status = 1 THEN 0
        ELSE d.health_status
      END,
      d.control_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.control_status
      END,
      d.online_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.online_status
      END,
      d.offline_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN COALESCE(d.offline_time, NOW())
        ELSE d.offline_time
      END,
      d.fault_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN COALESCE(d.fault_time, NOW())
        ELSE NULL
      END
  WHERE d.id = OLD.device_id;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `light_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '补光灯监测ID',
  `device_id` bigint NOT NULL COMMENT '补光灯设备ID，关联 iot_device.id',
  `light_intensity` decimal(10,2) DEFAULT NULL COMMENT '光照强度（lux）',
  `data_status` tinyint DEFAULT '1' COMMENT '数据状态（1正常 2异常），根据作物适宜光照范围判定',
  `abnormal_detail` varchar(500) DEFAULT NULL COMMENT '异常详情，记录光照强度超出范围的情况，如：光照强度偏低',
  `cumulative_runtime` decimal(10,2) DEFAULT NULL COMMENT '累计运行时长（小时）',
  `collect_time` datetime NOT NULL COMMENT '数据采集时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_light_data_device_id` (`device_id`) USING BTREE,
  KEY `idx_light_data_data_status` (`data_status`) USING BTREE,
  KEY `idx_light_data_collect_time` (`collect_time`) USING BTREE,
  KEY `idx_light_data_device_time` (`device_id`,`collect_time`),
  CONSTRAINT `fk_light_data_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='补光灯监测数据表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_ld_check_device_type_bi` BEFORE INSERT ON `light_data` FOR EACH ROW BEGIN DECLARE v_count INT DEFAULT 0; SELECT COUNT(1) INTO v_count FROM `iot_device` d JOIN `device_type` dt ON dt.id = d.type_id WHERE d.id = NEW.device_id AND dt.type_code = 'GROW_LIGHT'; IF v_count = 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'light_data device must be GROW_LIGHT'; END IF; END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_ld_check_device_type_bu` BEFORE UPDATE ON `light_data` FOR EACH ROW BEGIN DECLARE v_count INT DEFAULT 0; SELECT COUNT(1) INTO v_count FROM `iot_device` d JOIN `device_type` dt ON dt.id = d.type_id WHERE d.id = NEW.device_id AND dt.type_code = 'GROW_LIGHT'; IF v_count = 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'light_data device must be GROW_LIGHT'; END IF; END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `maintenance_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '维护消息ID',
  `fault_id` bigint DEFAULT NULL COMMENT '自动消息来源故障ID，人工发布为空',
  `owner_id` bigint NOT NULL COMMENT '设备所属农场主ID',
  `plot_id` bigint NOT NULL COMMENT '故障发生地块ID',
  `plot_name` varchar(100) NOT NULL COMMENT '发布时地块名称快照',
  `title` varchar(100) NOT NULL COMMENT '消息标题',
  `content` text NOT NULL COMMENT '消息正文快照',
  `level` tinyint NOT NULL DEFAULT '1' COMMENT '1普通 2重要 3紧急',
  `publisher_name` varchar(50) NOT NULL DEFAULT '系统自动发布' COMMENT '自动消息发布人',
  `send_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `device_id` bigint DEFAULT NULL COMMENT '关联设备ID',
  `device_name` varchar(100) DEFAULT NULL COMMENT '设备名称快照',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1正常 0逻辑删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '编辑版本号',
  `publisher_id` bigint DEFAULT NULL COMMENT '人工发布人ID',
  `content_customized` tinyint NOT NULL DEFAULT '0' COMMENT '0默认正文 1自定义正文',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_maintenance_fault` (`fault_id`),
  KEY `idx_maintenance_owner_time` (`owner_id`,`send_time`),
  KEY `idx_maintenance_plot` (`plot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备故障维护消息';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模型地址ID',
  `model_name` varchar(100) NOT NULL COMMENT '模型展示名称',
  `base_url` varchar(500) NOT NULL COMMENT '模型服务基础地址',
  `api_key` varchar(500) NOT NULL COMMENT '模型服务API Key',
  `model` varchar(100) NOT NULL COMMENT '模型标识',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1启用 0禁用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_name` (`model_name`),
  KEY `idx_model_model` (`model`),
  KEY `idx_model_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息通知ID',
  `user_id` bigint NOT NULL COMMENT '接收用户ID',
  `title` varchar(100) NOT NULL COMMENT '消息标题',
  `content` text COMMENT '消息内容',
  `notice_type` tinyint NOT NULL COMMENT '消息类型（1系统消息 2农事任务 3设备消息 4环境预警 5专家咨询 6AI识别 7库存预警）',
  `ref_type` varchar(50) DEFAULT NULL COMMENT '关联业务类型，如alert_event/farm_task/iot_device/consult_session',
  `ref_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `task_id` bigint DEFAULT NULL COMMENT '关联农事任务ID',
  `alert_id` bigint DEFAULT NULL COMMENT '关联预警事件ID',
  `level` tinyint DEFAULT '1' COMMENT '消息级别（1普通 2重要 3紧急）',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读（0未读 1已读）',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `send_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `publisher_id` bigint DEFAULT NULL COMMENT '发布人用户ID',
  `publisher_name` varchar(50) DEFAULT NULL COMMENT '发布人名称',
  `status` tinyint DEFAULT '1' COMMENT '状态（1正常 0删除）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_notification_user_id` (`user_id`),
  KEY `idx_notification_notice_type` (`notice_type`),
  KEY `idx_notification_is_read` (`is_read`),
  KEY `idx_notification_send_time` (`send_time`),
  KEY `idx_notification_ref` (`ref_type`,`ref_id`),
  KEY `idx_notification_alert_id` (`alert_id`),
  KEY `idx_notification_user_read_time` (`user_id`,`is_read`,`send_time`),
  KEY `idx_notification_publisher_id` (`publisher_id`),
  KEY `idx_notification_task_id` (`task_id`),
  CONSTRAINT `fk_notification_alert` FOREIGN KEY (`alert_id`) REFERENCES `alert_event` (`id`),
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_notification_level` CHECK ((`level` in (1,2,3))),
  CONSTRAINT `chk_notification_read` CHECK ((`is_read` in (0,1))),
  CONSTRAINT `chk_notification_status` CHECK ((`status` in (0,1))),
  CONSTRAINT `chk_notification_type` CHECK ((`notice_type` in (1,2,3,4,5,6,7)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息通知表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '操作日志ID',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人',
  `operation` varchar(100) NOT NULL COMMENT '操作内容',
  `ip` varchar(50) DEFAULT NULL COMMENT '操作IP',
  `operate_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_operator` (`operator_id`),
  KEY `idx_operation_log_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `page_visit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问日志ID',
  `user_id` bigint DEFAULT NULL COMMENT '访问用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '访问用户名',
  `page_code` varchar(64) NOT NULL COMMENT '页面编码',
  `page_name` varchar(100) NOT NULL COMMENT '页面名称',
  `ip` varchar(64) DEFAULT NULL COMMENT '访问IP',
  `user_agent` varchar(512) DEFAULT NULL COMMENT '浏览器标识',
  `visit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_page_visit_code_time` (`page_code`,`visit_time`),
  KEY `idx_page_visit_user_time` (`user_id`,`visit_time`),
  KEY `idx_page_visit_time` (`visit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='页面访问日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父权限ID',
  `permission_name` varchar(80) NOT NULL COMMENT '权限名称',
  `permission_code` varchar(100) NOT NULL COMMENT '权限编码',
  `type` tinyint NOT NULL COMMENT '权限类型（1菜单 2按钮 3接口）',
  `path` varchar(255) DEFAULT NULL COMMENT '前端路由路径',
  `component` varchar(255) DEFAULT NULL COMMENT '前端组件路径',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1启用 0禁用）',
  `sort` int NOT NULL DEFAULT '100' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_permission_parent` (`parent_id`),
  KEY `idx_permission_type_status` (`type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `planting_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '种植批次ID',
  `plot_id` bigint NOT NULL COMMENT '地块ID',
  `crop_id` bigint NOT NULL COMMENT '作物ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `batch_no` varchar(50) DEFAULT NULL COMMENT '批次编号',
  `planting_area` decimal(10,2) DEFAULT NULL COMMENT '本批次种植面积',
  `area_unit` varchar(20) DEFAULT '亩' COMMENT '面积单位',
  `planted_at` date DEFAULT NULL COMMENT '种植日期',
  `expected_harvest_at` date DEFAULT NULL COMMENT '预计采收日期',
  `actual_harvest_at` date DEFAULT NULL COMMENT '实际采收日期',
  `growth_stage_id` bigint DEFAULT NULL COMMENT '生长阶段ID，关联growth_stage.id',
  `status` tinyint DEFAULT '1' COMMENT '状态（1种植中 2已采收 3已失败 4已取消）',
  `expected_yield_amount` decimal(10,2) DEFAULT NULL COMMENT '预计产量',
  `crop_image` varchar(255) DEFAULT NULL COMMENT '作物图片',
  `grown_days` int DEFAULT NULL COMMENT '已生长天数',
  `yield_amount` decimal(10,2) DEFAULT NULL COMMENT '实际产量',
  `yield_unit` varchar(20) DEFAULT 'kg' COMMENT '产量单位',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_id_user_plot` (`id`,`user_id`,`plot_id`),
  UNIQUE KEY `uk_batch_user_no` (`user_id`,`batch_no`),
  KEY `idx_planting_batch_plot_id` (`plot_id`),
  KEY `idx_planting_batch_crop_id` (`crop_id`),
  KEY `idx_planting_batch_user_id` (`user_id`),
  KEY `idx_planting_batch_status` (`status`),
  KEY `idx_planting_batch_planted_at` (`planted_at`),
  KEY `fk_batch_plot_user` (`plot_id`,`user_id`),
  KEY `idx_planting_batch_growth_stage_id` (`growth_stage_id`),
  CONSTRAINT `fk_batch_plot_user` FOREIGN KEY (`plot_id`, `user_id`) REFERENCES `plot` (`id`, `user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_planting_batch_crop` FOREIGN KEY (`crop_id`) REFERENCES `crop` (`id`),
  CONSTRAINT `fk_planting_batch_growth_stage` FOREIGN KEY (`growth_stage_id`) REFERENCES `growth_stage` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_planting_batch_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`),
  CONSTRAINT `fk_planting_batch_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_batch_area` CHECK (((`planting_area` is null) or (`planting_area` >= 0))),
  CONSTRAINT `chk_batch_date` CHECK (((`expected_harvest_at` is null) or (`planted_at` is null) or (`expected_harvest_at` >= `planted_at`))),
  CONSTRAINT `chk_batch_expected_yield` CHECK (((`expected_yield_amount` is null) or (`expected_yield_amount` >= 0))),
  CONSTRAINT `chk_batch_grown_days` CHECK (((`grown_days` is null) or (`grown_days` >= 0))),
  CONSTRAINT `chk_batch_status` CHECK ((`status` in (1,2,3,4))),
  CONSTRAINT `chk_batch_yield` CHECK (((`yield_amount` is null) or (`yield_amount` >= 0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='种植批次表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '地块ID',
  `farm_id` bigint DEFAULT NULL COMMENT '所属农场ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `plot_name` varchar(100) NOT NULL COMMENT '地块名称',
  `plot_code` varchar(50) DEFAULT NULL COMMENT '地块编号，如3号棚、A区01',
  `crop_image` varchar(500) DEFAULT NULL COMMENT '作物图片地址',
  `type` tinyint DEFAULT '2' COMMENT '地块类型（1水培种植 2大棚种植 3室外种植）',
  `area` decimal(10,2) DEFAULT NULL COMMENT '地块面积',
  `area_unit` varchar(20) DEFAULT '亩' COMMENT '面积单位',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `coordinate` varchar(64) DEFAULT NULL COMMENT '经纬度（经度,纬度）',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '纬度',
  `status` tinyint DEFAULT '1' COMMENT '状态（1正常 0停用）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plot_id_user` (`id`,`user_id`),
  UNIQUE KEY `uk_plot_user_code` (`user_id`,`plot_code`),
  KEY `idx_farm_plot_user_id` (`user_id`),
  KEY `idx_farm_plot_status` (`status`),
  KEY `idx_farm_plot_name` (`plot_name`),
  KEY `idx_plot_type` (`type`),
  KEY `idx_plot_farm_id` (`farm_id`) USING BTREE,
  KEY `idx_plot_farm_user` (`farm_id`,`user_id`) USING BTREE,
  CONSTRAINT `fk_farm_plot_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_plot_farm` FOREIGN KEY (`farm_id`) REFERENCES `farm` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_plot_farm_user` FOREIGN KEY (`farm_id`, `user_id`) REFERENCES `farm` (`id`, `user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_plot_area` CHECK (((`area` is null) or (`area` >= 0))),
  CONSTRAINT `chk_plot_latitude` CHECK (((`latitude` is null) or ((`latitude` >= -(90)) and (`latitude` <= 90)))),
  CONSTRAINT `chk_plot_longitude` CHECK (((`longitude` is null) or ((`longitude` >= -(180)) and (`longitude` <= 180)))),
  CONSTRAINT `chk_plot_status` CHECK ((`status` in (0,1))),
  CONSTRAINT `chk_plot_type` CHECK (((`type` is null) or (`type` in (1,2,3))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地块表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pump_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '水泵监测ID',
  `device_id` bigint NOT NULL COMMENT '水泵设备ID，关联 iot_device.id',
  `water_flow` decimal(8,2) DEFAULT NULL COMMENT '水流量（m³/h）',
  `water_pressure` decimal(6,2) DEFAULT NULL COMMENT '水压（MPa）',
  `data_status` tinyint DEFAULT '1' COMMENT '数据状态（1正常 2异常），根据水流量和水压正常运行范围判定',
  `abnormal_detail` varchar(500) DEFAULT NULL COMMENT '异常详情，记录哪些指标超出范围，如：水流量偏低、水压偏高',
  `cumulative_runtime` decimal(10,2) DEFAULT NULL COMMENT '累计运行时长（小时）',
  `collect_time` datetime NOT NULL COMMENT '数据采集时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_pump_data_device_id` (`device_id`) USING BTREE,
  KEY `idx_pump_data_data_status` (`data_status`) USING BTREE,
  KEY `idx_pump_data_collect_time` (`collect_time`) USING BTREE,
  KEY `idx_pump_data_device_time` (`device_id`,`collect_time`),
  CONSTRAINT `fk_pump_data_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='水泵监测数据表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_pd_check_device_type_bi` BEFORE INSERT ON `pump_data` FOR EACH ROW BEGIN DECLARE v_count INT DEFAULT 0; SELECT COUNT(1) INTO v_count FROM `iot_device` d JOIN `device_type` dt ON dt.id = d.type_id WHERE d.id = NEW.device_id AND dt.type_code = 'WATER_PUMP'; IF v_count = 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'pump_data device must be WATER_PUMP'; END IF; END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_pd_check_device_type_bu` BEFORE UPDATE ON `pump_data` FOR EACH ROW BEGIN DECLARE v_count INT DEFAULT 0; SELECT COUNT(1) INTO v_count FROM `iot_device` d JOIN `device_type` dt ON dt.id = d.type_id WHERE d.id = NEW.device_id AND dt.type_code = 'WATER_PUMP'; IF v_count = 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'pump_data device must be WATER_PUMP'; END IF; END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `role_name` varchar(50) NOT NULL COMMENT '????',
  `role_code` varchar(50) NOT NULL COMMENT '????',
  `description` varchar(255) DEFAULT NULL COMMENT '角色描述',
  `remark` varchar(255) DEFAULT NULL COMMENT '??',
  `status` tinyint DEFAULT '1' COMMENT '状态（1启用 0禁用）',
  `sort` int DEFAULT '100' COMMENT '排序',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '????',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除（0否 1是）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_name` (`role_name`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_data_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色数据权限ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `data_scope` varchar(30) NOT NULL DEFAULT 'self' COMMENT '数据范围',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_data_permission` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色数据权限表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色权限ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
  KEY `idx_role_permission_role` (`role_id`),
  KEY `idx_role_permission_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sms_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '短信验证码ID',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `code` varchar(10) NOT NULL COMMENT '验证码',
  `scene` tinyint NOT NULL COMMENT '使用场景（1登录 2注册 3找回密码 4绑定手机号 5修改手机号）',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `used` tinyint DEFAULT '0' COMMENT '是否已使用（0未使用 1已使用）',
  `used_time` datetime DEFAULT NULL COMMENT '使用时间',
  `send_ip` varchar(50) DEFAULT NULL COMMENT '发送请求IP',
  `user_agent` varchar(255) DEFAULT NULL COMMENT '请求客户端信息',
  `status` tinyint DEFAULT '1' COMMENT '状态（1正常 0失效）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sms_code_phone` (`phone`),
  KEY `idx_sms_code_scene` (`scene`),
  KEY `idx_sms_code_expire_time` (`expire_time`),
  KEY `idx_sms_code_used` (`used`),
  KEY `idx_sms_phone_scene_used_expire` (`phone`,`scene`,`used`,`expire_time`),
  CONSTRAINT `chk_sms_scene` CHECK ((`scene` in (1,2,3,4,5))),
  CONSTRAINT `chk_sms_status` CHECK ((`status` in (0,1))),
  CONSTRAINT `chk_sms_time` CHECK ((((`used` = 0) and (`used_time` is null)) or ((`used` = 1) and (`used_time` is not null) and (`used_time` <= `expire_time`)))),
  CONSTRAINT `chk_sms_used` CHECK ((`used` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短信验证码表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '加密密码',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `gender` tinyint DEFAULT '0' COMMENT '性别（0未知 1男 2女）',
  `role_id` bigint NOT NULL DEFAULT '5' COMMENT '??ID',
  `status` tinyint DEFAULT '1' COMMENT '状态（1正常 0禁用）',
  `region` varchar(100) DEFAULT NULL COMMENT '地区',
  `login_count` int DEFAULT '0' COMMENT '登录次数',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `phone` (`phone`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_user_role_id` (`role_id`),
  CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_user_gender` CHECK ((`gender` in (0,1,2))),
  CONSTRAINT `chk_user_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_oauth_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '第三方账号ID',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `provider` tinyint NOT NULL COMMENT '第三方平台（1微信 2QQ 3支付宝 4苹果）',
  `open_id` varchar(100) NOT NULL COMMENT '第三方平台OpenID',
  `union_id` varchar(100) DEFAULT NULL COMMENT '第三方平台UnionID',
  `nickname` varchar(100) DEFAULT NULL COMMENT '第三方昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '第三方头像',
  `access_token` varchar(500) DEFAULT NULL COMMENT '访问令牌',
  `refresh_token` varchar(500) DEFAULT NULL COMMENT '刷新令牌',
  `token_expire_time` datetime DEFAULT NULL COMMENT '令牌过期时间',
  `bind_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `status` tinyint DEFAULT '1' COMMENT '状态（1正常 0解绑）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_oauth_provider_openid` (`provider`,`open_id`),
  KEY `idx_oauth_user_id` (`user_id`),
  KEY `idx_oauth_union_id` (`union_id`),
  KEY `idx_oauth_status` (`status`),
  CONSTRAINT `fk_oauth_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='第三方登录账号表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户角色ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_user_role_user` (`user_id`),
  KEY `idx_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '仓库物资ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `inbound_operator_id` bigint NOT NULL COMMENT '初始入库人用户ID',
  `item_name` varchar(100) NOT NULL COMMENT '物资名称',
  `item_code` varchar(50) DEFAULT NULL COMMENT '物资编码',
  `image_url` varchar(500) DEFAULT NULL COMMENT '物资图片',
  `category` tinyint NOT NULL COMMENT '物资分类（1种子 2肥料 3农药 4工具 5设备耗材 6其他）',
  `specification` varchar(100) DEFAULT NULL COMMENT '规格型号，如500g/袋、20kg/桶',
  `unit` varchar(20) NOT NULL COMMENT '单位，如袋、瓶、kg、个',
  `stock_qty` decimal(10,2) DEFAULT '0.00' COMMENT '当前库存数量',
  `warning_qty` decimal(10,2) DEFAULT '0.00' COMMENT '库存预警数量',
  `manufacturer` varchar(100) DEFAULT NULL COMMENT '生产厂家',
  `status` tinyint DEFAULT '1' COMMENT '状态（1启用 0停用）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_warehouse_item_user_code` (`user_id`,`item_code`),
  KEY `idx_warehouse_item_user_id` (`user_id`),
  KEY `idx_warehouse_item_name` (`item_name`),
  KEY `idx_warehouse_item_category` (`category`),
  KEY `idx_warehouse_item_status` (`status`),
  KEY `idx_warehouse_item_inbound_operator_id` (`inbound_operator_id`),
  CONSTRAINT `fk_warehouse_item_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_warehouse_item_category` CHECK ((`category` in (1,2,3,4,5,6))),
  CONSTRAINT `chk_warehouse_item_status` CHECK ((`status` in (0,1))),
  CONSTRAINT `chk_warehouse_item_stock` CHECK ((`stock_qty` >= 0)),
  CONSTRAINT `chk_warehouse_item_warning` CHECK ((`warning_qty` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仓库物资表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '出入库记录ID',
  `item_id` bigint NOT NULL COMMENT '仓库物资ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `operator_id` bigint NOT NULL COMMENT '操作人用户ID',
  `recipient` varchar(100) DEFAULT NULL COMMENT '领用人或接收方',
  `record_type` tinyint NOT NULL COMMENT '记录类型（1入库 2出库 3库存调整）',
  `quantity` decimal(10,2) NOT NULL COMMENT '本次数量',
  `before_qty` decimal(10,2) DEFAULT NULL COMMENT '操作前库存',
  `after_qty` decimal(10,2) DEFAULT NULL COMMENT '操作后库存',
  `related_plot_id` bigint DEFAULT NULL COMMENT '关联地块ID，可为空',
  `related_task_id` bigint DEFAULT NULL COMMENT '关联农事任务ID，可为空',
  `source_type` tinyint DEFAULT NULL COMMENT '来源类型（1采购入库 2农事消耗 3盘点调整 4报损 5其他）',
  `supplier` varchar(100) DEFAULT NULL COMMENT '供应商',
  `price` decimal(10,2) DEFAULT NULL COMMENT '单价',
  `total_amount` decimal(10,2) DEFAULT NULL COMMENT '总金额',
  `record_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  `status` tinyint DEFAULT '1' COMMENT '状态（1正常 0删除）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_warehouse_record_item_id` (`item_id`),
  KEY `idx_warehouse_record_user_id` (`user_id`),
  KEY `idx_warehouse_record_operator_id` (`operator_id`),
  KEY `idx_warehouse_record_type` (`record_type`),
  KEY `idx_warehouse_record_time` (`record_time`),
  KEY `idx_warehouse_record_plot_id` (`related_plot_id`),
  KEY `idx_warehouse_record_task_id` (`related_task_id`),
  KEY `idx_warehouse_record_item_time` (`item_id`,`record_time`),
  CONSTRAINT `fk_warehouse_record_item` FOREIGN KEY (`item_id`) REFERENCES `warehouse_item` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_warehouse_record_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_warehouse_record_plot` FOREIGN KEY (`related_plot_id`) REFERENCES `plot` (`id`),
  CONSTRAINT `fk_warehouse_record_task` FOREIGN KEY (`related_task_id`) REFERENCES `farm_task` (`id`),
  CONSTRAINT `fk_warehouse_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_warehouse_record_amount` CHECK ((((`price` is null) or (`price` >= 0)) and ((`total_amount` is null) or (`total_amount` >= 0)))),
  CONSTRAINT `chk_warehouse_record_qty_snapshot` CHECK ((((`before_qty` is null) or (`before_qty` >= 0)) and ((`after_qty` is null) or (`after_qty` >= 0)))),
  CONSTRAINT `chk_warehouse_record_quantity` CHECK ((`quantity` > 0)),
  CONSTRAINT `chk_warehouse_record_status` CHECK ((`status` in (0,1))),
  CONSTRAINT `chk_warehouse_record_type` CHECK ((`record_type` in (1,2,3)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出入库记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `water_quality_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '水质监测ID',
  `device_id` bigint NOT NULL COMMENT '水质监测设备ID，关联 iot_device.id',
  `water_temperature` decimal(6,2) DEFAULT NULL COMMENT '水温（℃）',
  `ph` decimal(5,2) DEFAULT NULL COMMENT 'PH值',
  `ec_value` decimal(8,2) DEFAULT NULL COMMENT 'EC值（mS/cm）',
  `dissolved_oxygen` decimal(6,2) DEFAULT NULL COMMENT '溶解氧（mg/L）',
  `data_status` tinyint DEFAULT '1' COMMENT '数据状态（1正常 2异常），根据作物适宜范围自动判定',
  `abnormal_detail` varchar(500) DEFAULT NULL COMMENT '异常详情，记录哪些指标超出范围，如：水温偏高、EC值偏低',
  `cumulative_runtime` decimal(10,2) DEFAULT NULL COMMENT '累计运行时长（小时）',
  `collect_time` datetime NOT NULL COMMENT '数据采集时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_wq_data_device_id` (`device_id`) USING BTREE,
  KEY `idx_wq_data_data_status` (`data_status`) USING BTREE,
  KEY `idx_wq_data_collect_time` (`collect_time`) USING BTREE,
  KEY `idx_wq_data_device_time` (`device_id`,`collect_time`),
  CONSTRAINT `fk_wq_data_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='水质监测数据表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_wq_check_device_type_bi` BEFORE INSERT ON `water_quality_data` FOR EACH ROW BEGIN DECLARE v_count INT DEFAULT 0; SELECT COUNT(1) INTO v_count FROM `iot_device` d JOIN `device_type` dt ON dt.id = d.type_id WHERE d.id = NEW.device_id AND dt.type_code = 'WATER_QUALITY'; IF v_count = 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'water_quality_data device must be WATER_QUALITY'; END IF; END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `trg_wq_check_device_type_bu` BEFORE UPDATE ON `water_quality_data` FOR EACH ROW BEGIN DECLARE v_count INT DEFAULT 0; SELECT COUNT(1) INTO v_count FROM `iot_device` d JOIN `device_type` dt ON dt.id = d.type_id WHERE d.id = NEW.device_id AND dt.type_code = 'WATER_QUALITY'; IF v_count = 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'water_quality_data device must be WATER_QUALITY'; END IF; END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

