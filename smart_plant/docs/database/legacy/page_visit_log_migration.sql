-- Add page visit log for dynamic home dashboard visit trend.

CREATE TABLE IF NOT EXISTS `page_visit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问日志ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '访问用户ID',
  `username` varchar(50) NULL DEFAULT NULL COMMENT '访问用户名',
  `page_code` varchar(64) NOT NULL COMMENT '页面编码',
  `page_name` varchar(100) NOT NULL COMMENT '页面名称',
  `ip` varchar(64) NULL DEFAULT NULL COMMENT '访问IP',
  `user_agent` varchar(512) NULL DEFAULT NULL COMMENT '浏览器标识',
  `visit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_page_visit_code_time` (`page_code`, `visit_time`),
  KEY `idx_page_visit_user_time` (`user_id`, `visit_time`),
  KEY `idx_page_visit_time` (`visit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='页面访问日志表';

INSERT INTO `page_visit_log`
  (`user_id`, `username`, `page_code`, `page_name`, `ip`, `user_agent`, `visit_time`)
SELECT
  l.admin_id,
  l.username,
  'home_workbench',
  '首页工作台',
  l.ip,
  l.user_agent,
  l.login_time
FROM `admin_login_log` l
WHERE l.success = 1
  AND NOT EXISTS (
    SELECT 1
    FROM `page_visit_log` v
    WHERE v.page_code = 'home_workbench'
  );
