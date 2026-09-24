# 安全配置与 Redis 状态部署

## 必需环境变量

应用不再提供数据库、Redis 或 JWT 的仓库内默认秘密。启动前必须通过部署平台的 Secret、密钥管理系统或进程环境注入：

| 变量 | 用途 | 要求 |
| --- | --- | --- |
| `DB_URL` | MySQL JDBC 地址 | 生产启用 TLS，禁止 `allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | MySQL 应用账号 | 使用最小权限账号，不使用 `root` |
| `DB_PASSWORD` | MySQL 密码 | 独立高熵秘密 |
| `JWT_SECRET` | HS256 签名密钥 | 至少 32 字节随机值 |
| `REDIS_HOST` | Redis 地址 | 使用私网地址或托管 Redis |
| `REDIS_PASSWORD` | Redis 密码 | 独立高熵秘密 |

可选变量包括 `REDIS_PORT`、`REDIS_USERNAME`、`REDIS_DATABASE`、`SECURITY_REDIS_KEY_PREFIX`、`REQUIRE_HTTPS` 和 `API_DOCS_ENABLED`。

## 生产要求

1. `API_DOCS_ENABLED` 保持 `false`；确需排障时短时开启，Swagger 仍需要有效 JWT。
2. 仅 `/actuator/health` 匿名开放。`info`、`prometheus` 等端点通过网关内网访问并携带运维账号 Bearer Token。
3. `/uploads/**` 需要有效登录态。登录接口同时写入 host-only、HttpOnly、SameSite=Lax 的访问 Cookie，仅 GET 上传资源可读取它；普通 API 和写请求仍只接受 Bearer Header。
4. 前端与 API 应部署在同站点网关后。跨站部署时不要放宽 Cookie，应改用对象存储私有桶和短时签名 URL。
5. Redis 必须开启认证、持久化/高可用和网络访问控制。Token 黑名单、单用户会话、IP/账号限流 Key 均有 TTL，不应由业务脚本手工清理。
6. 仓库历史中出现过的数据库密码和 JWT 密钥必须在上线前轮换；仅从当前配置文件删除并不能撤销已经泄露的秘密。

## Redis Key 模型

- `...:token-blacklist:<sha256>`：登出 Token，TTL 等于 Token 剩余有效期。
- `...:login-session:<sha256>`：用户当前 access/refresh JTI，TTL 等于 refresh Token 剩余有效期。
- `...:login-rate:ip:<sha256>`：IP 失败计数和锁定窗口。
- `...:login-rate:account:<sha256>`：账号失败计数和锁定窗口。

Key 中的账号、IP 和 JTI 均使用 SHA-256 摘要，避免在 Redis 运维界面暴露原始标识。
