-- 维护消息独立保存故障快照；唯一故障索引保证重复调用不会重复发布。
CREATE TABLE IF NOT EXISTS maintenance_message (
 id BIGINT NOT NULL AUTO_INCREMENT COMMENT '维护消息ID',
 fault_id BIGINT NULL COMMENT '来源设备故障ID，保留历史不级联删除',
 device_id BIGINT NULL COMMENT '关联设备ID',
 device_name VARCHAR(100) NULL COMMENT '设备名称快照',
 status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0逻辑删除',
 version INT NOT NULL DEFAULT 0 COMMENT '编辑版本号，防止覆盖他人修改',
 publisher_id BIGINT NULL COMMENT '人工发布人ID，自动发布为空',
 owner_id BIGINT NOT NULL COMMENT '设备所属农场主ID',
 plot_id BIGINT NOT NULL COMMENT '故障发生地块ID',
 plot_name VARCHAR(100) NOT NULL COMMENT '发布时地块名称快照',
 title VARCHAR(100) NOT NULL COMMENT '消息标题',
 content_customized TINYINT NOT NULL DEFAULT 0 COMMENT '0系统默认正文 1用户提交正文，迁移不得覆盖',
 content TEXT NOT NULL COMMENT '消息正文快照',
 level TINYINT NOT NULL DEFAULT 1 COMMENT '1普通 2重要 3紧急',
 publisher_name VARCHAR(50) NOT NULL DEFAULT '系统自动发布' COMMENT '自动消息发布人',
 send_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
 PRIMARY KEY(id), UNIQUE KEY uk_maintenance_fault(fault_id),
 KEY idx_maintenance_owner_time(owner_id,send_time), KEY idx_maintenance_plot(plot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备故障维护消息';
