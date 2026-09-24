-- 普通用户与农场主的归属关系；技术员继续使用已有的 farm_owner_technician 表。
CREATE TABLE `farm_owner_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '绑定ID',
    `owner_user_id` BIGINT NOT NULL COMMENT '农场主用户ID',
    `user_id` BIGINT NOT NULL COMMENT '普通用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_farm_owner_user_user` (`user_id`),
    KEY `idx_farm_owner_user_owner` (`owner_user_id`),
    CONSTRAINT `fk_farm_owner_user_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_farm_owner_user_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='普通用户所属农场主';
