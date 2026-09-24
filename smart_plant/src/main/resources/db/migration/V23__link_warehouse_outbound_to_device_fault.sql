ALTER TABLE `warehouse_record`
    ADD COLUMN `related_fault_id` BIGINT DEFAULT NULL COMMENT '关联设备故障ID，出库时必填' AFTER `related_task_id`,
    ADD KEY `idx_warehouse_record_fault_id` (`related_fault_id`),
    ADD CONSTRAINT `fk_warehouse_record_fault`
        FOREIGN KEY (`related_fault_id`) REFERENCES `iot_device_fault` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT;
