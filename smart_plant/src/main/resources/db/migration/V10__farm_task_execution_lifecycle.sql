-- 农事任务生命周期升级：以单一截至时间替代计划起止区间，并补全可追溯的执行事件字段。
-- 迁移遵循“先复制数据、再删除旧字段”的顺序，避免历史计划时间丢失。

ALTER TABLE `farm_task`
  ADD COLUMN `deadline_time` datetime DEFAULT NULL COMMENT '任务截至时间' AFTER `priority`;

-- 历史任务优先沿用计划结束时间；没有计划结束时间时退回计划开始时间。
UPDATE `farm_task`
SET `deadline_time` = COALESCE(`planned_end_time`, `planned_start_time`)
WHERE `deadline_time` IS NULL;

ALTER TABLE `farm_task`
  DROP CHECK `chk_task_plan_time`,
  DROP INDEX `idx_farm_task_plan_time`,
  DROP INDEX `idx_farm_task_user_status_time`,
  DROP COLUMN `planned_start_time`,
  DROP COLUMN `planned_end_time`,
  ADD INDEX `idx_farm_task_deadline` (`deadline_time`),
  ADD INDEX `idx_farm_task_user_status_deadline` (`user_id`, `status`, `deadline_time`);

-- 执行记录是面向业务的不可变时间线；新增字段支持进度、来源和幂等控制。
ALTER TABLE `farm_task_record`
  ADD COLUMN `operator_name_snapshot` varchar(100) DEFAULT NULL COMMENT '操作人名称快照' AFTER `operator_id`,
  ADD COLUMN `progress_percent` tinyint DEFAULT NULL COMMENT '执行进度百分比（0-100）' AFTER `result_status`,
  ADD COLUMN `attachments` text COMMENT '附件地址JSON数组' AFTER `optimize_suggestion`,
  ADD COLUMN `request_id` varchar(64) DEFAULT NULL COMMENT '客户端幂等请求号' AFTER `attachments`,
  ADD COLUMN `source_client` varchar(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '事件来源：FARM_APP/SMART_FARM/SYSTEM' AFTER `request_id`,
  ADD UNIQUE INDEX `uk_farm_task_record_request_id` (`request_id`),
  ADD INDEX `idx_farm_task_record_task_time` (`task_id`, `execute_time`, `id`),
  ADD CONSTRAINT `fk_farm_task_record_task` FOREIGN KEY (`task_id`) REFERENCES `farm_task` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  ADD CONSTRAINT `fk_farm_task_record_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  ADD CONSTRAINT `chk_farm_task_record_progress` CHECK (`progress_percent` IS NULL OR (`progress_percent` BETWEEN 0 AND 100)),
  ADD CONSTRAINT `chk_farm_task_record_result` CHECK (`result_status` IS NULL OR `result_status` IN (1, 2, 3));

-- 旧的 farm_task_log 与业务执行记录职责重复，暂时保留只读兼容；新业务统一写入 farm_task_record。
