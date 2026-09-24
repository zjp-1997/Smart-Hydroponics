-- 每次手工出入库提交使用唯一业务请求键；历史自动入库流水允许为空。
ALTER TABLE `warehouse_record`
    ADD COLUMN `request_id` VARCHAR(64) NULL COMMENT '业务请求幂等键' AFTER `id`,
    ADD UNIQUE KEY `uk_warehouse_record_request_id` (`request_id`);
