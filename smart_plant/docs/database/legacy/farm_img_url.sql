/*
 Navicat migration script: add farm image field.

 Usage:
 1. Select the smart_plant database in Navicat.
 2. Execute this script if the farm table already exists and lacks img_url.
 3. The Spring Boot startup initializer also performs the same check automatically.
*/

ALTER TABLE `farm`
  ADD COLUMN `img_url` varchar(500) DEFAULT NULL COMMENT '农场图片地址' AFTER `farm_code`;
