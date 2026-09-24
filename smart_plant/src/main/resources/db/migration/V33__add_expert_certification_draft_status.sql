-- 0 表示专家账号已注册但尚未主动提交认证材料；只有 1 才进入后台审核队列。
ALTER TABLE `expert_profile`
    DROP CHECK `chk_expert_audit_status`;

ALTER TABLE `expert_profile`
    MODIFY COLUMN `audit_status` tinyint DEFAULT 0 COMMENT '审核状态（0未提交 1待审核 2审核通过 3审核拒绝）',
    ADD CONSTRAINT `chk_expert_audit_status` CHECK (`audit_status` IN (0, 1, 2, 3));
