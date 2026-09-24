-- Keep database-direct IoT ingestion safe as well: validate the device type and
-- always derive ownership snapshots from the authoritative device row.
DROP TRIGGER IF EXISTS `trg_ed_check_device_type_bi`;
DROP TRIGGER IF EXISTS `trg_wq_check_device_type_bi`;
DROP TRIGGER IF EXISTS `trg_ld_check_device_type_bi`;
DROP TRIGGER IF EXISTS `trg_pd_check_device_type_bi`;

DELIMITER $$
CREATE TRIGGER `trg_ed_check_device_type_bi`
BEFORE INSERT ON `environment_data`
FOR EACH ROW
BEGIN
    DECLARE v_type_code VARCHAR(50) DEFAULT NULL;
    DECLARE v_user_id BIGINT DEFAULT NULL;
    DECLARE v_plot_id BIGINT DEFAULT NULL;
    SELECT dt.type_code, d.user_id, d.plot_id
      INTO v_type_code, v_user_id, v_plot_id
      FROM `iot_device` d JOIN `device_type` dt ON dt.id = d.type_id
     WHERE d.id = NEW.device_id LIMIT 1;
    IF v_type_code IS NULL OR v_type_code <> 'ENV_SENSOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'environment_data device must be ENV_SENSOR';
    END IF;
    SET NEW.owner_user_id = v_user_id;
    SET NEW.plot_id_at_collection = v_plot_id;
END$$

CREATE TRIGGER `trg_wq_check_device_type_bi`
BEFORE INSERT ON `water_quality_data`
FOR EACH ROW
BEGIN
    DECLARE v_type_code VARCHAR(50) DEFAULT NULL;
    DECLARE v_user_id BIGINT DEFAULT NULL;
    DECLARE v_plot_id BIGINT DEFAULT NULL;
    SELECT dt.type_code, d.user_id, d.plot_id
      INTO v_type_code, v_user_id, v_plot_id
      FROM `iot_device` d JOIN `device_type` dt ON dt.id = d.type_id
     WHERE d.id = NEW.device_id LIMIT 1;
    IF v_type_code IS NULL OR v_type_code <> 'WATER_QUALITY' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'water_quality_data device must be WATER_QUALITY';
    END IF;
    SET NEW.owner_user_id = v_user_id;
    SET NEW.plot_id_at_collection = v_plot_id;
END$$

CREATE TRIGGER `trg_ld_check_device_type_bi`
BEFORE INSERT ON `light_data`
FOR EACH ROW
BEGIN
    DECLARE v_type_code VARCHAR(50) DEFAULT NULL;
    DECLARE v_user_id BIGINT DEFAULT NULL;
    DECLARE v_plot_id BIGINT DEFAULT NULL;
    SELECT dt.type_code, d.user_id, d.plot_id
      INTO v_type_code, v_user_id, v_plot_id
      FROM `iot_device` d JOIN `device_type` dt ON dt.id = d.type_id
     WHERE d.id = NEW.device_id LIMIT 1;
    IF v_type_code IS NULL OR v_type_code <> 'GROW_LIGHT' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'light_data device must be GROW_LIGHT';
    END IF;
    SET NEW.owner_user_id = v_user_id;
    SET NEW.plot_id_at_collection = v_plot_id;
END$$

CREATE TRIGGER `trg_pd_check_device_type_bi`
BEFORE INSERT ON `pump_data`
FOR EACH ROW
BEGIN
    DECLARE v_type_code VARCHAR(50) DEFAULT NULL;
    DECLARE v_user_id BIGINT DEFAULT NULL;
    DECLARE v_plot_id BIGINT DEFAULT NULL;
    SELECT dt.type_code, d.user_id, d.plot_id
      INTO v_type_code, v_user_id, v_plot_id
      FROM `iot_device` d JOIN `device_type` dt ON dt.id = d.type_id
     WHERE d.id = NEW.device_id LIMIT 1;
    IF v_type_code IS NULL OR v_type_code <> 'WATER_PUMP' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'pump_data device must be WATER_PUMP';
    END IF;
    SET NEW.owner_user_id = v_user_id;
    SET NEW.plot_id_at_collection = v_plot_id;
END$$
DELIMITER ;
