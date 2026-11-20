# Security Policy

## 报告问题
请通过仓库 Issue（或私下联系维护者）报告安全漏洞，不要公开利用细节。

## 敏感信息
- 本仓库不包含任何网关 Key、token、keystore、数据库或真实账号数据。
- 部署时请务必替换：`TOKEN_SECRET`、`ADMIN_TOKEN`，并使用服务端环境变量注入 `GATEWAY_KEY`。
- 不要把 `.env`、`gateway.properties`、`*.jks`、`flowclone.db` 提交到版本库。
- 生产环境请使用 HTTPS（反向代理），并限制 `/admin/*` 访问来源。

## 已知风险
- 上游 AI Gateway 的内容审核/风控由服务提供方负责。
- 本项目的账号/额度为教学实现，未做完整风控与合规审计。
