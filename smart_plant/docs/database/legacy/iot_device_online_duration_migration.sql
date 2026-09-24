-- Add cumulative online duration for IoT devices.
-- Unit: seconds. Current online sessions are calculated dynamically from last_online_time.

ALTER TABLE `iot_device`
  ADD COLUMN `online_duration` bigint NOT NULL DEFAULT 0 COMMENT '累计在线时长，单位秒' AFTER `last_online_time`;
