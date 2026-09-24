-- 病虫害知识库描述稳定的基础属性；危害等级属于具体识别事件，因此从主数据表移除。
ALTER TABLE `disease_pest`
    DROP CHECK `chk_disease_pest_severity`,
    DROP COLUMN `severity_level`;
