-- 补齐与 smart_farm LeftMenu.vue 一致的一级菜单分组。
INSERT IGNORE INTO `permission` (parent_id, permission_name, permission_code, type, path, component, status, sort)
VALUES
    (0, '系统设置', 'menu_group:system', 1, NULL, NULL, 1, 20),
    (0, '用户管理', 'menu_group:user', 1, NULL, NULL, 1, 30),
    (0, '专家管理', 'menu_group:expert', 1, NULL, NULL, 1, 40),
    (0, '农场管理', 'menu_group:farm', 1, NULL, NULL, 1, 50),
    (0, '地块管理', 'menu_group:plot', 1, NULL, NULL, 1, 60),
    (0, '作物管理', 'menu_group:crop', 1, NULL, NULL, 1, 70),
    (0, '病虫害知识库', 'menu_group:disease', 1, NULL, NULL, 1, 80),
    (0, '设备管理', 'menu_group:device', 1, NULL, NULL, 1, 90),
    (0, '环境监测管理', 'menu_group:environment', 1, NULL, NULL, 1, 100),
    (0, '图片管理', 'menu_group:image', 1, NULL, NULL, 1, 110),
    (0, 'AI 识别管理', 'menu_group:ai_recognition', 1, NULL, NULL, 1, 120),
    (0, '模型信息管理', 'menu_group:model', 1, NULL, NULL, 1, 130),
    (0, '日志管理', 'menu_group:log', 1, NULL, NULL, 1, 150);

-- 原先复用同一权限码的二级菜单拆成独立权限，才能分别授权。
INSERT IGNORE INTO `permission` (parent_id, permission_name, permission_code, type, path, component, status, sort)
SELECT parent.id, child.permission_name, child.permission_code, 1, child.path, child.component, 1, child.sort
FROM `permission` parent
JOIN (
    SELECT '短信验证码管理' permission_name, 'user_sms:manage' permission_code, NULL path, NULL component, 23 sort
    UNION ALL SELECT '水质数据管理', 'water_quality_data:manage', '/water-quality-data/list', 'deviceData/ListView', 102
    UNION ALL SELECT '补光灯数据管理', 'light_data:manage', '/light-data/list', 'deviceData/ListView', 103
    UNION ALL SELECT '水泵数据管理', 'pump_data:manage', '/pump-data/list', 'deviceData/ListView', 104
) child
WHERE parent.permission_code = CASE
    WHEN child.permission_code = 'user_sms:manage' THEN 'menu_group:user'
    ELSE 'menu_group:environment'
END;

UPDATE `permission` child
JOIN `permission` parent ON parent.permission_code = CASE
    WHEN child.permission_code IN ('role:manage', 'permission:manage', 'system_config:manage') THEN 'menu_group:system'
    WHEN child.permission_code IN ('user:manage', 'user_oauth:manage', 'user_sms:manage') THEN 'menu_group:user'
    WHEN child.permission_code IN ('expert:manage', 'expert:audit', 'expert_review:manage') THEN 'menu_group:expert'
    WHEN child.permission_code = 'farm:manage' THEN 'menu_group:farm'
    WHEN child.permission_code IN ('plot:manage', 'planting_batch:manage') THEN 'menu_group:plot'
    WHEN child.permission_code IN ('crop_type:manage', 'growth_stage:manage', 'crop:manage') THEN 'menu_group:crop'
    WHEN child.permission_code IN ('disease_pest:manage', 'disease_control:manage') THEN 'menu_group:disease'
    WHEN child.permission_code IN ('device_type:manage', 'iot_device:manage', 'iot_device_fault:manage', 'iot_device_plan:manage') THEN 'menu_group:device'
    WHEN child.permission_code IN ('sensor_data:manage', 'water_quality_data:manage', 'light_data:manage', 'pump_data:manage') THEN 'menu_group:environment'
    WHEN child.permission_code IN ('crop_image:manage', 'camera_image:manage') THEN 'menu_group:image'
    WHEN child.permission_code IN ('ai_recognition_type:manage', 'ai_recognition_record:manage') THEN 'menu_group:ai_recognition'
    WHEN child.permission_code IN ('model:manage', 'ai_chat:manage') THEN 'menu_group:model'
    WHEN child.permission_code IN ('operation_log:view', 'error_log:manage', 'login_log:view') THEN 'menu_group:log'
END
SET child.parent_id = parent.id
WHERE child.permission_code IN (
    'role:manage', 'permission:manage', 'system_config:manage',
    'user:manage', 'user_oauth:manage', 'user_sms:manage',
    'expert:manage', 'expert:audit', 'expert_review:manage', 'farm:manage',
    'plot:manage', 'planting_batch:manage', 'crop_type:manage', 'growth_stage:manage', 'crop:manage',
    'disease_pest:manage', 'disease_control:manage', 'device_type:manage', 'iot_device:manage',
    'iot_device_fault:manage', 'iot_device_plan:manage', 'sensor_data:manage', 'water_quality_data:manage',
    'light_data:manage', 'pump_data:manage', 'crop_image:manage', 'camera_image:manage',
    'ai_recognition_type:manage', 'ai_recognition_record:manage', 'model:manage', 'ai_chat:manage',
    'operation_log:view', 'error_log:manage', 'login_log:view'
);

UPDATE `permission` SET permission_name = '环境数据管理' WHERE permission_code = 'sensor_data:manage';

-- 已拥有原共享权限的角色继续拥有拆分后的二级菜单，避免升级后权限缩水。
INSERT IGNORE INTO `role_permission` (role_id, permission_id)
SELECT rp.role_id, child.id
FROM `role_permission` rp
JOIN `permission` source ON source.id = rp.permission_id
JOIN `permission` child ON child.permission_code IN (
    CASE WHEN source.permission_code = 'user:manage' THEN 'user_sms:manage' ELSE 'water_quality_data:manage' END,
    CASE WHEN source.permission_code = 'sensor_data:manage' THEN 'light_data:manage' ELSE 'user_sms:manage' END,
    CASE WHEN source.permission_code = 'sensor_data:manage' THEN 'pump_data:manage' ELSE 'user_sms:manage' END
)
WHERE source.permission_code IN ('user:manage', 'sensor_data:manage');
