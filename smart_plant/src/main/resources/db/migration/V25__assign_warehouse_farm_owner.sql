-- 独立记录物资所属农场主；原 user_id 保留创建账号，历史管理员物资先不猜测归属。
ALTER TABLE `warehouse_item`
    ADD COLUMN `farm_owner_id` BIGINT DEFAULT NULL COMMENT '所属农场主用户ID，历史未分配物资为空' AFTER `user_id`,
    ADD KEY `idx_warehouse_item_farm_owner_id` (`farm_owner_id`),
    ADD CONSTRAINT `fk_warehouse_item_farm_owner` FOREIGN KEY (`farm_owner_id`) REFERENCES `user` (`id`);

-- 历史上由农场主本人创建的物资可安全恢复归属；管理员创建的物资仍待人工指定。
UPDATE `warehouse_item` wi
JOIN `user` u ON u.id = wi.user_id
JOIN `role` r ON r.id = u.role_id AND LOWER(r.role_code) = 'farm_owner'
SET wi.farm_owner_id = wi.user_id;

-- 编码唯一性由创建账号转为农场主范围；NULL 归属的历史行不参与新约束。
ALTER TABLE `warehouse_item`
    DROP INDEX `uk_warehouse_item_user_code`,
    ADD UNIQUE KEY `uk_warehouse_item_owner_code` (`farm_owner_id`, `item_code`);
