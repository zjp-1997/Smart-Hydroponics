-- Migrate disease_pest from crop-level association to crop-type-level association.
-- Existing disease_pest.crop_id values are converted through crop.type_id.

ALTER TABLE `disease_pest`
  ADD COLUMN `crop_type_id` bigint NULL COMMENT '关联作物类型ID' AFTER `id`;

UPDATE `disease_pest` dp
LEFT JOIN `crop` c ON c.id = dp.crop_id
SET dp.crop_type_id = c.type_id
WHERE dp.crop_id IS NOT NULL;

ALTER TABLE `disease_pest`
  DROP FOREIGN KEY `fk_disease_pest_crop`,
  DROP INDEX `idx_disease_pest_crop_id`,
  ADD INDEX `idx_disease_pest_crop_type_id` (`crop_type_id` ASC),
  ADD CONSTRAINT `fk_disease_pest_crop_type`
    FOREIGN KEY (`crop_type_id`) REFERENCES `crop_type` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  DROP COLUMN `crop_id`;
