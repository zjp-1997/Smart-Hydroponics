/*
  四类设备数据表关联优化脚本。

  设计说明：
  1. environment_data、water_quality_data、light_data、pump_data 只保存 device_id。
     地块信息通过 iot_device.plot_id 获取，避免在数据表中重复保存 plot_id 导致设备与地块不一致。
  2. 四张数据表分别保存不同设备类型的指标，字段语义清晰，适合后台分别管理和统计。
  3. 外键只能保证 device_id 存在，不能保证设备类型正确，因此增加触发器校验设备类型。
  4. 复合索引用于优化按设备和采集时间查询的列表、详情和统计接口。
*/

ALTER TABLE `environment_data`
    ADD INDEX `idx_env_data_device_time` (`device_id`, `collect_time`);

ALTER TABLE `water_quality_data`
    ADD INDEX `idx_wq_data_device_time` (`device_id`, `collect_time`);

ALTER TABLE `light_data`
    ADD INDEX `idx_light_data_device_time` (`device_id`, `collect_time`);

ALTER TABLE `pump_data`
    ADD INDEX `idx_pump_data_device_time` (`device_id`, `collect_time`);

DROP TRIGGER IF EXISTS `trg_ed_check_device_type_bi`;
DROP TRIGGER IF EXISTS `trg_ed_check_device_type_bu`;
DROP TRIGGER IF EXISTS `trg_wq_check_device_type_bi`;
DROP TRIGGER IF EXISTS `trg_wq_check_device_type_bu`;
DROP TRIGGER IF EXISTS `trg_ld_check_device_type_bi`;
DROP TRIGGER IF EXISTS `trg_ld_check_device_type_bu`;
DROP TRIGGER IF EXISTS `trg_pd_check_device_type_bi`;
DROP TRIGGER IF EXISTS `trg_pd_check_device_type_bu`;

DELIMITER $$

CREATE TRIGGER `trg_ed_check_device_type_bi`
BEFORE INSERT ON `environment_data`
FOR EACH ROW
BEGIN
    DECLARE v_count INT DEFAULT 0;
    SELECT COUNT(1) INTO v_count
    FROM `iot_device` d
    JOIN `device_type` dt ON dt.id = d.type_id
    WHERE d.id = NEW.device_id AND dt.type_code = 'ENV_SENSOR';
    IF v_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'environment_data device must be ENV_SENSOR';
    END IF;
END$$

CREATE TRIGGER `trg_ed_check_device_type_bu`
BEFORE UPDATE ON `environment_data`
FOR EACH ROW
BEGIN
    DECLARE v_count INT DEFAULT 0;
    SELECT COUNT(1) INTO v_count
    FROM `iot_device` d
    JOIN `device_type` dt ON dt.id = d.type_id
    WHERE d.id = NEW.device_id AND dt.type_code = 'ENV_SENSOR';
    IF v_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'environment_data device must be ENV_SENSOR';
    END IF;
END$$

CREATE TRIGGER `trg_wq_check_device_type_bi`
BEFORE INSERT ON `water_quality_data`
FOR EACH ROW
BEGIN
    DECLARE v_count INT DEFAULT 0;
    SELECT COUNT(1) INTO v_count
    FROM `iot_device` d
    JOIN `device_type` dt ON dt.id = d.type_id
    WHERE d.id = NEW.device_id AND dt.type_code = 'WATER_QUALITY';
    IF v_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'water_quality_data device must be WATER_QUALITY';
    END IF;
END$$

CREATE TRIGGER `trg_wq_check_device_type_bu`
BEFORE UPDATE ON `water_quality_data`
FOR EACH ROW
BEGIN
    DECLARE v_count INT DEFAULT 0;
    SELECT COUNT(1) INTO v_count
    FROM `iot_device` d
    JOIN `device_type` dt ON dt.id = d.type_id
    WHERE d.id = NEW.device_id AND dt.type_code = 'WATER_QUALITY';
    IF v_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'water_quality_data device must be WATER_QUALITY';
    END IF;
END$$

CREATE TRIGGER `trg_ld_check_device_type_bi`
BEFORE INSERT ON `light_data`
FOR EACH ROW
BEGIN
    DECLARE v_count INT DEFAULT 0;
    SELECT COUNT(1) INTO v_count
    FROM `iot_device` d
    JOIN `device_type` dt ON dt.id = d.type_id
    WHERE d.id = NEW.device_id AND dt.type_code = 'GROW_LIGHT';
    IF v_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'light_data device must be GROW_LIGHT';
    END IF;
END$$

CREATE TRIGGER `trg_ld_check_device_type_bu`
BEFORE UPDATE ON `light_data`
FOR EACH ROW
BEGIN
    DECLARE v_count INT DEFAULT 0;
    SELECT COUNT(1) INTO v_count
    FROM `iot_device` d
    JOIN `device_type` dt ON dt.id = d.type_id
    WHERE d.id = NEW.device_id AND dt.type_code = 'GROW_LIGHT';
    IF v_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'light_data device must be GROW_LIGHT';
    END IF;
END$$

CREATE TRIGGER `trg_pd_check_device_type_bi`
BEFORE INSERT ON `pump_data`
FOR EACH ROW
BEGIN
    DECLARE v_count INT DEFAULT 0;
    SELECT COUNT(1) INTO v_count
    FROM `iot_device` d
    JOIN `device_type` dt ON dt.id = d.type_id
    WHERE d.id = NEW.device_id AND dt.type_code = 'WATER_PUMP';
    IF v_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'pump_data device must be WATER_PUMP';
    END IF;
END$$

CREATE TRIGGER `trg_pd_check_device_type_bu`
BEFORE UPDATE ON `pump_data`
FOR EACH ROW
BEGIN
    DECLARE v_count INT DEFAULT 0;
    SELECT COUNT(1) INTO v_count
    FROM `iot_device` d
    JOIN `device_type` dt ON dt.id = d.type_id
    WHERE d.id = NEW.device_id AND dt.type_code = 'WATER_PUMP';
    IF v_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'pump_data device must be WATER_PUMP';
    END IF;
END$$

DELIMITER ;
