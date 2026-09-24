-- 独立专家没有关联用户，消息通过 consult_session.expert_id 归属专家。
ALTER TABLE `consult_message`
    MODIFY COLUMN `receiver_id` BIGINT NULL COMMENT '接收人用户ID，独立专家可为空';
