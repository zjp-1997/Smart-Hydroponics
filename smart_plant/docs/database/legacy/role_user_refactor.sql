SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_name` (`role_name`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

INSERT INTO `role` (`id`, `role_name`, `role_code`, `remark`) VALUES
(1, 'Farm Owner', 'farm_owner', 'Farm owner role'),
(2, 'Administrator', 'admin', 'System administrator role'),
(3, 'Technician', 'technician', 'Technician role'),
(4, 'Expert', 'expert', 'Expert role'),
(5, 'User', 'user', 'Normal user role')
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `remark` = VALUES(`remark`);

ALTER TABLE `user` DROP CHECK `chk_user_role_type`;

ALTER TABLE `user` DROP CHECK `chk_user_role`;

ALTER TABLE `user` DROP CHECK `chk_user_type`;

ALTER TABLE `user`
  ADD COLUMN `role_id` bigint NULL COMMENT '角色ID' AFTER `gender`;

UPDATE `user` u
LEFT JOIN `role` r ON r.role_code = u.`role`
SET u.role_id = COALESCE(r.id, (SELECT id FROM `role` WHERE role_code = 'user' LIMIT 1));

ALTER TABLE `user`
  MODIFY COLUMN `role_id` bigint NOT NULL DEFAULT 5 COMMENT '角色ID',
  ADD INDEX `idx_user_role_id` (`role_id`);

ALTER TABLE `user`
  ADD CONSTRAINT `fk_user_role`
  FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
  ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `user`
  DROP COLUMN `role`,
  DROP COLUMN `user_type`;
