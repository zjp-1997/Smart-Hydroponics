ALTER TABLE `warehouse_item`
    ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常 1已删除' AFTER `status`,
    ADD KEY `idx_warehouse_item_is_deleted` (`is_deleted`),
    ADD CONSTRAINT `chk_warehouse_item_is_deleted` CHECK (`is_deleted` IN (0, 1));

ALTER TABLE `iot_device_fault`
    ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常 1已删除' AFTER `handle_result`,
    ADD KEY `idx_fault_is_deleted` (`is_deleted`),
    ADD CONSTRAINT `chk_fault_is_deleted` CHECK (`is_deleted` IN (0, 1));

ALTER TABLE `iot_device_fault`
    DROP CHECK `chk_fault_status`,
    ADD CONSTRAINT `chk_fault_status` CHECK (`status` IN (0, 1, 2, 3));

DROP TRIGGER IF EXISTS `trg_iot_device_fault_bi`;
DROP TRIGGER IF EXISTS `trg_iot_device_fault_bu`;

DELIMITER $$
CREATE TRIGGER `trg_iot_device_fault_bi`
BEFORE INSERT ON `iot_device_fault`
FOR EACH ROW
BEGIN
    IF NEW.start_time IS NULL THEN
        SET NEW.start_time = NOW();
    END IF;
    IF NEW.status IN (2, 3) AND NEW.end_time IS NULL THEN
        SET NEW.end_time = NOW();
    END IF;
    IF NEW.end_time IS NOT NULL THEN
        SET NEW.duration = TIMESTAMPDIFF(MINUTE, NEW.start_time, NEW.end_time);
        IF NEW.status NOT IN (2, 3) THEN
            SET NEW.status = 2;
        END IF;
    END IF;
END$$

CREATE TRIGGER `trg_iot_device_fault_bu`
BEFORE UPDATE ON `iot_device_fault`
FOR EACH ROW
BEGIN
    IF NEW.status IN (2, 3) AND NEW.end_time IS NULL THEN
        SET NEW.end_time = NOW();
    END IF;
    IF NEW.end_time IS NOT NULL THEN
        SET NEW.duration = TIMESTAMPDIFF(MINUTE, NEW.start_time, NEW.end_time);
        IF NEW.status NOT IN (2, 3) THEN
            SET NEW.status = 2;
        END IF;
    END IF;
END$$
DELIMITER ;
