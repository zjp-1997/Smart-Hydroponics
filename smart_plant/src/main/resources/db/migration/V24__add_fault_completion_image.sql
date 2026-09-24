-- 保存 farm 端故障处理的现场图片凭证，原有故障单允许为空。
ALTER TABLE `iot_device_fault`
    ADD COLUMN `completion_image_url` VARCHAR(500) DEFAULT NULL COMMENT '故障完成现场图片' AFTER `handle_result`;
