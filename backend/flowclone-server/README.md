# FlowClone Server（商业化最小后端）

App ⇄ 本服务 ⇄ NGSFlow 网关。AI 网关密钥只放服务端，App 不再内置密钥。

## 启动
```bash
export GATEWAY_URL=https://your-gateway.example
export GATEWAY_KEY=<gateway-key>        # 从 native/gateway.properties 迁到服务端环境变量
python3 server.py                     # 默认 0.0.0.0:8081
```

## 与 App 联调
```bash
adb reverse tcp:8081 tcp:8081
```
然后重新构建 App，把网关地址指到 `http://127.0.0.1:8081`（`native/gateway.properties` 的 `url=`，key 可留空走 /v1/auth/device 换 token，当前版本 App 仍用 Bearer key 直连模式，切换需小改 Gateway.kt）。

## 新增（第 8 轮）
- `DELETE /v1/account`：注销账号并级联删除该用户 jobs/notifications/ledger/refresh_tokens/push_tokens（App 账号页入口 + 二次确认）

## 新增（第 5 轮）
- **短期 access token（1h）+ refresh token（90d，轮换）**：`/v1/auth/refresh`、`/v1/auth/logout`
- **服务端通知**：任务完成/失败自动落 `notifications` 表；`GET /v1/notifications`
- SQLite 并发修复：`with db()` 改为正确关闭连接的上下文管理器 + WAL + busy_timeout（解决 database is locked）

## 新增（第 2 轮）
- `GET /v1/jobs?limit=20` 用户任务列表（跨设备历史）
- 限流：生成 30 次/分钟/用户（环境变量 `RATE_LIMIT_PER_MIN` 控制默认 60 次/分钟）
- CORS：支持 Web 端（OPTIONS + Access-Control-*）
- Admin 只读接口（需 `X-Admin-Token` = 环境变量 `ADMIN_TOKEN`）：
  - `/admin/users`、`/admin/jobs`、`/admin/ledger`、`/admin/stats`

## 已实现
- `POST /v1/auth/device` 设备登录（发 token + 初始额度 100）
- `POST /v1/generations` 代理生成 + 额度扣减 + 任务落 SQLite（返回本地 job id）
- `GET /v1/jobs/{id}` 代理任务状态（本地持久化，重启不丢）
- `GET /v1/jobs/{id}/outputs/{i}/content` 代理下载
- `POST /v1/files` 代理上传
- `GET /v1/me` 额度；`POST /v1/push/register` 推送 token 登记
- 账本表 `ledger` 记录每次扣费

## 下一步（商业化 P0/P1）
1. JWT 鉴权替换简易 token；用户表接订阅状态
2. 异步 worker：提交后由后台线程拉取网关结果并推送 FCM
3. 对象存储：下载结果转存 OSS/S3 + CDN 签名 URL（App 不再直接存本地）
4. 审核：提交前文本审核、下载前图像审核
5. 计费：Google Play Billing 校验 + 点数购买/退款
6. 运营后台 + 指标（任务成功率、时延、成本）
