# 维护消息管理交付说明

## 页面与正文

smart_farm 菜单：消息通知管理 → 维护消息管理，路由 `/maintenance-msg/list`。
统计与农事消息一致：消息总数、今日发布、未读送达、已读送达。
查询条件：消息标题、消息内容、地块名称。
工具栏提供批量删除、发布消息；表格提供复选框、编辑和删除。
发布表单选择设备并填写标题、正文和级别；自动消息、人工消息均允许修改标题、正文和级别，关联设备保持不变。
自动生成及新建表单的默认正文如下。默认文案可以自由改写，发布和编辑请求会提交并保存实际正文：

> {地块名称}的{设备名称}发生故障，请技术人员或农场主及时处理

启动迁移只升级未编辑、严格匹配旧模板的默认正文，并同步接收明细；自定义内容不会被模板覆盖，阅读状态和阅读时间保持不变。
新增 content_customized 标记：人工发布或编辑正文后设为1，防止重启迁移改写用户输入，即使输入恰好与旧模板相同也保留。
farm 消息页保留维护消息入口、个人未读数、分页列表及已读操作，同时区分自动故障消息和人工发布消息。

## 数据与权限

`maintenance_message` 保存消息本体和设备/地块名称快照。
自动消息使用唯一 `fault_id`，人工发布的 `fault_id` 为空，不创建或修改设备故障。
新增 `device_id`、`device_name`、`publisher_id`、`status`、`version` 字段。
人工发布人来自登录态，自动发布人显示“系统自动发布”。
`notification` 为接收明细：`notice_type=3`、`ref_type=maintenance_message`，`ref_id` 对应消息本体ID。
前端发布/编辑/删除接口始终使用消息本体ID，不能混用通知明细ID。

故障创建和通知生成同事务提交，唯一故障键及消息行锁保证自动发布幂等。
三个故障入口均保留：人工新增故障、设备健康状态自动建单、故障列表补偿建单。
启动时补发无消息的历史故障；已删除消息仍保留故障唯一键，不会被重新补发。
删除对本体和全部接收明细做同事务逻辑删除，所有端列表与统计排除已删除数据。
批量删除先校验全部消息权限，再执行删除，任意失败使整批回滚。
编辑使用 `version` 检测并发冲突，并同步所有活跃通知明细，保留接收人阅读状态。

通知只发送给启用的设备所属农场主及有效绑定的启用技术人员（支持多名并去重）。
无绑定技术人员时只通知农场主；后续绑定不会自动重发历史消息。
管理员管理全部消息；其他用户只管理自己收到的消息。
发布设备候选和提交均校验设备归属或有效技术人员绑定，不能自行指定发布人或接收人。
移动端强制按登录接收人查询和标记本人已读，不允许代读。
菜单、路由和后台管理接口使用 `maintenance_msg:manage` 权限。

## 接口

后端上下文 `/smart_plant`；统一返回 `{code,message,data}`。

| 方法 | 管理端路径 | 用途 |
| --- | --- | --- |
| GET | `/maintenance-msg/list` | 分页列表 |
| GET | `/maintenance-msg/statistics` | 四项统计 |
| GET | `/maintenance-msg/devices` | 有权发布的设备候选，支持keyword远程搜索 |
| GET | `/maintenance-msg/{id}` | 编辑回填 |
| POST | `/maintenance-msg/add` | 人工发布 |
| PUT | `/maintenance-msg/{id}` | 编辑 |
| DELETE | `/maintenance-msg/{id}` | 单条删除 |
| DELETE | `/maintenance-msg/batch` | 批量删除，JSON数组，最多100条 |
| PUT | `/maintenance-msg/{id}/read` | 本人已读 |

发布请求：`{deviceId,title,content,level}`；编辑请求增加必填 `version`。
标题长度1至100，正文不能为空或纯空白、最长10000字符（保留换行），级别1至3；编辑时不能更换设备。
列表参数：`title`、`content`、`plotName`、`pageNum`、`pageSize`，页大小上限100。
分页返回PageInfo；统计字段 `totalCount`、`todayCount`、`unreadDeliveryCount`、`readDeliveryCount`。
farm个人端继续使用 `/client/maintenance-msg` 下的 list、statistics、详情与read接口。

## 部署与验证

数据库结构只允许通过 Flyway 版本迁移，不再由启动初始化器执行 DDL。维护消息通知通过受外键保护的 `maintenance_message_id` 建立业务关联，`ref_type/ref_id` 仅用于投递分组。
本地 MySQL `smart_plant` 由 Flyway 执行增量升级并校验通知引用一致性。
重启/更新运行中的后端并刷新smart_farm即可使用新接口；farm需重新运行或打包。
消息随应用故障创建事务立即落库；页面每15秒同步新数据，编辑和勾选删除期间暂停后台轮询。
本次未接入短信或手机系统推送；禁止绕过应用直接写入故障数据，外部设备接入必须通过受控接口或消息消费服务。

验证命令：

- `mvn -f smart_plant/pom.xml "-Dtest=*Test,*Tests" test`
- `mvn -f smart_plant/pom.xml -Dtest=MaintenanceMessageIntegrationTest test`（11项事务集成测试）
- `npm --prefix smart_farm run build`（TypeScript检查与生产构建）

集成测试覆盖自动发布、人工发布、默认正文、自定义多行正文、自动/人工消息编辑及通知同步、启动迁移保护自定义内容、版本冲突、个人已读、跨用户越权、事务回滚、单条/批量删除、删除后不再补发。
集成测试依赖本地数据库的农场主设备及技术人员绑定，新增测试业务数据全部回滚。
手机真机和浏览器完整登录流程未在本轮验收。

本轮修复验证：27项后端测试通过，smart_farm类型检查与生产构建通过。默认文案只影响新消息；已保存正文通过编辑表单修改。运行中的后端需更新/重启，前端需刷新页面以加载新版请求字段。
