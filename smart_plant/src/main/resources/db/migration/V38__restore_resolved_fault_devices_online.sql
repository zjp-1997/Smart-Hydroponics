-- 修复旧版本完成故障后仅恢复健康状态、仍保留离线状态的历史数据。
-- 仅处理离线时间不晚于最后一次故障完成时间的设备，避免覆盖故障完成后发生的真实掉线。
UPDATE `iot_device` d
JOIN (
    SELECT f.device_id,
           MAX(COALESCE(f.end_time, f.handle_time, f.update_time)) AS last_resolved_time
    FROM `iot_device_fault` f
    WHERE f.is_deleted = 0
      AND f.status IN (2, 3)
    GROUP BY f.device_id
) resolved ON resolved.device_id = d.id
SET d.health_status = 0,
    d.online_status = 1,
    d.offline_time = NULL,
    d.fault_time = NULL,
    d.last_online_time = NOW()
WHERE d.health_status = 0
  AND d.online_status = 0
  AND d.offline_time IS NOT NULL
  AND d.offline_time <= resolved.last_resolved_time
  AND NOT EXISTS (
      SELECT 1
      FROM `iot_device_fault` active_fault
      WHERE active_fault.device_id = d.id
        AND active_fault.is_deleted = 0
        AND active_fault.status IN (0, 1)
  );
