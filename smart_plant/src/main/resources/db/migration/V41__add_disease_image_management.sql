-- 用户从 farm 病虫害详情页上传的手机图片与知识库条目建立可管理关联。
ALTER TABLE `crop_image`
    ADD COLUMN `disease_pest_id` bigint DEFAULT NULL COMMENT '关联病虫害ID' AFTER `user_id`,
    ADD KEY `idx_crop_image_disease_pest_id` (`disease_pest_id`),
    ADD CONSTRAINT `fk_crop_image_disease_pest`
        FOREIGN KEY (`disease_pest_id`) REFERENCES `disease_pest` (`id`)
        ON DELETE CASCADE ON UPDATE RESTRICT;

INSERT IGNORE INTO `permission`
    (`parent_id`, `permission_name`, `permission_code`, `type`, `path`, `component`, `status`, `sort`)
SELECT id, '病害图片管理', 'disease_image:manage', 1,
       '/disease-image/list', 'phonePicture/ListView', 1, 83
FROM `permission`
WHERE `permission_code` = 'menu_group:disease';

-- 已拥有病虫害或手机图片管理权限的角色自动获得新菜单权限，升级后即可使用。
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
SELECT DISTINCT source_role.role_id, target.id
FROM `role_permission` source_role
JOIN `permission` source_permission ON source_permission.id = source_role.permission_id
JOIN `permission` target ON target.permission_code = 'disease_image:manage'
WHERE source_permission.permission_code IN ('disease_pest:manage', 'crop_image:manage');
