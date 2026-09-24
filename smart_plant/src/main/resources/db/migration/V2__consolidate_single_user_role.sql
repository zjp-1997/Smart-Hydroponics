-- 用户领域采用“单用户单角色”模型，`user.role_id` 是唯一角色事实源。
-- 历史 `user_role` 仅由启动初始化器从 `user.role_id` 复制，业务查询与授权均不读取它。
-- 迁移前已确认所有 user.role_id 均存在对应关系；额外关系属于无效影子数据。
DROP TABLE IF EXISTS `user_role`;
