ALTER TABLE `planting_batch`
    ADD COLUMN `harvester` varchar(50) DEFAULT NULL COMMENT '实际采收人' AFTER `actual_harvest_at`;
