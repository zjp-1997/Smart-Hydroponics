-- 截至时间已经替代计划起止区间，数据库层同步设为必填，避免绕过服务层产生不可归类任务。
-- 极少数历史空值无法还原原计划时间时，以任务创建时间兜底，并明确保留原始创建语义。
UPDATE `farm_task`
SET `deadline_time` = COALESCE(`deadline_time`, `create_time`, NOW())
WHERE `deadline_time` IS NULL;

ALTER TABLE `farm_task`
  MODIFY COLUMN `deadline_time` datetime NOT NULL COMMENT '任务截至时间';
