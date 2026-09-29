-- 审计记录必须随任务永久保留，禁止级联删除任务执行时间线。
ALTER TABLE `farm_task_record`
    DROP FOREIGN KEY `fk_farm_task_record_task`;

ALTER TABLE `farm_task_record`
    ADD CONSTRAINT `fk_farm_task_record_task`
        FOREIGN KEY (`task_id`) REFERENCES `farm_task` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
