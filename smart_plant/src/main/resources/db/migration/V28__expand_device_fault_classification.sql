-- 管理端提供六种故障类型和四档严重程度，数据库约束必须与接口枚举保持一致。
ALTER TABLE `iot_device_fault`
    DROP CHECK `chk_fault_type`,
    DROP CHECK `chk_fault_severity`,
    ADD CONSTRAINT `chk_fault_type` CHECK (`fault_type` IN (1, 2, 3, 4, 5, 6)),
    ADD CONSTRAINT `chk_fault_severity` CHECK (`severity` IN (1, 2, 3, 4));

-- 同步字段说明，便于运维人员直接查看表结构时理解枚举含义。
ALTER TABLE `iot_device_fault`
    MODIFY COLUMN `fault_type` TINYINT NOT NULL
        COMMENT '故障类型 1通信 2传感器 3电源 4执行器 5数据异常 6其他',
    MODIFY COLUMN `severity` TINYINT NOT NULL DEFAULT 1
        COMMENT '严重程度 1低 2中 3高 4严重';
