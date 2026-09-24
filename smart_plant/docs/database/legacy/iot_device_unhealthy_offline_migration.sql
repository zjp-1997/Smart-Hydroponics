-- Keep IoT device runtime status consistent when health status is fault or maintenance.
-- Unhealthy devices must be offline and control-closed.

UPDATE `iot_device`
SET `control_status` = 0,
    `online_status` = 0,
    `offline_time` = COALESCE(`offline_time`, NOW()),
    `fault_time` = COALESCE(`fault_time`, NOW())
WHERE `health_status` IN (1, 2);

ALTER TABLE `iot_device`
  DROP CHECK `chk_iot_device_unhealthy_control_off`;

ALTER TABLE `iot_device`
  ADD CONSTRAINT `chk_iot_device_unhealthy_offline_closed`
  CHECK (`health_status` NOT IN (1, 2) OR (`control_status` = 0 AND `online_status` = 0));
