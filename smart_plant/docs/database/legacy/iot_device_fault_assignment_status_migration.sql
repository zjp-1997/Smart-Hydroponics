-- Add assignment acceptance status for device fault dispatch workflow.
--
-- assign_status:
-- 0 = unassigned
-- 1 = assigned, waiting for technician acceptance
-- 2 = accepted by technician
-- 3 = rejected by technician, waiting for reassignment

SET @has_assign_status := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'iot_device_fault'
    AND COLUMN_NAME = 'assign_status'
);

SET @add_assign_status_sql := IF(
  @has_assign_status = 0,
  'ALTER TABLE `iot_device_fault` ADD COLUMN `assign_status` tinyint NOT NULL DEFAULT 0 COMMENT ''分配状态（0未分配 1已分配待接受 2已接受 3已拒绝）'' AFTER `status`',
  'SELECT 1'
);
PREPARE add_assign_status_stmt FROM @add_assign_status_sql;
EXECUTE add_assign_status_stmt;
DEALLOCATE PREPARE add_assign_status_stmt;

UPDATE `iot_device_fault`
SET `assign_status` = CASE
  WHEN `handle_user_id` IS NULL THEN 0
  WHEN `status` = 1 THEN 2
  ELSE 1
END
WHERE `assign_status` = 0;

SET @has_assign_status_index := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'iot_device_fault'
    AND INDEX_NAME = 'idx_fault_assign_status'
);

SET @add_assign_status_index_sql := IF(
  @has_assign_status_index = 0,
  'ALTER TABLE `iot_device_fault` ADD INDEX `idx_fault_assign_status` (`assign_status` ASC)',
  'SELECT 1'
);
PREPARE add_assign_status_index_stmt FROM @add_assign_status_index_sql;
EXECUTE add_assign_status_index_stmt;
DEALLOCATE PREPARE add_assign_status_index_stmt;

SET @has_assign_status_check := (
  SELECT COUNT(1)
  FROM information_schema.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'iot_device_fault'
    AND CONSTRAINT_NAME = 'chk_fault_assign_status'
);

SET @add_assign_status_check_sql := IF(
  @has_assign_status_check = 0,
  'ALTER TABLE `iot_device_fault` ADD CONSTRAINT `chk_fault_assign_status` CHECK (`assign_status` IN (0, 1, 2, 3))',
  'SELECT 1'
);
PREPARE add_assign_status_check_stmt FROM @add_assign_status_check_sql;
EXECUTE add_assign_status_check_stmt;
DEALLOCATE PREPARE add_assign_status_check_stmt;
