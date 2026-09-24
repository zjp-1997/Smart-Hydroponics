-- 扩展病虫害知识库，使后台能够维护发生规律、多图详情和结构化防治信息。
ALTER TABLE `disease_pest`
    ADD COLUMN `affected_crops` varchar(255) DEFAULT NULL COMMENT '危害作物范围（展示文本）' AFTER `crop_type_id`,
    ADD COLUMN `occurrence_period` varchar(100) DEFAULT NULL COMMENT '主要发生时期或季节' AFTER `suitable_stage`,
    ADD COLUMN `living_habits` text COMMENT '生活习性' AFTER `occurrence_period`,
    ADD COLUMN `suitable_environment` text COMMENT '适宜发生的环境条件' AFTER `living_habits`,
    ADD COLUMN `transmission_route` text COMMENT '传播或扩散途径' AFTER `suitable_environment`,
    ADD COLUMN `image_urls` json DEFAULT NULL COMMENT '详情图片地址JSON数组' AFTER `cover_image`,
    ADD COLUMN `sort_order` int NOT NULL DEFAULT 0 COMMENT '展示顺序，数值越小越靠前' AFTER `status`,
    ADD KEY `idx_disease_pest_sort_order` (`sort_order`);

-- control_type 保留“预防/治疗”维度，control_category 新增“农业/物理/化学”手段维度。
ALTER TABLE `disease_control`
    ADD COLUMN `control_category` tinyint DEFAULT NULL COMMENT '防治手段（1农业 2物理 3化学）' AFTER `control_type`,
    ADD COLUMN `dosage_spec` varchar(255) DEFAULT NULL COMMENT '剂型、浓度或稀释倍数' AFTER `usage_method`,
    ADD COLUMN `safety_interval_days` smallint unsigned DEFAULT NULL COMMENT '安全间隔期（天）' AFTER `dosage_spec`,
    ADD COLUMN `precautions` varchar(500) DEFAULT NULL COMMENT '禁限用与施药注意事项' AFTER `safety_interval_days`,
    ADD COLUMN `sort_order` int NOT NULL DEFAULT 0 COMMENT '展示顺序，数值越小越靠前' AFTER `status`,
    ADD KEY `idx_disease_control_category` (`control_category`),
    ADD KEY `idx_disease_control_sort_order` (`disease_id`, `sort_order`),
    ADD CONSTRAINT `chk_disease_control_category`
        CHECK (`control_category` IS NULL OR `control_category` IN (1, 2, 3));
