-- 地图总览原先复用 plot:manage，无法在菜单管理和角色授权中独立配置。
INSERT IGNORE INTO `permission`
    (parent_id, permission_name, permission_code, type, path, component, status, sort)
VALUES
    (0, '地图总览', 'map:view', 1, '/map/overview', 'map/MapOverview', 1, 105);

-- 已拥有地块管理权限的角色继续可访问地图，避免升级后权限缩水。
INSERT IGNORE INTO `role_permission` (role_id, permission_id)
SELECT rp.role_id, map_permission.id
FROM `role_permission` rp
JOIN `permission` plot_permission
  ON plot_permission.id = rp.permission_id
 AND plot_permission.permission_code = 'plot:manage'
JOIN `permission` map_permission
  ON map_permission.permission_code = 'map:view';
