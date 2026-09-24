-- 增加病虫害综合识别类型，供 farm 病虫害防治页跳转智能策略时自动选中。
INSERT INTO `ai_recognition_type` (`type_code`, `type_name`, `description`, `status`)
VALUES ('DISEASE_PEST_RECOGNITION', '病虫害识别', '综合识别作物病害、虫害及其典型症状', 1)
ON DUPLICATE KEY UPDATE
    `type_name` = '病虫害识别',
    `description` = '综合识别作物病害、虫害及其典型症状',
    `status` = 1;
