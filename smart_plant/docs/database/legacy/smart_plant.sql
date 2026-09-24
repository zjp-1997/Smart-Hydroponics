/*
 Navicat Premium Dump SQL

 Source Server         : admin
 Source Server Type    : MySQL
 Source Server Version : 80400 (8.4.0)
 Source Host           : localhost:3306
 Source Schema         : smart_plant

 Target Server Type    : MySQL
 Target Server Version : 80400 (8.4.0)
 File Encoding         : 65001

 Date: 11/06/2026 22:42:38
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI消息ID',
  `session_id` bigint NOT NULL COMMENT 'AI会话ID',
  `role` tinyint NOT NULL COMMENT '消息角色（1用户 2AI助手 3系统）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `message_type` tinyint NULL DEFAULT 1 COMMENT '消息类型（1文本 2图片 3语音 4文件）',
  `media_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片、语音或文件地址',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型名称',
  `model_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型版本',
  `prompt_tokens` int NULL DEFAULT 0 COMMENT '输入Token数量',
  `completion_tokens` int NULL DEFAULT 0 COMMENT '输出Token数量',
  `total_tokens` int NULL DEFAULT 0 COMMENT '总Token数量',
  `status` tinyint NULL DEFAULT 1 COMMENT '消息状态（1正常 2生成失败 0删除）',
  `fail_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成失败原因',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_chat_message_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_ai_chat_message_role`(`role` ASC) USING BTREE,
  INDEX `idx_ai_chat_message_create_time`(`create_time` ASC) USING BTREE,
  CONSTRAINT `fk_ai_chat_message_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_chat_message
-- ----------------------------

-- ----------------------------
-- Table structure for ai_chat_session
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE `ai_chat_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI会话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'AI会话编号',
  `session_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话标题',
  `scene` tinyint NULL DEFAULT 1 COMMENT '会话场景（1通用问答 2智能策略 3病虫害咨询 4识别结果追问 5农事建议）',
  `plot_id` bigint NULL DEFAULT NULL COMMENT '关联地块ID，可为空',
  `batch_id` bigint NULL DEFAULT NULL COMMENT '关联种植批次ID，可为空',
  `crop_id` bigint NULL DEFAULT NULL COMMENT '关联作物ID，可为空',
  `recognition_result_id` bigint NULL DEFAULT NULL COMMENT '关联智能识别结果ID，可为空',
  `last_message_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后一条消息内容',
  `last_message_time` datetime NULL DEFAULT NULL COMMENT '最后消息时间',
  `status` tinyint NULL DEFAULT 1 COMMENT '会话状态（1进行中 2已结束 0删除）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `session_no`(`session_no` ASC) USING BTREE,
  INDEX `idx_ai_chat_session_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_ai_chat_session_scene`(`scene` ASC) USING BTREE,
  INDEX `idx_ai_chat_session_plot_id`(`plot_id` ASC) USING BTREE,
  INDEX `idx_ai_chat_session_batch_id`(`batch_id` ASC) USING BTREE,
  INDEX `idx_ai_chat_session_crop_id`(`crop_id` ASC) USING BTREE,
  INDEX `idx_ai_chat_session_recognition_result_id`(`recognition_result_id` ASC) USING BTREE,
  INDEX `idx_ai_chat_session_last_message_time`(`last_message_time` ASC) USING BTREE,
  CONSTRAINT `fk_ai_chat_session_batch` FOREIGN KEY (`batch_id`) REFERENCES `planting_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_chat_session_crop` FOREIGN KEY (`crop_id`) REFERENCES `crop` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_chat_session_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_chat_session_recognition_result` FOREIGN KEY (`recognition_result_id`) REFERENCES `ai_recognition_result` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_chat_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_chat_session
-- ----------------------------

-- ----------------------------
-- Table structure for ai_recognition_type
-- ----------------------------
DROP TABLE IF EXISTS `ai_recognition_type`;
CREATE TABLE `ai_recognition_type`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '识别类型ID',
  `type_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型编码',
  `type_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1启用 0禁用）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ai_recognition_type_code`(`type_code` ASC) USING BTREE,
  INDEX `idx_ai_recognition_type_name`(`type_name` ASC) USING BTREE,
  INDEX `idx_ai_recognition_type_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI识别类型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_recognition_type
-- ----------------------------
INSERT INTO `ai_recognition_type` (`id`, `type_code`, `type_name`, `description`, `status`) VALUES
(1, 'CROP_RECOGNITION', '作物识别', '识别图片中的作物品种或作物类别', 1),
(2, 'DISEASE_RECOGNITION', '病害识别', '识别作物病害类型、症状和风险程度', 1),
(3, 'PEST_RECOGNITION', '虫害识别', '识别作物虫害类型、虫体或危害特征', 1),
(4, 'GROWTH_RECOGNITION', '生长状态识别', '识别作物长势、生育阶段和异常生长状态', 1),
(5, 'COMPREHENSIVE_RECOGNITION', '综合识别', '综合识别作物、病虫害、生长状态和处理建议', 1);

-- ----------------------------
-- Table structure for ai_recognition_record
-- ----------------------------
DROP TABLE IF EXISTS `ai_recognition_record`;
CREATE TABLE `ai_recognition_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI识别记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `source_type` tinyint NOT NULL DEFAULT 1 COMMENT '识别来源（1手动上传 2摄像头自动识别）',
  `crop_image_id` bigint NULL DEFAULT NULL COMMENT '手动上传图片ID，可为空',
  `camera_id` bigint NULL DEFAULT NULL COMMENT '摄像头ID，自动识别时必填',
  `plot_id` bigint NULL DEFAULT NULL COMMENT '地块ID，自动识别时必填',
  `capture_id` bigint NULL DEFAULT NULL COMMENT '摄像头采集记录ID，自动识别时可关联',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '识别图片地址',
  `thumbnail_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '缩略图地址',
  `image_size` bigint NULL DEFAULT NULL COMMENT '图片大小(字节)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '记录状态（1待识别 2识别中 3已完成 4识别失败）',
  `recognition_start_time` datetime NULL DEFAULT NULL COMMENT '识别开始时间',
  `recognition_end_time` datetime NULL DEFAULT NULL COMMENT '识别结束时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_record_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_ai_record_source_type`(`source_type` ASC) USING BTREE,
  INDEX `idx_ai_record_crop_image_id`(`crop_image_id` ASC) USING BTREE,
  INDEX `idx_ai_record_camera_id`(`camera_id` ASC) USING BTREE,
  INDEX `idx_ai_record_plot_id`(`plot_id` ASC) USING BTREE,
  INDEX `idx_ai_record_capture_id`(`capture_id` ASC) USING BTREE,
  INDEX `idx_ai_record_status_time`(`status` ASC, `create_time` ASC) USING BTREE,
  CONSTRAINT `fk_ai_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_record_crop_image` FOREIGN KEY (`crop_image_id`) REFERENCES `crop_image` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_record_camera` FOREIGN KEY (`camera_id`) REFERENCES `camera_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_record_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_record_capture` FOREIGN KEY (`capture_id`) REFERENCES `camera_capture_record` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI识别记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_recognition_record
-- ----------------------------

-- ----------------------------
-- Table structure for ai_recognition_result
-- ----------------------------
DROP TABLE IF EXISTS `ai_recognition_result`;
CREATE TABLE `ai_recognition_result`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '智能识别结果ID',
  `record_id` bigint NOT NULL COMMENT 'AI识别记录ID',
  `crop_id` bigint NULL DEFAULT NULL COMMENT '作物ID，可为空，模型识别后可回填',
  `recognition_type` bigint NOT NULL DEFAULT 5 COMMENT '识别类型ID，关联ai_recognition_type.id',
  `result_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '识别结果名称，如生菜、霜霉病',
  `result_summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '识别结果摘要',
  `result_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '识别结果详情',
  `disease_pest_id` bigint NULL DEFAULT NULL COMMENT '关联病虫害ID，识别出病虫害时使用',
  `confidence` decimal(5, 2) NULL DEFAULT NULL COMMENT '识别置信度，百分比',
  `severity_level` tinyint NULL DEFAULT NULL COMMENT '严重程度（1轻微 2中等 3严重）',
  `suggestion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '处理建议',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型名称',
  `model_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型版本',
  `status` tinyint NULL DEFAULT 3 COMMENT '识别状态（1成功 2失败 3处理中）',
  `fail_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '识别失败原因',
  `recognize_time` datetime NULL DEFAULT NULL COMMENT '识别完成时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_result_record_id`(`record_id` ASC) USING BTREE,
  INDEX `idx_ai_result_crop_id`(`crop_id` ASC) USING BTREE,
  INDEX `idx_ai_result_type`(`recognition_type` ASC) USING BTREE,
  INDEX `idx_ai_result_status`(`status` ASC) USING BTREE,
  INDEX `idx_ai_result_time`(`recognize_time` ASC) USING BTREE,
  INDEX `idx_ai_result_disease_pest_id`(`disease_pest_id` ASC) USING BTREE,
  CONSTRAINT `fk_ai_result_record` FOREIGN KEY (`record_id`) REFERENCES `ai_recognition_record` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_result_type` FOREIGN KEY (`recognition_type`) REFERENCES `ai_recognition_type` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_result_crop` FOREIGN KEY (`crop_id`) REFERENCES `crop` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_ai_result_disease_pest` FOREIGN KEY (`disease_pest_id`) REFERENCES `disease_pest` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能识别结果表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_recognition_result
-- ----------------------------

-- ----------------------------
-- Table structure for alert_event
-- ----------------------------
DROP TABLE IF EXISTS `alert_event`;
CREATE TABLE `alert_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '预警事件ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `plot_id` bigint NULL DEFAULT NULL COMMENT '所属地块ID',
  `device_id` bigint NULL DEFAULT NULL COMMENT '关联设备ID',
  `batch_id` bigint NULL DEFAULT NULL COMMENT '关联种植批次ID',
  `alert_type` tinyint NOT NULL COMMENT '预警类型（1环境异常 2设备异常 3农事任务逾期 4摄像头异常 5AI识别异常 6库存预警）',
  `alert_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '预警标题',
  `alert_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '预警内容',
  `metric_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '异常指标编码，如temperature/humidity/ph/ec/do',
  `metric_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '异常指标名称，如温度、湿度、PH',
  `metric_value` decimal(10, 3) NULL DEFAULT NULL COMMENT '异常指标值',
  `metric_unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '指标单位',
  `threshold_min` decimal(10, 3) NULL DEFAULT NULL COMMENT '阈值下限',
  `threshold_max` decimal(10, 3) NULL DEFAULT NULL COMMENT '阈值上限',
  `alert_level` tinyint NULL DEFAULT 1 COMMENT '预警等级（1普通 2重要 3紧急）',
  `source_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '来源类型，如iot_device/farm_task/camera_capture_record/ai_recognition_record',
  `source_id` bigint NULL DEFAULT NULL COMMENT '来源数据ID',
  `process_status` tinyint NULL DEFAULT 1 COMMENT '处理状态（1未处理 2处理中 3已处理 4已忽略）',
  `handler_id` bigint NULL DEFAULT NULL COMMENT '处理人用户ID',
  `handle_time` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `handle_result` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理结果',
  `trigger_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1正常 0删除）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_alert_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_alert_plot_id`(`plot_id` ASC) USING BTREE,
  INDEX `idx_alert_device_id`(`device_id` ASC) USING BTREE,
  INDEX `idx_alert_batch_id`(`batch_id` ASC) USING BTREE,
  INDEX `idx_alert_type`(`alert_type` ASC) USING BTREE,
  INDEX `idx_alert_level`(`alert_level` ASC) USING BTREE,
  INDEX `idx_alert_process_status`(`process_status` ASC) USING BTREE,
  INDEX `idx_alert_trigger_time`(`trigger_time` ASC) USING BTREE,
  INDEX `idx_alert_source`(`source_type` ASC, `source_id` ASC) USING BTREE,
  INDEX `fk_alert_handler`(`handler_id` ASC) USING BTREE,
  CONSTRAINT `fk_alert_batch` FOREIGN KEY (`batch_id`) REFERENCES `planting_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_alert_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_alert_handler` FOREIGN KEY (`handler_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_alert_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_alert_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '预警事件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of alert_event
-- ----------------------------

-- ----------------------------
-- Table structure for camera_device
-- ----------------------------
DROP TABLE IF EXISTS `camera_device`;
CREATE TABLE `camera_device`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '监控设备ID',
  `device_id` bigint NOT NULL COMMENT '原 iot_device.id，迁移后保留来源设备编号',
  `plot_id` bigint NOT NULL COMMENT '所属地块',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '监控设备名称',
  `stream_protocol` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'RTSP/GB28181/HTTP',
  `stream_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '视频流地址',
  `snapshot_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '截图地址',
  `resolution` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '1920x1080',
  `online_status` tinyint NULL DEFAULT 0 COMMENT '在线状态 0离线 1在线',
  `direction` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '监控方向',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_camera_device_device_id`(`device_id` ASC) USING BTREE,
  INDEX `idx_camera_device_plot_id`(`plot_id` ASC) USING BTREE,
  INDEX `idx_camera_device_online_status`(`online_status` ASC) USING BTREE,
  CONSTRAINT `fk_camera_device_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '监控设备表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of camera_device
-- ----------------------------

-- ----------------------------
-- Table structure for camera_capture_plan
-- ----------------------------
DROP TABLE IF EXISTS `camera_capture_plan`;
CREATE TABLE `camera_capture_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '摄像头拍照计划ID',
  `device_id` bigint NOT NULL COMMENT '摄像头设备ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `plot_id` bigint NOT NULL COMMENT '所属地块ID',
  `interval_minutes` int NULL DEFAULT 60 COMMENT '拍照间隔，单位分钟',
  `start_time` time NULL DEFAULT NULL COMMENT '每日开始时间',
  `end_time` time NULL DEFAULT NULL COMMENT '每日结束时间',
  `enabled` tinyint NULL DEFAULT 1 COMMENT '是否启用（1启用 0停用）',
  `last_capture_time` datetime NULL DEFAULT NULL COMMENT '上次拍照时间',
  `next_capture_time` datetime NULL DEFAULT NULL COMMENT '下次拍照时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_capture_plan_device_id`(`device_id` ASC) USING BTREE,
  INDEX `idx_capture_plan_enabled`(`enabled` ASC) USING BTREE,
  INDEX `idx_capture_plan_next_time`(`next_capture_time` ASC) USING BTREE,
  INDEX `fk_capture_plan_user`(`user_id` ASC) USING BTREE,
  INDEX `fk_capture_plan_plot`(`plot_id` ASC) USING BTREE,
  CONSTRAINT `fk_capture_plan_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_capture_plan_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_capture_plan_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '摄像头拍照计划表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of camera_capture_plan
-- ----------------------------

-- ----------------------------
-- Table structure for camera_capture_record
-- ----------------------------
DROP TABLE IF EXISTS `camera_capture_record`;
CREATE TABLE `camera_capture_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '摄像头拍照记录ID',
  `device_id` bigint NOT NULL COMMENT '摄像头设备ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `plot_id` bigint NOT NULL COMMENT '所属地块ID',
  `batch_id` bigint NULL DEFAULT NULL COMMENT '种植批次ID',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片地址',
  `thumbnail_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '缩略图地址',
  `image_size` bigint NULL DEFAULT NULL COMMENT '图片大小(字节)',
  `capture_type` tinyint NULL DEFAULT 1 COMMENT '拍照类型（1定时拍照 2手动拍照 3告警抓拍）',
  `capture_time` datetime NOT NULL COMMENT '拍照时间',
  `ai_checked` tinyint NULL DEFAULT 0 COMMENT '是否已进行AI识别（1是 0否）',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1正常 0删除）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_camera_record_device_id`(`device_id` ASC) USING BTREE,
  INDEX `idx_camera_record_plot_id`(`plot_id` ASC) USING BTREE,
  INDEX `idx_camera_record_batch_id`(`batch_id` ASC) USING BTREE,
  INDEX `idx_camera_record_capture_time`(`capture_time` ASC) USING BTREE,
  INDEX `fk_camera_record_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_camera_record_batch` FOREIGN KEY (`batch_id`) REFERENCES `planting_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_camera_record_camera_device` FOREIGN KEY (`device_id`) REFERENCES `camera_device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_camera_record_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_camera_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '摄像头拍照记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of camera_capture_record
-- ----------------------------

-- ----------------------------
-- Table structure for consult_message
-- ----------------------------
DROP TABLE IF EXISTS `consult_message`;
CREATE TABLE `consult_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '咨询消息ID',
  `session_id` bigint NOT NULL COMMENT '咨询会话ID',
  `sender_id` bigint NOT NULL COMMENT '发送人用户ID',
  `receiver_id` bigint NOT NULL COMMENT '接收人用户ID',
  `message_type` tinyint NULL DEFAULT 1 COMMENT '消息类型（1文本 2图片 3语音 4文件）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '消息内容',
  `media_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片、语音或文件地址',
  `is_read` tinyint NULL DEFAULT 0 COMMENT '是否已读（0未读 1已读）',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1正常 0删除）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_consult_message_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_consult_message_sender_id`(`sender_id` ASC) USING BTREE,
  INDEX `idx_consult_message_receiver_id`(`receiver_id` ASC) USING BTREE,
  INDEX `idx_consult_message_is_read`(`is_read` ASC) USING BTREE,
  INDEX `idx_consult_message_create_time`(`create_time` ASC) USING BTREE,
  CONSTRAINT `fk_consult_message_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_consult_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_consult_message_session` FOREIGN KEY (`session_id`) REFERENCES `consult_session` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '咨询消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of consult_message
-- ----------------------------

-- ----------------------------
-- Table structure for consult_session
-- ----------------------------
DROP TABLE IF EXISTS `consult_session`;
CREATE TABLE `consult_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '咨询会话ID',
  `user_id` bigint NOT NULL COMMENT '咨询用户ID',
  `expert_id` bigint NOT NULL COMMENT '专家信息ID',
  `plot_id` bigint NULL DEFAULT NULL COMMENT '关联地块ID，可为空',
  `batch_id` bigint NULL DEFAULT NULL COMMENT '关联种植批次ID，可为空',
  `crop_id` bigint NULL DEFAULT NULL COMMENT '关联作物ID，可为空',
  `session_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话编号',
  `session_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '咨询标题',
  `question_summary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '问题摘要',
  `consult_type` tinyint NULL DEFAULT 1 COMMENT '咨询类型（1图文咨询 2病虫害咨询 3种植技术咨询 4设备环境咨询 5其他）',
  `status` tinyint NULL DEFAULT 1 COMMENT '会话状态（1进行中 2已结束 3已取消）',
  `last_message_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后一条消息内容',
  `last_message_time` datetime NULL DEFAULT NULL COMMENT '最后消息时间',
  `user_unread_count` int NULL DEFAULT 0 COMMENT '用户未读数量',
  `expert_unread_count` int NULL DEFAULT 0 COMMENT '专家未读数量',
  `start_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `rating` decimal(3, 2) NULL DEFAULT NULL COMMENT '用户评分',
  `evaluation` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户评价',
  `status_flag` tinyint NULL DEFAULT 1 COMMENT '数据状态（1正常 0删除）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `session_no`(`session_no` ASC) USING BTREE,
  INDEX `idx_consult_session_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_consult_session_expert_id`(`expert_id` ASC) USING BTREE,
  INDEX `idx_consult_session_plot_id`(`plot_id` ASC) USING BTREE,
  INDEX `idx_consult_session_batch_id`(`batch_id` ASC) USING BTREE,
  INDEX `idx_consult_session_crop_id`(`crop_id` ASC) USING BTREE,
  INDEX `idx_consult_session_status`(`status` ASC) USING BTREE,
  INDEX `idx_consult_session_last_message_time`(`last_message_time` ASC) USING BTREE,
  CONSTRAINT `fk_consult_session_batch` FOREIGN KEY (`batch_id`) REFERENCES `planting_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_consult_session_crop` FOREIGN KEY (`crop_id`) REFERENCES `crop` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_consult_session_expert` FOREIGN KEY (`expert_id`) REFERENCES `expert_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_consult_session_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_consult_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '咨询会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of consult_session
-- ----------------------------

-- ----------------------------
-- Table structure for crop_type
-- ----------------------------
DROP TABLE IF EXISTS `crop_type`;
CREATE TABLE `crop_type`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '类型ID',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父类型ID',
  `type_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1启用 0停用）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_crop_type_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_crop_type_name`(`type_name` ASC) USING BTREE,
  INDEX `idx_crop_type_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_crop_type_parent` FOREIGN KEY (`parent_id`) REFERENCES `crop_type` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作物类型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of crop_type
-- ----------------------------

-- ----------------------------
-- Table structure for crop
-- ----------------------------
DROP TABLE IF EXISTS `crop`;
CREATE TABLE `crop`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '作物ID',
  `type_id` bigint NULL DEFAULT NULL COMMENT '作物类型ID',
  `crop_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '作物名称，如生菜、上海青',
  `crop_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '作物编码',
  `variety` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '品种名称',
  `growth_days` int NULL DEFAULT NULL COMMENT '推荐生长周期，单位天',
  `suitable_temperature` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '适宜温度范围，如15-25℃',
  `suitable_humidity` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '适宜湿度范围，如60%-80%',
  `suitable_ph` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '适宜PH范围，如6.0-7.5',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '作物图片',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '作物说明',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1启用 0停用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_crop_type_id`(`type_id` ASC) USING BTREE,
  INDEX `idx_crop_name`(`crop_name` ASC) USING BTREE,
  INDEX `idx_crop_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_crop_type` FOREIGN KEY (`type_id`) REFERENCES `crop_type` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作物表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of crop
-- ----------------------------

-- ----------------------------
-- Table structure for growth_stage
-- ----------------------------
DROP TABLE IF EXISTS `growth_stage`;
CREATE TABLE `growth_stage`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '阶段ID',
  `crop_id` bigint NOT NULL COMMENT '作物ID',
  `stage_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '阶段名称（发芽期/幼苗期等）',
  `stage_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '阶段编码',
  `stage_order` int NOT NULL COMMENT '阶段顺序（1,2,3...）',
  `start_day` int NULL DEFAULT NULL COMMENT '开始天数，从播种或定植第几天开始',
  `end_day` int NULL DEFAULT NULL COMMENT '结束天数，从播种或定植第几天结束',
  `duration` int NULL DEFAULT NULL COMMENT '持续天数',
  `light_hours` decimal(4, 1) NULL DEFAULT NULL COMMENT '建议光照时长（小时/天）',
  `temp_min` decimal(4, 1) NULL DEFAULT NULL COMMENT '最低适宜温度',
  `temp_max` decimal(4, 1) NULL DEFAULT NULL COMMENT '最高适宜温度',
  `humidity_min` decimal(5, 2) NULL DEFAULT NULL COMMENT '最低适宜湿度（%）',
  `humidity_max` decimal(5, 2) NULL DEFAULT NULL COMMENT '最高适宜湿度（%）',
  `ph_min` decimal(3, 1) NULL DEFAULT NULL COMMENT '最低适宜PH',
  `ph_max` decimal(3, 1) NULL DEFAULT NULL COMMENT '最高适宜PH',
  `ec_min` decimal(4, 2) NULL DEFAULT NULL COMMENT '最低适宜EC值',
  `ec_max` decimal(4, 2) NULL DEFAULT NULL COMMENT '最高适宜EC值',
  `water_interval_days` int NULL DEFAULT NULL COMMENT '建议浇水间隔天数',
  `fertilizer_interval_days` int NULL DEFAULT NULL COMMENT '建议施肥间隔天数',
  `management_advice` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '管理建议',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1启用 0停用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_growth_stage_crop_order`(`crop_id` ASC, `stage_order` ASC) USING BTREE,
  INDEX `idx_growth_stage_crop_id`(`crop_id` ASC) USING BTREE,
  INDEX `idx_growth_stage_name`(`stage_name` ASC) USING BTREE,
  INDEX `idx_growth_stage_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_growth_stage_crop` FOREIGN KEY (`crop_id`) REFERENCES `crop` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作物生长期表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of growth_stage
-- ----------------------------

-- ----------------------------
-- Table structure for disease_pest
-- ----------------------------
DROP TABLE IF EXISTS `disease_control`;
DROP TABLE IF EXISTS `disease_pest`;
CREATE TABLE `disease_pest`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '病虫害ID',
  `crop_type_id` bigint NULL DEFAULT NULL COMMENT '关联作物类型ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '病虫害名称',
  `type` tinyint NOT NULL COMMENT '类型（1病害 2虫害 3生理性病害）',
  `symptom` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '症状描述',
  `cause` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '发生原因',
  `suitable_stage` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '易发生生长阶段',
  `severity_level` tinyint NULL DEFAULT 1 COMMENT '危害等级（1轻微 2中等 3严重）',
  `cover_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面图片',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1启用 0停用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_disease_pest_crop_type_id`(`crop_type_id` ASC) USING BTREE,
  INDEX `idx_disease_pest_type`(`type` ASC) USING BTREE,
  INDEX `idx_disease_pest_name`(`name` ASC) USING BTREE,
  INDEX `idx_disease_pest_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_disease_pest_crop_type` FOREIGN KEY (`crop_type_id`) REFERENCES `crop_type` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '病虫害知识库表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of disease_pest
-- ----------------------------

-- ----------------------------
-- Table structure for disease_control
-- ----------------------------
CREATE TABLE `disease_control`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '防治措施ID',
  `disease_id` bigint NOT NULL COMMENT '病虫害ID',
  `control_type` tinyint NOT NULL COMMENT '防治类型（1预防 2治疗）',
  `method` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '措施内容',
  `drug_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '药剂名称',
  `usage_method` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '使用方法',
  `suitable_stage` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '适用阶段',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1启用 0停用）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_crop_code`(`crop_code` ASC) USING BTREE,
  INDEX `idx_disease_control_disease_id`(`disease_id` ASC) USING BTREE,
  INDEX `idx_disease_control_type`(`control_type` ASC) USING BTREE,
  INDEX `idx_disease_control_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_disease_control_disease` FOREIGN KEY (`disease_id`) REFERENCES `disease_pest` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '病虫害防治措施表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of disease_control
-- ----------------------------

-- ----------------------------
-- Table structure for institution
-- ----------------------------
DROP TABLE IF EXISTS `institution`;
CREATE TABLE `institution`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '机构ID',
  `institution_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '学校或机构名称',
  `institution_type` tinyint NULL DEFAULT 1 COMMENT '机构类型（1学校 2科研机构 3企业 4其他）',
  `contact_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '机构地址',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1正常 0禁用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_institution_name`(`institution_name` ASC) USING BTREE,
  INDEX `idx_institution_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学校或机构表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of institution
-- ----------------------------

-- ----------------------------
-- Table structure for expert_profile
-- ----------------------------
DROP TABLE IF EXISTS `expert_profile`;
CREATE TABLE `expert_profile`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '专家信息ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '关联用户ID（可选）',
  `institution_id` bigint NULL DEFAULT NULL COMMENT '所属学校或机构ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家登录用户名',
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家登录密码密文',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专家真实姓名',
  `job_title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '职称，如高级农艺师、教授',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家邮箱',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家头像，可默认取用户头像',
  `rating` decimal(3, 2) NULL DEFAULT 5.00 COMMENT '评分',
  `review_count` int NULL DEFAULT 0 COMMENT '评价数量',
  `consultation_count` int NULL DEFAULT 0 COMMENT '咨询次数',
  `audit_status` tinyint NULL DEFAULT 1 COMMENT '审核状态（1待审核 2审核通过 3审核拒绝）',
  `audit_pass_time` datetime NULL DEFAULT NULL COMMENT '审核通过时间',
  `service_status` tinyint NULL DEFAULT 1 COMMENT '服务状态（1可咨询 0暂停咨询）',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1正常 0禁用）',
  `consultation_status` tinyint NULL DEFAULT 1 COMMENT '咨询状态（1可咨询 0暂停咨询）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_expert_profile_user_id`(`user_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_expert_profile_username`(`username` ASC) USING BTREE,
  INDEX `idx_expert_profile_institution_id`(`institution_id` ASC) USING BTREE,
  INDEX `idx_expert_profile_real_name`(`real_name` ASC) USING BTREE,
  INDEX `idx_expert_profile_audit_status`(`audit_status` ASC) USING BTREE,
  INDEX `idx_expert_profile_audit_pass_time`(`audit_pass_time` ASC) USING BTREE,
  INDEX `idx_expert_profile_service_status`(`service_status` ASC) USING BTREE,
  INDEX `idx_expert_profile_status`(`status` ASC) USING BTREE,
  INDEX `idx_expert_profile_consultation_status`(`consultation_status` ASC) USING BTREE,
  CONSTRAINT `fk_expert_profile_institution` FOREIGN KEY (`institution_id`) REFERENCES `institution` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_expert_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '专家基础信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of expert_profile
-- ----------------------------

-- ----------------------------
-- Table structure for expert_detail
-- ----------------------------
DROP TABLE IF EXISTS `expert_detail`;
CREATE TABLE `expert_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '专家详情ID',
  `expert_id` bigint NOT NULL COMMENT '专家信息ID',
  `specialty` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '擅长方向，如病虫害防治、水培种植、蔬菜栽培',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '专家简介',
  `research_direction` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '研究方向',
  `work_years` int NULL DEFAULT NULL COMMENT '从业年限',
  `service_scope` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '服务范围',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_expert_detail_expert_id`(`expert_id` ASC) USING BTREE,
  CONSTRAINT `fk_expert_detail_profile` FOREIGN KEY (`expert_id`) REFERENCES `expert_profile` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '专家扩展详情表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of expert_detail
-- ----------------------------

-- ----------------------------
-- Table structure for expert_certificate
-- ----------------------------
DROP TABLE IF EXISTS `expert_certificate`;
CREATE TABLE `expert_certificate`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '专家证书ID',
  `expert_id` bigint NOT NULL COMMENT '专家信息ID',
  `certificate_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '资质证书图片地址',
  `is_expired` tinyint NULL DEFAULT 0 COMMENT '是否过期（1已过期 0未过期）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_expert_certificate_expert_id`(`expert_id` ASC) USING BTREE,
  CONSTRAINT `fk_expert_certificate_profile` FOREIGN KEY (`expert_id`) REFERENCES `expert_profile` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '专家证书表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of expert_certificate
-- ----------------------------

-- ----------------------------
-- Table structure for expert_review
-- ----------------------------
DROP TABLE IF EXISTS `expert_review`;
CREATE TABLE `expert_review`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '专家评价ID',
  `expert_id` bigint NOT NULL COMMENT '专家信息ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '评价用户ID',
  `rating` decimal(2, 1) NOT NULL COMMENT '本次评价评分',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '评价内容',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_expert_review_expert_id`(`expert_id` ASC) USING BTREE,
  INDEX `idx_expert_review_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_expert_review_profile` FOREIGN KEY (`expert_id`) REFERENCES `expert_profile` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_expert_review_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '专家评价表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of expert_review
-- ----------------------------

-- ----------------------------
-- Table structure for expert_audit_record
-- ----------------------------
DROP TABLE IF EXISTS `expert_audit_record`;
CREATE TABLE `expert_audit_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '审核记录ID',
  `expert_id` bigint NOT NULL COMMENT '专家信息ID',
  `audit_status` tinyint NOT NULL COMMENT '审核结果（1待审核 2审核通过 3审核拒绝）',
  `audit_opinion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核意见',
  `auditor_id` bigint NULL DEFAULT NULL COMMENT '审核人用户ID',
  `audit_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_expert_audit_expert_id`(`expert_id` ASC) USING BTREE,
  INDEX `idx_expert_audit_status`(`audit_status` ASC) USING BTREE,
  CONSTRAINT `fk_expert_audit_profile` FOREIGN KEY (`expert_id`) REFERENCES `expert_profile` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_expert_audit_auditor` FOREIGN KEY (`auditor_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '专家入驻审核记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of expert_audit_record
-- ----------------------------

-- ----------------------------
-- Table structure for farm_task
-- ----------------------------
DROP TABLE IF EXISTS `farm_task`;
CREATE TABLE `farm_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '农事任务ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `plot_id` bigint NOT NULL COMMENT '所属地块ID',
  `batch_id` bigint NULL DEFAULT NULL COMMENT '种植批次ID',
  `task_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务标题',
  `task_type` tinyint NOT NULL COMMENT '任务类型（1浇水 2施肥 3打药 4采收 5巡检 6除草 7补光 8其他）',
  `task_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '任务内容',
  `priority` tinyint NULL DEFAULT 2 COMMENT '优先级（1低 2普通 3高 4紧急）',
  `planned_start_time` datetime NULL DEFAULT NULL COMMENT '计划开始时间',
  `planned_end_time` datetime NULL DEFAULT NULL COMMENT '计划结束时间',
  `actual_start_time` datetime NULL DEFAULT NULL COMMENT '实际开始时间',
  `actual_end_time` datetime NULL DEFAULT NULL COMMENT '实际完成时间',
  `status` tinyint NULL DEFAULT 1 COMMENT '任务状态（1未开始 2进行中 3已完成 4已逾期 5已取消）',
  `executor_id` bigint NULL DEFAULT NULL COMMENT '执行人用户ID',
  `complete_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '完成备注',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_farm_task_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_farm_task_plot_id`(`plot_id` ASC) USING BTREE,
  INDEX `idx_farm_task_batch_id`(`batch_id` ASC) USING BTREE,
  INDEX `idx_farm_task_type`(`task_type` ASC) USING BTREE,
  INDEX `idx_farm_task_status`(`status` ASC) USING BTREE,
  INDEX `idx_farm_task_plan_time`(`planned_start_time` ASC, `planned_end_time` ASC) USING BTREE,
  INDEX `idx_farm_task_executor_id`(`executor_id` ASC) USING BTREE,
  CONSTRAINT `fk_farm_task_batch` FOREIGN KEY (`batch_id`) REFERENCES `planting_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_farm_task_executor` FOREIGN KEY (`executor_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_farm_task_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_farm_task_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '农事任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of farm_task
-- ----------------------------

-- ----------------------------
-- Table structure for farm_task_log
-- ----------------------------
DROP TABLE IF EXISTS `farm_task_log`;
CREATE TABLE `farm_task_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `task_id` bigint NOT NULL COMMENT '农事任务ID',
  `operator_id` bigint NOT NULL COMMENT '操作人用户ID',
  `action_type` tinyint NOT NULL COMMENT '操作类型（1创建 2开始 3完成 4取消 5修改 6逾期）',
  `action_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作内容',
  `before_status` tinyint NULL DEFAULT NULL COMMENT '操作前状态',
  `after_status` tinyint NULL DEFAULT NULL COMMENT '操作后状态',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_log_task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_task_log_operator_id`(`operator_id` ASC) USING BTREE,
  INDEX `idx_task_log_action_type`(`action_type` ASC) USING BTREE,
  CONSTRAINT `fk_task_log_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_task_log_task` FOREIGN KEY (`task_id`) REFERENCES `farm_task` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '农事任务日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of farm_task_log
-- ----------------------------

-- ----------------------------
-- Table structure for iot_device
-- ----------------------------
DROP TABLE IF EXISTS `iot_device`;
CREATE TABLE `iot_device`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  `plot_id` bigint NOT NULL COMMENT '所属地块ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `device_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备唯一编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备名称',
  `type_id` bigint NOT NULL COMMENT '设备类型ID',
  `control_status` tinyint NOT NULL DEFAULT 0 COMMENT '控制状态（0关闭 1开启）',
  `online_status` tinyint NOT NULL DEFAULT 0 COMMENT '在线状态（0离线 1在线）',
  `health_status` tinyint NOT NULL DEFAULT 0 COMMENT '健康状态（0正常 1故障 2维护）',
  `install_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '设备安装时间',
  `offline_time` datetime NULL DEFAULT NULL COMMENT '最近一次离线时间',
  `fault_time` datetime NULL DEFAULT NULL COMMENT '最近一次故障或维护时间',
  `last_heartbeat_time` datetime NULL DEFAULT NULL COMMENT '最后心跳时间',
  `last_online_time` datetime NULL DEFAULT NULL COMMENT '最近一次在线会话开始时间',
  `online_duration` bigint NOT NULL DEFAULT 0 COMMENT '累计在线时长，单位秒',
  `last_data_time` datetime NULL DEFAULT NULL COMMENT '最后数据上报时间',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备安装位置或所属地块位置说明',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_iot_device_code`(`device_code` ASC) USING BTREE,
  INDEX `idx_iot_device_plot_id`(`plot_id` ASC) USING BTREE,
  INDEX `idx_iot_device_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_iot_device_type_id`(`type_id` ASC) USING BTREE,
  INDEX `idx_iot_device_control_status`(`control_status` ASC) USING BTREE,
  INDEX `idx_iot_device_online_status`(`online_status` ASC) USING BTREE,
  INDEX `idx_iot_device_health_status`(`health_status` ASC) USING BTREE,
  INDEX `idx_iot_device_install_time`(`install_time` ASC) USING BTREE,
  INDEX `idx_iot_device_offline_time`(`offline_time` ASC) USING BTREE,
  INDEX `idx_iot_device_fault_time`(`fault_time` ASC) USING BTREE,
  INDEX `idx_iot_device_heartbeat`(`last_heartbeat_time` ASC) USING BTREE,
  CONSTRAINT `fk_iot_device_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_iot_device_type` FOREIGN KEY (`type_id`) REFERENCES `device_type` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_iot_device_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_device
-- ----------------------------

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息通知ID',
  `user_id` bigint NOT NULL COMMENT '接收用户ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '消息内容',
  `notice_type` tinyint NOT NULL COMMENT '消息类型（1系统消息 2农事任务 3设备消息 4环境预警 5专家咨询 6AI识别 7库存预警）',
  `ref_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联业务类型，如alert_event/farm_task/iot_device/consult_session',
  `ref_id` bigint NULL DEFAULT NULL COMMENT '关联业务ID',
  `alert_id` bigint NULL DEFAULT NULL COMMENT '关联预警事件ID',
  `level` tinyint NULL DEFAULT 1 COMMENT '消息级别（1普通 2重要 3紧急）',
  `is_read` tinyint NULL DEFAULT 0 COMMENT '是否已读（0未读 1已读）',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  `send_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `publisher_id` bigint NULL DEFAULT NULL COMMENT '发布人用户ID',
  `publisher_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发布人名称',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1正常 0删除）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notification_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_notification_notice_type`(`notice_type` ASC) USING BTREE,
  INDEX `idx_notification_is_read`(`is_read` ASC) USING BTREE,
  INDEX `idx_notification_send_time`(`send_time` ASC) USING BTREE,
  INDEX `idx_notification_publisher_id`(`publisher_id` ASC) USING BTREE,
  INDEX `idx_notification_ref`(`ref_type` ASC, `ref_id` ASC) USING BTREE,
  INDEX `idx_notification_alert_id`(`alert_id` ASC) USING BTREE,
  CONSTRAINT `fk_notification_alert` FOREIGN KEY (`alert_id`) REFERENCES `alert_event` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notification
-- ----------------------------

-- ----------------------------
-- Table structure for page_visit_log
-- ----------------------------
DROP TABLE IF EXISTS `page_visit_log`;
CREATE TABLE `page_visit_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问日志ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '访问用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '访问用户名',
  `page_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '页面编码',
  `page_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '页面名称',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '访问IP',
  `user_agent` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浏览器标识',
  `visit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_page_visit_code_time`(`page_code` ASC, `visit_time` ASC) USING BTREE,
  INDEX `idx_page_visit_user_time`(`user_id` ASC, `visit_time` ASC) USING BTREE,
  INDEX `idx_page_visit_time`(`visit_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '页面访问日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of page_visit_log
-- ----------------------------

-- ----------------------------
-- Table structure for planting_batch
-- ----------------------------
DROP TABLE IF EXISTS `planting_batch`;
CREATE TABLE `planting_batch`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '种植批次ID',
  `plot_id` bigint NOT NULL COMMENT '地块ID',
  `crop_id` bigint NOT NULL COMMENT '作物ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `batch_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '批次编号',
  `planting_area` decimal(10, 2) NULL DEFAULT NULL COMMENT '本批次种植面积',
  `area_unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '亩' COMMENT '面积单位',
  `planted_at` date NULL DEFAULT NULL COMMENT '种植日期',
  `expected_harvest_at` date NULL DEFAULT NULL COMMENT '预计采收日期',
  `actual_harvest_at` date NULL DEFAULT NULL COMMENT '实际采收日期',
  `growth_stage` tinyint NULL DEFAULT 1 COMMENT '生长阶段（1育苗期 2生长期 3成熟期 4采收期）',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1种植中 2已采收 3已失败 4已取消）',
  `yield_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '实际产量',
  `yield_unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'kg' COMMENT '产量单位',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_planting_batch_plot_id`(`plot_id` ASC) USING BTREE,
  INDEX `idx_planting_batch_crop_id`(`crop_id` ASC) USING BTREE,
  INDEX `idx_planting_batch_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_planting_batch_status`(`status` ASC) USING BTREE,
  INDEX `idx_planting_batch_planted_at`(`planted_at` ASC) USING BTREE,
  INDEX `idx_planting_batch_growth_stage_id`(`growth_stage_id` ASC) USING BTREE,
  CONSTRAINT `fk_planting_batch_crop` FOREIGN KEY (`crop_id`) REFERENCES `crop` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_planting_batch_plot` FOREIGN KEY (`plot_id`) REFERENCES `plot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_planting_batch_growth_stage` FOREIGN KEY (`growth_stage_id`) REFERENCES `growth_stage` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_planting_batch_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '种植批次表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of planting_batch
-- ----------------------------

-- ----------------------------
-- Table structure for plot
-- ----------------------------
DROP TABLE IF EXISTS `plot`;
CREATE TABLE `plot`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '地块ID',
  `farm_id` bigint NULL DEFAULT NULL COMMENT '所属农场ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `plot_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '地块名称',
  `plot_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地块编号，如3号棚、A区01',
  `crop_image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '作物图片地址',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地块位置',
  `region` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属地区',
  `area` decimal(10, 2) NULL DEFAULT NULL COMMENT '地块面积',
  `area_unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '亩' COMMENT '面积单位',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `coordinate` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '经纬度（经度,纬度）',
  `environment_type` tinyint NULL DEFAULT 1 COMMENT '种植环境（1大棚 2露天 3室内 4水培）',
  `cover_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地块封面图',
  `longitude` decimal(10, 6) NULL DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10, 6) NULL DEFAULT NULL COMMENT '纬度',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1正常 0停用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_farm_plot_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_farm_plot_status`(`status` ASC) USING BTREE,
  INDEX `idx_farm_plot_name`(`plot_name` ASC) USING BTREE,
  INDEX `idx_plot_type`(`type` ASC) USING BTREE,
  CONSTRAINT `fk_farm_plot_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '地块表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of plot
-- ----------------------------

-- ----------------------------
-- Table structure for sms_code
-- ----------------------------
DROP TABLE IF EXISTS `sms_code`;
CREATE TABLE `sms_code`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '短信验证码ID',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号',
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '验证码',
  `scene` tinyint NOT NULL COMMENT '使用场景（1登录 2注册 3找回密码 4绑定手机号 5修改手机号）',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `used` tinyint NULL DEFAULT 0 COMMENT '是否已使用（0未使用 1已使用）',
  `used_time` datetime NULL DEFAULT NULL COMMENT '使用时间',
  `send_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送请求IP',
  `user_agent` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求客户端信息',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1正常 0失效）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sms_code_phone`(`phone` ASC) USING BTREE,
  INDEX `idx_sms_code_scene`(`scene` ASC) USING BTREE,
  INDEX `idx_sms_code_expire_time`(`expire_time` ASC) USING BTREE,
  INDEX `idx_sms_code_used`(`used` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '短信验证码表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sms_code
-- ----------------------------

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_name`(`role_name` ASC) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role
-- ----------------------------
INSERT INTO `role` (`id`, `role_name`, `role_code`, `remark`) VALUES
(1, 'Farm Owner', 'farm_owner', 'Farm owner role'),
(2, 'Administrator', 'admin', 'System administrator role'),
(3, 'Technician', 'technician', 'Technician role'),
(4, 'Expert', 'expert', 'Expert role'),
(5, 'User', 'user', 'Normal user role');
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '加密密码',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `gender` tinyint NULL DEFAULT 0 COMMENT '性别（0未知 1男 2女）',
  `role_id` bigint NOT NULL DEFAULT 5 COMMENT '角色ID',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1正常 0禁用）',
  `region` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地区',
  `login_count` int NULL DEFAULT 0 COMMENT '登录次数',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `phone`(`phone` ASC) USING BTREE,
  INDEX `idx_user_role_id`(`role_id` ASC) USING BTREE,
  CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------

-- ----------------------------
-- Table structure for farm_owner_technician
-- ----------------------------
DROP TABLE IF EXISTS `farm_owner_technician`;
CREATE TABLE `farm_owner_technician`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '绑定ID',
  `owner_user_id` bigint NOT NULL COMMENT '农场主用户ID',
  `technician_user_id` bigint NOT NULL COMMENT '技术人员用户ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1启用 0停用）',
  `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_owner_technician`(`owner_user_id` ASC, `technician_user_id` ASC) USING BTREE,
  INDEX `idx_fot_owner`(`owner_user_id` ASC) USING BTREE,
  INDEX `idx_fot_technician`(`technician_user_id` ASC) USING BTREE,
  INDEX `idx_fot_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_fot_owner_user` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_fot_technician_user` FOREIGN KEY (`technician_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_fot_status` CHECK (`status` IN (0, 1))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '农场主技术人员绑定表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of farm_owner_technician
-- ----------------------------

-- ----------------------------
-- Table structure for user_oauth_account
-- ----------------------------
DROP TABLE IF EXISTS `user_oauth_account`;
CREATE TABLE `user_oauth_account`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '第三方账号ID',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `provider` tinyint NOT NULL COMMENT '第三方平台（1微信 2QQ 3支付宝 4苹果）',
  `open_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '第三方平台OpenID',
  `union_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方平台UnionID',
  `nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方头像',
  `access_token` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '访问令牌',
  `refresh_token` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '刷新令牌',
  `token_expire_time` datetime NULL DEFAULT NULL COMMENT '令牌过期时间',
  `bind_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1正常 0解绑）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_oauth_provider_openid`(`provider` ASC, `open_id` ASC) USING BTREE,
  INDEX `idx_oauth_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_oauth_union_id`(`union_id` ASC) USING BTREE,
  INDEX `idx_oauth_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_oauth_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '第三方登录账号表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_oauth_account
-- ----------------------------

-- ----------------------------
-- Table structure for warehouse_item
-- ----------------------------
DROP TABLE IF EXISTS `warehouse_item`;
CREATE TABLE `warehouse_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '仓库物资ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `inbound_operator_id` bigint NOT NULL COMMENT '初始入库人用户ID',
  `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '物资名称',
  `item_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '物资编码',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '物资图片',
  `category` tinyint NOT NULL COMMENT '物资分类（1种子 2肥料 3农药 4工具 5设备耗材 6其他）',
  `specification` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格型号，如500g/袋、20kg/桶',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '单位，如袋、瓶、kg、个',
  `stock_qty` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '当前库存数量',
  `warning_qty` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '库存预警数量',
  `manufacturer` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生产厂家',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1启用 0停用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_warehouse_item_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_warehouse_item_inbound_operator_id`(`inbound_operator_id` ASC) USING BTREE,
  INDEX `idx_warehouse_item_name`(`item_name` ASC) USING BTREE,
  INDEX `idx_warehouse_item_category`(`category` ASC) USING BTREE,
  INDEX `idx_warehouse_item_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_warehouse_item_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '仓库物资表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of warehouse_item
-- ----------------------------

-- ----------------------------
-- Table structure for warehouse_record
-- ----------------------------
DROP TABLE IF EXISTS `warehouse_record`;
CREATE TABLE `warehouse_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '出入库记录ID',
  `item_id` bigint NOT NULL COMMENT '仓库物资ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `operator_id` bigint NOT NULL COMMENT '操作人用户ID',
  `recipient` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '领用人或接收方',
  `record_type` tinyint NOT NULL COMMENT '记录类型（1入库 2出库 3库存调整）',
  `quantity` decimal(10, 2) NOT NULL COMMENT '本次数量',
  `before_qty` decimal(10, 2) NULL DEFAULT NULL COMMENT '操作前库存',
  `after_qty` decimal(10, 2) NULL DEFAULT NULL COMMENT '操作后库存',
  `related_plot_id` bigint NULL DEFAULT NULL COMMENT '关联地块ID，可为空',
  `related_task_id` bigint NULL DEFAULT NULL COMMENT '关联农事任务ID，可为空',
  `source_type` tinyint NULL DEFAULT NULL COMMENT '来源类型（1采购入库 2农事消耗 3盘点调整 4报损 5其他）',
  `supplier` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '单价',
  `total_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '总金额',
  `record_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（1正常 0删除）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_warehouse_record_item_id`(`item_id` ASC) USING BTREE,
  INDEX `idx_warehouse_record_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_warehouse_record_operator_id`(`operator_id` ASC) USING BTREE,
  INDEX `idx_warehouse_record_type`(`record_type` ASC) USING BTREE,
  INDEX `idx_warehouse_record_time`(`record_time` ASC) USING BTREE,
  INDEX `idx_warehouse_record_plot_id`(`related_plot_id` ASC) USING BTREE,
  INDEX `idx_warehouse_record_task_id`(`related_task_id` ASC) USING BTREE,
  CONSTRAINT `fk_warehouse_record_item` FOREIGN KEY (`item_id`) REFERENCES `warehouse_item` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_warehouse_record_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_warehouse_record_plot` FOREIGN KEY (`related_plot_id`) REFERENCES `plot` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_warehouse_record_task` FOREIGN KEY (`related_task_id`) REFERENCES `farm_task` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_warehouse_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '出入库记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of warehouse_record
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
