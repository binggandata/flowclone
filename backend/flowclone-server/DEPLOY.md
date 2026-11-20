# FlowClone Server 部署与上线清单

## 一、环境变量

| 变量 | 必填 | 说明 |
|---|---|---|
| `GATEWAY_URL` | ✅ | NGSFlow 网关地址（`https://your-gateway.example`） |
| `GATEWAY_KEY` | ✅ | 网关 Key（**只放服务端**，不进 APK） |
| `TOKEN_SECRET` | ✅ | 用户 token 的 HMAC 密钥，**上线必须更换**（默认值仅供本地） |
| `ADMIN_TOKEN` | ✅ | 管理接口 `X-Admin-Token`，上线必须更换 |
| `PORT` | 否 | 默认 8081 |
| `DB_PATH` | 否 | SQLite 路径，默认同目录 `flowclone.db` |
| `RATE_LIMIT_PER_MIN` | 否 | 全局默认 60 次/分钟，生成单独 30 次/分钟 |
| `ACCESS_TTL_SECONDS` | 否 | access token 有效期，默认 3600（1 小时）；refresh token 90 天并轮换 |
| `GATEWAY_INSECURE` | 否 | 1=跳过网关自签证书校验（默认 1；上线建议置 0 并在网关配正式证书） |

启动：
```bash
GATEWAY_URL=... GATEWAY_KEY=... TOKEN_SECRET=... ADMIN_TOKEN=... \
  python3 server.py
```

## 二、HTTPS 部署（示例：Caddy）

```caddyfile
api.example.com {
    reverse_proxy 127.0.0.1:8081
}
```
- Caddy 自动签发/续期证书；Nginx 类似：`proxy_pass http://127.0.0.1:8081;`
- 上线后 App 端 `native/gateway.properties` 改为 `url=https://api.example.com`（`key` 留空）

## 三、systemd 服务

```ini
[Unit]
Description=FlowClone Server
After=network.target

[Service]
WorkingDirectory=/opt/flowclone-server
EnvironmentFile=/opt/flowclone-server/.env
ExecStart=/usr/bin/python3 server.py
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
```

## 四、App 发布构建

1. `native/gateway.properties`：`url=https://api.example.com`、`key=`（空）
2. 签名（keystore 已在 `native/your-release.jks`，口令环境变量覆盖）：
```bash
export RELEASE_STORE_PASSWORD=*** RELEASE_KEY_ALIAS=flowclone RELEASE_KEY_PASSWORD=***
cd native && ./build.sh assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk（已签名，v1.0.0 / versionCode 10）
# 上架建议再出 AAB：gradle bundleRelease
```
3. 验证签名：`apksigner verify --print-certs app-release.apk`

## 五、安全与运维
- `TOKEN_SECRET`、`ADMIN_TOKEN`、`GATEWAY_KEY` 全部走环境变量/密钥管理，禁止提交
- 定期备份 `flowclone.db`（用户/任务/账本）
- 日志：stdout（systemd journal）；建议接日志采集与任务成功率告警
- 限流已内置（生成 30/min/用户），线上可按需调低

## 六、上架 Google Play 前 checklist
- [ ] 隐私政策 + 数据安全表单（收集邮箱、生成提示词、生成内容）
- [ ] 内容审核机制（AI 生成内容政策）、举报入口已就绪（App 内已有举报 UI）
- [ ] 订阅/点数计费接 Google Play Billing（服务端校验 purchaseToken）
- [ ] FCM 推送（生成完成通知）证书配置
- [ ] 目标 API 级别/64 位/权限声明合规
- [ ] release 签名密钥离线妥善保存（丢失无法更新应用）
