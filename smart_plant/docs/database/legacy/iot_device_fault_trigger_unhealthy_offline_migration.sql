-- Align iot_device_fault triggers with the rule:
-- any open device fault keeps the device health=fault, control=closed and online=offline.

DROP TRIGGER IF EXISTS `trg_iot_device_fault_ai`;
DROP TRIGGER IF EXISTS `trg_iot_device_fault_au`;
DROP TRIGGER IF EXISTS `trg_iot_device_fault_ad`;

DELIMITER ;;

CREATE TRIGGER `trg_iot_device_fault_ai`
AFTER INSERT ON `iot_device_fault`
FOR EACH ROW
BEGIN
  UPDATE `iot_device` d
  SET d.health_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN 1
        WHEN d.health_status = 1 THEN 0
        ELSE d.health_status
      END,
      d.control_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.control_status
      END,
      d.online_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.online_status
      END,
      d.offline_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN COALESCE(d.offline_time, NOW())
        ELSE d.offline_time
      END,
      d.fault_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN COALESCE(d.fault_time, NOW())
        ELSE d.fault_time
      END
  WHERE d.id = NEW.device_id;
END;;

CREATE TRIGGER `trg_iot_device_fault_au`
AFTER UPDATE ON `iot_device_fault`
FOR EACH ROW
BEGIN
  UPDATE `iot_device` d
  SET d.health_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN 1
        WHEN d.health_status = 1 THEN 0
        ELSE d.health_status
      END,
      d.control_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.control_status
      END,
      d.online_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.online_status
      END,
      d.offline_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN COALESCE(d.offline_time, NOW())
        ELSE d.offline_time
      END,
      d.fault_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN COALESCE(d.fault_time, NOW())
        ELSE NULL
      END
  WHERE d.id = OLD.device_id;

  UPDATE `iot_device` d
  SET d.health_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN 1
        WHEN d.health_status = 1 THEN 0
        ELSE d.health_status
      END,
      d.control_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.control_status
      END,
      d.online_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.online_status
      END,
      d.offline_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN COALESCE(d.offline_time, NOW())
        ELSE d.offline_time
      END,
      d.fault_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = NEW.device_id AND f.status IN (0, 1)) THEN COALESCE(d.fault_time, NOW())
        ELSE NULL
      END
  WHERE d.id = NEW.device_id;
END;;

CREATE TRIGGER `trg_iot_device_fault_ad`
AFTER DELETE ON `iot_device_fault`
FOR EACH ROW
BEGIN
  UPDATE `iot_device` d
  SET d.health_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN 1
        WHEN d.health_status = 1 THEN 0
        ELSE d.health_status
      END,
      d.control_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.control_status
      END,
      d.online_status = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN 0
        ELSE d.online_status
      END,
      d.offline_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN COALESCE(d.offline_time, NOW())
        ELSE d.offline_time
      END,
      d.fault_time = CASE
        WHEN EXISTS (SELECT 1 FROM `iot_device_fault` f WHERE f.device_id = OLD.device_id AND f.status IN (0, 1)) THEN COALESCE(d.fault_time, NOW())
        ELSE NULL
      END
  WHERE d.id = OLD.device_id;
END;;

DELIMITER ;
