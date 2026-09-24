# Smart Plant 数据库版本管理

## 决策

- 用户模型是单用户单角色，`user.role_id` 是唯一角色事实源。
- `user_role` 已由 `V2__consolidate_single_user_role.sql` 删除，不允许重新创建或双写。
- 所有结构和参考数据变更必须进入 `src/main/resources/db/migration`，禁止在 Java 启动代码中执行 DDL/DML。
- 传感器演示数据不再随应用启动生成；需要演示数据时应使用独立、显式执行且仅面向开发环境的数据脚本。

## 迁移版本

| 版本 | 用途 |
| --- | --- |
| V1 | 当前 55 张历史表的纯结构基线；全新数据库执行，已有非空数据库登记为基线而不重复建表 |
| V2 | 删除冗余 `user_role`，确立 `user.role_id` 为唯一角色来源 |
| V3 | 初始化 5 个内置角色、60 个权限及其角色权限/数据范围关系 |
| V4 | 吊销历史脚本中遗留的固定管理员凭据，仅影响尚未修改旧密码的账号 |
| V5 | 强化坐标、通知业务引用和 IoT 采集数据的领域完整性约束 |
| V6–V12 | 系统设置、错误日志、设备计划、农事任务生命周期及专家状态修复 |
| V13–V21 | 菜单权限、地图、专家咨询、种植批次与病虫害知识库演进 |
| V22–V30 | 仓库/故障历史保留、农场归属、AI 文件名与系统公告唯一投递 |
| V31–V36 | 农场成员聊天、地块边界、专家认证、入场审批与附件索引 |
| V37 | 库存行锁与请求幂等键，防止并发出库超卖或重复记账 |
| V38 | 修复已解决故障对应设备的在线状态 |
| V39 | 固化新遥测数据的采集时归属，增加协议消息去重，并为告警/通知增加并发幂等唯一键 |
| V40 | 让数据库直写的 IoT 遥测同样自动保存采集时归属快照 |

## 发布流程

1. 发布前备份数据库，并在同版本的预发布副本上执行迁移。
2. 检查 `flyway_schema_history` 不存在失败记录，且校验通过。
3. 先执行单实例迁移作业，再滚动启动应用实例；不要让多个实例承担建库职责。
4. 迁移文件发布后不可修改；下一次变更必须新增更高版本脚本。
5. 禁止在生产环境启用 Flyway `clean`，本项目已配置 `clean-disabled: true`。

## 账号最小权限

- 应用运行账号只授予业务表的 `SELECT/INSERT/UPDATE/DELETE`，不得授予 `ALTER/CREATE/DROP/TRIGGER`。
- 迁移账号仅由发布流水线持有，负责 Flyway 元数据、DDL 和触发器变更；应用实例不得使用该账号。
- 账号名、主机范围和口令由部署环境决定，仓库不保存可直接执行的生产 `GRANT` 或真实凭据。

## 遥测历史与留存

- V39 只为新写入数据自动保存 `owner_user_id`、`plot_id_at_collection`；历史行保持 `NULL`，查询时兼容回退到设备当前归属，避免无证据地伪造历史归属。
- 若现场能提供设备调拨记录，应另建一次性、可审计的回填迁移；没有可靠证据时不得按当前归属批量回填。
- 遥测留存期限、归档介质和删除审批属于业务合规决策。确定规则后再新增分区/归档迁移，不在本次变更中猜测期限或直接删除历史数据。

生产应用实例保持 `FLYWAY_ENABLED=false`（默认值）。发布流水线使用独立且固定版本的 Flyway CLI/容器执行迁移，迁移文件位置指向本项目的 `src/main/resources/db/migration`，成功并核对版本后再启动应用；开发和测试环境可在本地 `.env` 中启用应用内迁移。MySQL 8.4 的生产迁移作业应使用已经过预发布验证的当前 Flyway 版本，不能绕过 Spring Boot 依赖管理强行替换应用内嵌版本。这样既避免多实例争抢元数据锁，也隔离迁移工具和应用运行时的依赖生命周期。

## 现有库接入说明

`baseline-version: 1` 用于接入当前历史库，`baseline-on-migrate` 默认关闭。全新空库无需额外配置，会依次执行全部版本迁移；现有非空库首次升级时，发布人员必须显式设置 `FLYWAY_BASELINE_ON_MIGRATE=true`，使当前结构登记为 V1 后执行 V2 及后续版本。迁移完成即移除该环境变量，避免错误数据库被静默接管。

## 校验 SQL

```sql
SELECT installed_rank, version, description, type, success
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT COUNT(*)
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'user_role';

SELECT COUNT(*)
FROM `user` u
LEFT JOIN role r ON r.id = u.role_id
WHERE r.id IS NULL;
```

后两项必须均为 `0`。
