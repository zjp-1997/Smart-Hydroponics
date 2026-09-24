-- 兼容升级前“注册即待审核”的历史账号：缺少核心材料时回退为草稿。
UPDATE `expert_profile` p
LEFT JOIN (
    SELECT `expert_id`, MAX(NULLIF(TRIM(`certificate_url`), '')) AS `certificate_url`
    FROM `expert_certificate`
    GROUP BY `expert_id`
) c ON c.`expert_id` = p.`id`
SET p.`audit_status` = 0,
    p.`status` = 0,
    p.`service_status` = 0,
    p.`consultation_status` = 0
WHERE p.`audit_status` = 1
  AND (
      NULLIF(TRIM(p.`real_name`), '') IS NULL
      OR NULLIF(TRIM(p.`organization`), '') IS NULL
      OR NULLIF(TRIM(p.`job_title`), '') IS NULL
      OR c.`certificate_url` IS NULL
  );
