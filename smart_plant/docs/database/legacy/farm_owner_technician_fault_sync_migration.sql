-- Add farm owner to technician binding and backfill device fault records
-- for devices whose health status is already fault or maintenance.

CREATE TABLE IF NOT EXISTS `farm_owner_technician` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '绑定ID',
  `owner_user_id` bigint NOT NULL COMMENT '农场主用户ID',
  `technician_user_id` bigint NOT NULL COMMENT '技术人员用户ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1启用 0停用）',
  `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `remark` varchar(255) NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner_technician` (`owner_user_id`, `technician_user_id`),
  KEY `idx_fot_owner` (`owner_user_id`),
  KEY `idx_fot_technician` (`technician_user_id`),
  KEY `idx_fot_status` (`status`),
  CONSTRAINT `fk_fot_owner_user` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_fot_technician_user` FOREIGN KEY (`technician_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_fot_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农场主技术人员绑定表';

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

INSERT INTO `farm_owner_technician` (`owner_user_id`, `technician_user_id`, `remark`)
SELECT owner_user.id, technician_user.id, '系统初始化默认绑定'
FROM `user` owner_user
INNER JOIN `role` owner_role ON owner_role.id = owner_user.role_id
INNER JOIN (
  SELECT tu.id
  FROM `user` tu
  INNER JOIN `role` tr ON tr.id = tu.role_id
  WHERE tu.status = 1
    AND LOWER(tr.role_code) = 'technician'
  ORDER BY tu.id ASC
  LIMIT 1
) technician_user
WHERE owner_user.status = 1
  AND LOWER(owner_role.role_code) = 'farm_owner'
  AND NOT EXISTS (
    SELECT 1
    FROM `farm_owner_technician` existing
    WHERE existing.owner_user_id = owner_user.id
      AND existing.status = 1
  )
ON DUPLICATE KEY UPDATE status = VALUES(status);

UPDATE `iot_device`
SET `control_status` = 0,
    `online_status` = 0,
    `offline_time` = COALESCE(`offline_time`, NOW()),
    `fault_time` = COALESCE(`fault_time`, NOW())
WHERE `health_status` IN (1, 2);

DROP TEMPORARY TABLE IF EXISTS `tmp_unhealthy_iot_device_fault`;
CREATE TEMPORARY TABLE `tmp_unhealthy_iot_device_fault` AS
SELECT
  d.id AS device_id,
  CONCAT('DF_', d.id, '_', DATE_FORMAT(COALESCE(d.fault_time, d.update_time, NOW()), '%Y%m%d%H%i%s')) AS fault_code,
  CONCAT(d.name, '设备故障') AS fault_name,
  4 AS fault_type,
  3 AS severity,
  '设备健康状态为故障，系统自动生成故障记录' AS fault_desc,
  COALESCE(d.fault_time, d.update_time, NOW()) AS start_time,
  0 AS status,
  CASE WHEN tech.id IS NULL THEN 0 ELSE 1 END AS assign_status,
  tech.id AS handle_user_id
FROM `iot_device` d
LEFT JOIN (
  SELECT fot.owner_user_id, MAX(fot.technician_user_id) AS technician_user_id
  FROM `farm_owner_technician` fot
  INNER JOIN `user` tu ON tu.id = fot.technician_user_id
  INNER JOIN `role` tr ON tr.id = tu.role_id
  WHERE fot.status = 1
    AND tu.status = 1
    AND LOWER(tr.role_code) = 'technician'
  GROUP BY fot.owner_user_id
) bound ON bound.owner_user_id = d.user_id
LEFT JOIN `user` tech ON tech.id = bound.technician_user_id
WHERE d.health_status = 1
  AND NOT EXISTS (
    SELECT 1
    FROM `iot_device_fault` existing
    WHERE existing.device_id = d.id
      AND existing.status IN (0, 1)
  );

INSERT INTO `iot_device_fault`
  (`device_id`, `fault_code`, `fault_name`, `fault_type`, `severity`, `fault_desc`,
   `start_time`, `status`, `assign_status`, `handle_user_id`)
SELECT *
FROM `tmp_unhealthy_iot_device_fault`;

DROP TEMPORARY TABLE IF EXISTS `tmp_unhealthy_iot_device_fault`;
