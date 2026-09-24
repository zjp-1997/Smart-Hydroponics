-- 同一系统公告对同一用户只能存在一条投递，避免列表与统计并发同步时产生重复未读消息。
ALTER TABLE `notification`
    ADD COLUMN `system_announcement_delivery_key` varchar(64)
        GENERATED ALWAYS AS (
            CASE
                WHEN `notice_type` = 1
                     AND `ref_type` = 'system_announcement'
                     AND `ref_id` IS NOT NULL
                THEN CONCAT(`user_id`, ':', `ref_id`)
                ELSE NULL
            END
        ) STORED COMMENT '系统公告接收明细唯一键',
    ADD UNIQUE KEY `uk_notification_system_announcement_delivery` (`system_announcement_delivery_key`);
