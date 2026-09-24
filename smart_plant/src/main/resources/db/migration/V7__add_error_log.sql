-- 错误日志保存不可变的异常现场，以及可由管理员维护的处理状态。
CREATE TABLE IF NOT EXISTS `error_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '错误日志ID',
    `trace_id` varchar(64) NOT NULL COMMENT '对外追踪编号',
    `level` varchar(16) NOT NULL DEFAULT 'ERROR' COMMENT '日志级别',
    `exception_type` varchar(255) NOT NULL COMMENT '异常类型',
    `error_message` varchar(1000) NULL COMMENT '异常摘要',
    `request_method` varchar(10) NULL COMMENT 'HTTP请求方法',
    `request_uri` varchar(500) NULL COMMENT '请求路径，不包含查询参数和请求体',
    `operator_id` bigint NULL COMMENT '操作人ID',
    `operator_name` varchar(50) NULL COMMENT '操作人用户名',
    `ip` varchar(64) NULL COMMENT '客户端IP',
    `user_agent` varchar(255) NULL COMMENT '浏览器标识',
    `stack_trace` mediumtext NULL COMMENT '异常堆栈，详情页按需读取',
    `handle_status` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态：0未处理 1已处理 2已忽略',
    `handle_remark` varchar(500) NULL COMMENT '处理备注',
    `handler_id` bigint NULL COMMENT '处理人ID',
    `handler_name` varchar(50) NULL COMMENT '处理人用户名',
    `handle_time` datetime NULL COMMENT '最近处理时间',
    `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '错误发生时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_error_log_trace_id` (`trace_id`),
    KEY `idx_error_log_status_time` (`handle_status`, `create_time`),
    KEY `idx_error_log_operator` (`operator_id`),
    CONSTRAINT `chk_error_log_status` CHECK (`handle_status` IN (0, 1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统错误日志表';

-- 菜单权限独立配置；管理员仍由系统通配权限访问。
INSERT IGNORE INTO `permission`
(`parent_id`, `permission_name`, `permission_code`, `type`, `path`, `component`, `status`, `sort`)
VALUES (0, '错误日志', 'error_log:manage', 1, '/log/error', 'errorLog/ListView', 1, 120);
