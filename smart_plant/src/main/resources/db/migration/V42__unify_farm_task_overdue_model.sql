-- 逾期是基于截止时间的派生属性，不再作为农事任务的持久化状态。
-- 历史 status=4 数据根据是否已开始恢复为未开始或进行中。
UPDATE `farm_task`
SET `status` = CASE WHEN `actual_start_time` IS NULL THEN 1 ELSE 2 END
WHERE `status` = 4;

ALTER TABLE `farm_task`
    DROP CHECK `chk_task_status`,
    MODIFY COLUMN `status` tinyint DEFAULT '1'
        COMMENT '任务状态（1未开始 2进行中 3已完成 5已取消；逾期由截止时间派生）',
    ADD CONSTRAINT `chk_task_status` CHECK (`status` IN (1, 2, 3, 5));
