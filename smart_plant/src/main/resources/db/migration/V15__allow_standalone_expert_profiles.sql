ALTER TABLE `expert_profile`
    MODIFY COLUMN `user_id` bigint NULL COMMENT '关联用户ID（可选）',
    ADD COLUMN `organization` varchar(100) NULL COMMENT '所属机构名称' AFTER `institution_id`;

UPDATE `expert_profile` p
LEFT JOIN `institution` i ON i.id = p.institution_id
SET p.organization = i.institution_name
WHERE p.organization IS NULL
  AND i.institution_name IS NOT NULL;

ALTER TABLE `expert_certificate`
    ADD COLUMN `certificate_name` varchar(100) NULL COMMENT '证书名称' AFTER `expert_id`,
    ADD COLUMN `audit_status` tinyint DEFAULT 1 COMMENT '审核状态（1待审核 2审核通过 3审核拒绝）' AFTER `certificate_url`,
    ADD CONSTRAINT `chk_expert_certificate_audit_status` CHECK (`audit_status` IN (1, 2, 3));
