/*
 Navicat Premium 17 optimization script for smart_plant

 Usage:
 1. Open Navicat Premium 17.
 2. Select database: smart_plant.
 3. Open Query, paste or run this file.
 4. Execute after smart_plant.sql has been imported successfully.

 Notes:
 - Run once only. Re-running may report duplicate index/constraint names.
 - If existing data violates a rule, fix the data first, then execute again.
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- 1. Unique constraints
-- ----------------------------
ALTER TABLE `user`
  ADD UNIQUE INDEX `uk_user_username` (`username` ASC),
  ADD UNIQUE INDEX `uk_user_email` (`email` ASC);

ALTER TABLE `plot`
  ADD UNIQUE INDEX `uk_plot_user_code` (`user_id` ASC, `plot_code` ASC),
  ADD UNIQUE INDEX `uk_plot_id_user` (`id` ASC, `user_id` ASC);

ALTER TABLE `planting_batch`
  ADD UNIQUE INDEX `uk_batch_user_no` (`user_id` ASC, `batch_no` ASC),
  ADD UNIQUE INDEX `uk_batch_id_user_plot` (`id` ASC, `user_id` ASC, `plot_id` ASC);

ALTER TABLE `iot_device`
  ADD UNIQUE INDEX `uk_device_id_user_plot` (`id` ASC, `user_id` ASC, `plot_id` ASC),
  ADD INDEX `idx_iot_device_install_time` (`install_time` ASC),
  ADD INDEX `idx_iot_device_offline_time` (`offline_time` ASC),
  ADD INDEX `idx_iot_device_fault_time` (`fault_time` ASC);

ALTER TABLE `warehouse_item`
  ADD UNIQUE INDEX `uk_warehouse_item_user_code` (`user_id` ASC, `item_code` ASC);

-- ----------------------------
-- 2. Composite indexes for common queries
-- ----------------------------
ALTER TABLE `ai_chat_message`
  ADD INDEX `idx_ai_message_session_time` (`session_id` ASC, `create_time` ASC);

ALTER TABLE `consult_message`
  ADD INDEX `idx_consult_message_session_time` (`session_id` ASC, `create_time` ASC);

ALTER TABLE `farm_task`
  ADD INDEX `idx_farm_task_user_status_time` (`user_id` ASC, `status` ASC, `planned_start_time` ASC),
  ADD INDEX `idx_farm_task_batch_user_plot` (`batch_id` ASC, `user_id` ASC, `plot_id` ASC);

ALTER TABLE `camera_capture_plan`
  ADD INDEX `idx_capture_plan_device_user_plot` (`device_id` ASC, `user_id` ASC, `plot_id` ASC);

ALTER TABLE `camera_capture_record`
  ADD INDEX `idx_camera_record_device_time` (`device_id` ASC, `capture_time` ASC),
  ADD INDEX `idx_camera_record_device_user_plot` (`device_id` ASC, `user_id` ASC, `plot_id` ASC),
  ADD INDEX `idx_camera_record_batch_user_plot` (`batch_id` ASC, `user_id` ASC, `plot_id` ASC);

ALTER TABLE `alert_event`
  ADD INDEX `idx_alert_user_process_time` (`user_id` ASC, `process_status` ASC, `trigger_time` ASC),
  ADD INDEX `idx_alert_batch_user_plot` (`batch_id` ASC, `user_id` ASC, `plot_id` ASC);

ALTER TABLE `notification`
  ADD INDEX `idx_notification_user_read_time` (`user_id` ASC, `is_read` ASC, `send_time` ASC);

ALTER TABLE `sms_code`
  ADD INDEX `idx_sms_phone_scene_used_expire` (`phone` ASC, `scene` ASC, `used` ASC, `expire_time` ASC);

ALTER TABLE `warehouse_record`
  ADD INDEX `idx_warehouse_record_item_time` (`item_id` ASC, `record_time` ASC);

-- ----------------------------
-- 3. Composite foreign keys to reduce inconsistent redundant IDs
-- ----------------------------
ALTER TABLE `planting_batch`
  ADD CONSTRAINT `fk_batch_plot_user`
  FOREIGN KEY (`plot_id`, `user_id`) REFERENCES `plot` (`id`, `user_id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `planting_batch`
  ADD CONSTRAINT `fk_planting_batch_growth_stage`
  FOREIGN KEY (`growth_stage_id`) REFERENCES `growth_stage` (`id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `farm_task`
  ADD CONSTRAINT `fk_farm_task_batch_user_plot`
  FOREIGN KEY (`batch_id`, `user_id`, `plot_id`) REFERENCES `planting_batch` (`id`, `user_id`, `plot_id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `camera_capture_plan`
  ADD CONSTRAINT `fk_capture_plan_device_user_plot`
  FOREIGN KEY (`device_id`, `user_id`, `plot_id`) REFERENCES `iot_device` (`id`, `user_id`, `plot_id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `camera_capture_record`
  ADD CONSTRAINT `fk_camera_record_batch_user_plot`
  FOREIGN KEY (`batch_id`, `user_id`, `plot_id`) REFERENCES `planting_batch` (`id`, `user_id`, `plot_id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `alert_event`
  ADD CONSTRAINT `fk_alert_batch_user_plot`
  FOREIGN KEY (`batch_id`, `user_id`, `plot_id`) REFERENCES `planting_batch` (`id`, `user_id`, `plot_id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT;

-- ----------------------------
-- 4. More suitable delete rules for detail/log tables
-- ----------------------------
ALTER TABLE `ai_chat_message` DROP FOREIGN KEY `fk_ai_chat_message_session`;
ALTER TABLE `ai_chat_message`
  ADD CONSTRAINT `fk_ai_chat_message_session`
  FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session` (`id`)
  ON DELETE CASCADE ON UPDATE RESTRICT;

ALTER TABLE `consult_message` DROP FOREIGN KEY `fk_consult_message_session`;
ALTER TABLE `consult_message`
  ADD CONSTRAINT `fk_consult_message_session`
  FOREIGN KEY (`session_id`) REFERENCES `consult_session` (`id`)
  ON DELETE CASCADE ON UPDATE RESTRICT;

ALTER TABLE `farm_task_log` DROP FOREIGN KEY `fk_task_log_task`;
ALTER TABLE `farm_task_log`
  ADD CONSTRAINT `fk_task_log_task`
  FOREIGN KEY (`task_id`) REFERENCES `farm_task` (`id`)
  ON DELETE CASCADE ON UPDATE RESTRICT;

ALTER TABLE `warehouse_record` DROP FOREIGN KEY `fk_warehouse_record_item`;
ALTER TABLE `warehouse_record`
  ADD CONSTRAINT `fk_warehouse_record_item`
  FOREIGN KEY (`item_id`) REFERENCES `warehouse_item` (`id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT;

-- ----------------------------
-- 5. Check constraints for enum-like and numeric fields
-- ----------------------------
ALTER TABLE `user`
  ADD CONSTRAINT `chk_user_gender` CHECK (`gender` IN (0, 1, 2)),
  ADD CONSTRAINT `chk_user_status` CHECK (`status` IN (0, 1));

ALTER TABLE `sms_code`
  ADD CONSTRAINT `chk_sms_scene` CHECK (`scene` IN (1, 2, 3, 4, 5)),
  ADD CONSTRAINT `chk_sms_used` CHECK (`used` IN (0, 1)),
  ADD CONSTRAINT `chk_sms_status` CHECK (`status` IN (0, 1)),
  ADD CONSTRAINT `chk_sms_time` CHECK (
    (`used` = 0 AND `used_time` IS NULL)
    OR (`used` = 1 AND `used_time` IS NOT NULL AND `used_time` <= `expire_time`)
  );

ALTER TABLE `plot`
  ADD CONSTRAINT `chk_plot_area` CHECK (`area` IS NULL OR `area` >= 0),
  ADD CONSTRAINT `chk_plot_environment_type` CHECK (`environment_type` IN (1, 2, 3, 4)),
  ADD CONSTRAINT `chk_plot_type` CHECK (`type` IS NULL OR `type` IN (1, 2, 3)),
  ADD CONSTRAINT `chk_plot_longitude` CHECK (`longitude` IS NULL OR (`longitude` >= -180 AND `longitude` <= 180)),
  ADD CONSTRAINT `chk_plot_latitude` CHECK (`latitude` IS NULL OR (`latitude` >= -90 AND `latitude` <= 90)),
  ADD CONSTRAINT `chk_plot_status` CHECK (`status` IN (0, 1));

ALTER TABLE `crop`
  ADD CONSTRAINT `chk_crop_growth_days` CHECK (`growth_days` IS NULL OR `growth_days` > 0),
  ADD CONSTRAINT `chk_crop_status` CHECK (`status` IN (0, 1));

ALTER TABLE `disease_pest`
  ADD CONSTRAINT `chk_disease_pest_type` CHECK (`type` IN (1, 2, 3)),
  ADD CONSTRAINT `chk_disease_pest_severity` CHECK (`severity_level` IN (1, 2, 3)),
  ADD CONSTRAINT `chk_disease_pest_status` CHECK (`status` IN (0, 1));

ALTER TABLE `disease_control`
  ADD CONSTRAINT `chk_disease_control_type` CHECK (`control_type` IN (1, 2)),
  ADD CONSTRAINT `chk_disease_control_status` CHECK (`status` IN (0, 1));

ALTER TABLE `planting_batch`
  ADD CONSTRAINT `chk_batch_area` CHECK (`planting_area` IS NULL OR `planting_area` >= 0),
  ADD CONSTRAINT `chk_batch_status` CHECK (`status` IN (1, 2, 3, 4)),
  ADD CONSTRAINT `chk_batch_expected_yield` CHECK (`expected_yield_amount` IS NULL OR `expected_yield_amount` >= 0),
  ADD CONSTRAINT `chk_batch_grown_days` CHECK (`grown_days` IS NULL OR `grown_days` >= 0),
  ADD CONSTRAINT `chk_batch_yield` CHECK (`yield_amount` IS NULL OR `yield_amount` >= 0),
  ADD CONSTRAINT `chk_batch_date` CHECK (`expected_harvest_at` IS NULL OR `planted_at` IS NULL OR `expected_harvest_at` >= `planted_at`);

ALTER TABLE `expert_profile`
  ADD CONSTRAINT `chk_expert_rating` CHECK (`rating` IS NULL OR (`rating` >= 0 AND `rating` <= 5)),
  ADD CONSTRAINT `chk_expert_consultation_count` CHECK (`consultation_count` >= 0),
  ADD CONSTRAINT `chk_expert_audit_status` CHECK (`audit_status` IN (1, 2, 3)),
  ADD CONSTRAINT `chk_expert_service_status` CHECK (`service_status` IN (0, 1)),
  ADD CONSTRAINT `chk_expert_status` CHECK (`status` IN (0, 1));

ALTER TABLE `consult_session`
  ADD CONSTRAINT `chk_consult_type` CHECK (`consult_type` IN (1, 2, 3, 4, 5)),
  ADD CONSTRAINT `chk_consult_status` CHECK (`status` IN (1, 2, 3)),
  ADD CONSTRAINT `chk_consult_status_flag` CHECK (`status_flag` IN (0, 1)),
  ADD CONSTRAINT `chk_consult_rating` CHECK (`rating` IS NULL OR (`rating` >= 0 AND `rating` <= 5)),
  ADD CONSTRAINT `chk_consult_unread_count` CHECK (`user_unread_count` >= 0 AND `expert_unread_count` >= 0);

ALTER TABLE `consult_message`
  ADD CONSTRAINT `chk_consult_message_type` CHECK (`message_type` IN (1, 2, 3, 4)),
  ADD CONSTRAINT `chk_consult_message_read` CHECK (`is_read` IN (0, 1)),
  ADD CONSTRAINT `chk_consult_message_status` CHECK (`status` IN (0, 1));

ALTER TABLE `farm_task`
  ADD CONSTRAINT `chk_task_type` CHECK (`task_type` IN (1, 2, 3, 4, 5, 6, 7, 8)),
  ADD CONSTRAINT `chk_task_priority` CHECK (`priority` IN (1, 2, 3, 4)),
  ADD CONSTRAINT `chk_task_status` CHECK (`status` IN (1, 2, 3, 4, 5)),
  ADD CONSTRAINT `chk_task_plan_time` CHECK (`planned_end_time` IS NULL OR `planned_start_time` IS NULL OR `planned_end_time` >= `planned_start_time`),
  ADD CONSTRAINT `chk_task_actual_time` CHECK (`actual_end_time` IS NULL OR `actual_start_time` IS NULL OR `actual_end_time` >= `actual_start_time`);

ALTER TABLE `farm_task_log`
  ADD CONSTRAINT `chk_task_log_action_type` CHECK (`action_type` IN (1, 2, 3, 4, 5, 6));

ALTER TABLE `iot_device`
  ADD CONSTRAINT `chk_iot_device_control_status_num` CHECK (`control_status` IN (0, 1)),
  ADD CONSTRAINT `chk_iot_device_online_status_num` CHECK (`online_status` IN (0, 1)),
  ADD CONSTRAINT `chk_iot_device_health_status_num` CHECK (`health_status` IN (0, 1, 2)),
  ADD CONSTRAINT `chk_iot_device_online_duration_nonnegative` CHECK (`online_duration` >= 0),
  ADD CONSTRAINT `chk_iot_device_offline_time_after_install` CHECK (`offline_time` IS NULL OR `offline_time` >= `install_time`),
  ADD CONSTRAINT `chk_iot_device_fault_time_after_install` CHECK (`fault_time` IS NULL OR `fault_time` >= `install_time`),
  ADD CONSTRAINT `chk_iot_device_unhealthy_offline_closed` CHECK (`health_status` NOT IN (1, 2) OR (`control_status` = 0 AND `online_status` = 0));

ALTER TABLE `iot_device_fault`
  ADD INDEX `idx_fault_assign_status` (`assign_status` ASC),
  ADD CONSTRAINT `chk_fault_assign_status` CHECK (`assign_status` IN (0, 1, 2, 3));

ALTER TABLE `camera_capture_plan`
  ADD CONSTRAINT `chk_capture_plan_interval` CHECK (`interval_minutes` > 0),
  ADD CONSTRAINT `chk_capture_plan_enabled` CHECK (`enabled` IN (0, 1)),
  ADD CONSTRAINT `chk_capture_plan_time` CHECK (`end_time` IS NULL OR `start_time` IS NULL OR `end_time` > `start_time`);

ALTER TABLE `camera_capture_record`
  ADD CONSTRAINT `chk_camera_record_type` CHECK (`capture_type` IN (1, 2, 3)),
  ADD CONSTRAINT `chk_camera_record_ai_checked` CHECK (`ai_checked` IN (0, 1)),
  ADD CONSTRAINT `chk_camera_record_status` CHECK (`status` IN (0, 1));

ALTER TABLE `ai_recognition_type`
  ADD CONSTRAINT `chk_ai_recognition_type_status` CHECK (`status` IN (0, 1));

ALTER TABLE `ai_recognition_record`
  ADD CONSTRAINT `chk_ai_record_source_type` CHECK (`source_type` IN (1, 2)),
  ADD CONSTRAINT `chk_ai_record_status` CHECK (`status` IN (1, 2, 3, 4)),
  ADD CONSTRAINT `chk_ai_record_source_relation` CHECK (
    (`source_type` = 1 AND `camera_id` IS NULL AND `plot_id` IS NULL)
    OR (`source_type` = 2 AND `camera_id` IS NOT NULL AND `plot_id` IS NOT NULL)
  );

ALTER TABLE `ai_recognition_result`
  ADD CONSTRAINT `chk_ai_result_confidence` CHECK (`confidence` IS NULL OR (`confidence` >= 0 AND `confidence` <= 100)),
  ADD CONSTRAINT `chk_ai_result_severity` CHECK (`severity_level` IS NULL OR `severity_level` IN (1, 2, 3)),
  ADD CONSTRAINT `chk_ai_result_status` CHECK (`status` IN (1, 2, 3));

ALTER TABLE `ai_chat_session`
  ADD CONSTRAINT `chk_ai_chat_session_scene` CHECK (`scene` IN (1, 2, 3, 4, 5)),
  ADD CONSTRAINT `chk_ai_chat_session_status` CHECK (`status` IN (0, 1, 2));

ALTER TABLE `ai_chat_message`
  ADD CONSTRAINT `chk_ai_chat_message_role` CHECK (`role` IN (1, 2, 3)),
  ADD CONSTRAINT `chk_ai_chat_message_type` CHECK (`message_type` IN (1, 2, 3, 4)),
  ADD CONSTRAINT `chk_ai_chat_message_tokens` CHECK (`prompt_tokens` >= 0 AND `completion_tokens` >= 0 AND `total_tokens` >= 0),
  ADD CONSTRAINT `chk_ai_chat_message_status` CHECK (`status` IN (0, 1, 2));

ALTER TABLE `alert_event`
  ADD CONSTRAINT `chk_alert_type` CHECK (`alert_type` IN (1, 2, 3, 4, 5, 6)),
  ADD CONSTRAINT `chk_alert_metric_value` CHECK (`threshold_min` IS NULL OR `threshold_max` IS NULL OR `threshold_max` >= `threshold_min`),
  ADD CONSTRAINT `chk_alert_level` CHECK (`alert_level` IN (1, 2, 3)),
  ADD CONSTRAINT `chk_alert_process_status` CHECK (`process_status` IN (1, 2, 3, 4)),
  ADD CONSTRAINT `chk_alert_status` CHECK (`status` IN (0, 1));

ALTER TABLE `notification`
  ADD CONSTRAINT `chk_notification_type` CHECK (`notice_type` IN (1, 2, 3, 4, 5, 6, 7)),
  ADD CONSTRAINT `chk_notification_level` CHECK (`level` IN (1, 2, 3)),
  ADD CONSTRAINT `chk_notification_read` CHECK (`is_read` IN (0, 1)),
  ADD CONSTRAINT `chk_notification_status` CHECK (`status` IN (0, 1));

ALTER TABLE `warehouse_item`
  ADD CONSTRAINT `chk_warehouse_item_category` CHECK (`category` IN (1, 2, 3, 4, 5, 6)),
  ADD CONSTRAINT `chk_warehouse_item_stock` CHECK (`stock_qty` >= 0),
  ADD CONSTRAINT `chk_warehouse_item_warning` CHECK (`warning_qty` >= 0),
  ADD CONSTRAINT `chk_warehouse_item_status` CHECK (`status` IN (0, 1));

ALTER TABLE `warehouse_record`
  ADD CONSTRAINT `chk_warehouse_record_type` CHECK (`record_type` IN (1, 2, 3)),
  ADD CONSTRAINT `chk_warehouse_record_quantity` CHECK (`quantity` > 0),
  ADD CONSTRAINT `chk_warehouse_record_qty_snapshot` CHECK (
    (`before_qty` IS NULL OR `before_qty` >= 0)
    AND (`after_qty` IS NULL OR `after_qty` >= 0)
  ),
  ADD CONSTRAINT `chk_warehouse_record_amount` CHECK (
    (`price` IS NULL OR `price` >= 0)
    AND (`total_amount` IS NULL OR `total_amount` >= 0)
  ),
  ADD CONSTRAINT `chk_warehouse_record_status` CHECK (`status` IN (0, 1));

SET FOREIGN_KEY_CHECKS = 1;
