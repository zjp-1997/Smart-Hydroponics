-- 修复历史专家数据中“咨询已启用但专家档案仍禁用”的矛盾状态。
-- 仅恢复审核通过、咨询已启用且关联专家用户正常的档案，不改变真正暂停咨询或禁用用户的数据。
UPDATE `expert_profile` p
INNER JOIN `user` u ON u.id = p.user_id
INNER JOIN `role` r ON r.id = u.role_id
SET p.status = 1
WHERE p.status = 0
  AND p.audit_status = 2
  AND p.service_status = 1
  AND p.consultation_status = 1
  AND u.status = 1
  AND LOWER(r.role_code) = 'expert';
