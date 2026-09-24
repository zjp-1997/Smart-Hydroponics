-- 保存地块真实边界的 WGS-84 坐标点；历史地块保持 NULL，并继续以中心点展示。
ALTER TABLE `plot`
    ADD COLUMN `boundary_points` JSON NULL COMMENT '地块边界点数组（WGS-84，经度/纬度）' AFTER `latitude`;
