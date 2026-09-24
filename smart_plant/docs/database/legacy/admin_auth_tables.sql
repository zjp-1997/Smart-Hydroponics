SET NAMES utf8mb4;

SET @user_table_exists = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user'
);
SET @login_count_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user'
    AND COLUMN_NAME = 'login_count'
);
SET @add_login_count_sql = IF(
  @user_table_exists > 0 AND @login_count_column_exists = 0,
  'ALTER TABLE `user` ADD COLUMN `login_count` int NULL DEFAULT 0 COMMENT ''登录次数'' AFTER `region`',
  'SELECT 1'
);
PREPARE add_login_count_stmt FROM @add_login_count_sql;
EXECUTE add_login_count_stmt;
DEALLOCATE PREPARE add_login_count_stmt;

CREATE TABLE IF NOT EXISTS `role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_name` (`role_name`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

INSERT INTO `role` (`id`, `role_name`, `role_code`, `remark`) VALUES
(1, 'Farm Owner', 'farm_owner', 'Farm owner role'),
(2, 'Administrator', 'admin', 'System administrator role'),
(3, 'Technician', 'technician', 'Technician role'),
(4, 'Expert', 'expert', 'Expert role'),
(5, 'User', 'user', 'Normal user role')
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `remark` = VALUES(`remark`);

CREATE TABLE IF NOT EXISTS `admin_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '登录日志ID',
  `admin_id` bigint DEFAULT NULL COMMENT '管理员用户ID，对应 user.id',
  `username` varchar(50) NOT NULL COMMENT '登录用户名',
  `success` tinyint NOT NULL COMMENT '是否成功，1成功 0失败',
  `failure_reason` varchar(100) DEFAULT NULL COMMENT '失败原因',
  `ip` varchar(50) DEFAULT NULL COMMENT '登录IP',
  `user_agent` varchar(255) DEFAULT NULL COMMENT 'User-Agent',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_login_log_admin_id` (`admin_id`),
  KEY `idx_admin_login_log_username` (`username`),
  KEY `idx_admin_login_log_ip` (`ip`),
  KEY `idx_admin_login_log_time` (`login_time`),
  CONSTRAINT `chk_admin_login_log_success` CHECK (`success` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员登录日志表';

-- Security note: this legacy script intentionally does not create an administrator
-- account. Provision the initial administrator through the approved deployment
-- process with a unique, high-entropy password hash and force credential rotation.
