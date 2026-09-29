-- 创建、编辑和取消事件与执行事件共用 farm_task_record。
-- 系统自动产生的任务没有真实操作人，因此 operator_id 允许为空，名称保存在快照字段。
ALTER TABLE `farm_task_record`
    MODIFY COLUMN `operator_id` bigint DEFAULT NULL COMMENT '操作人ID，系统自动事件为空',
    MODIFY COLUMN `action_type` tinyint NOT NULL
        COMMENT '动作：1开始 2完成 3反馈 4优化 5创建 6编辑 7取消',
    ADD CONSTRAINT `chk_farm_task_record_action_type`
        CHECK (`action_type` IN (1, 2, 3, 4, 5, 6, 7));

-- 为旧任务补一条创建事件，确保时间线从任务起点开始。
INSERT INTO `farm_task_record`
    (`task_id`, `operator_id`, `operator_name_snapshot`, `action_type`, `action_content`,
     `source_client`, `before_status`, `after_status`, `execute_time`, `create_time`)
SELECT ft.id, NULL, '系统迁移', 5, '农事任务已创建',
       'SYSTEM', NULL, 1, ft.create_time, ft.create_time
FROM `farm_task` ft
WHERE NOT EXISTS (
    SELECT 1
    FROM `farm_task_record` r
    WHERE r.task_id = ft.id
      AND r.action_type = 5
);
