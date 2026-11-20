# 商业化 Ready 总体计划（v2）

目标：从"个人可玩的原型"升级为"可商业化发布"的形态，分三条主线并行推进。

## 主线 A：后端（backend/flowclone-server）
A1 用户体系：邮箱+密码注册/登录（PBKDF2-SHA256 哈希）、会话 token（HMAC 签名，可扩展 JWT）、游客/设备登录、token 刷新与过期
A2 额度与账本：注册送额度、生成前扣减（图片 1 点/视频 10 点，服务端可配）、账本流水、余额查询、幂等
A3 任务服务：提交代理 → 本地 job 记录（SQLite）→ 后台 worker 轮询网关 → 完成落库 → 输出代理下载；任务列表（跨设备可见）
A4 推送与通知：FCM/APNs token 登记 + 完成事件（先做站内通知，推送 SDK 后续接）
A5 安全与运维：网关 Key 仅服务端环境变量、请求限流、请求日志、健康检查、错误码规范、CORS、HTTPS 部署说明
A6 管理面：用户/任务/账本查询接口（简单 Admin token）

## 主线 B：App 接入自建 Server
B1 网关切换：Gateway.kt base URL 指向自建 Server；鉴权从固定 Key 改为用户 token；401 处理与重登
B2 登录/注册页：邮箱+密码、注册/登录切换、错误提示、loading、协议勾选；游客模式
B3 会话持久化：token 存 SharedPreferences；启动时校验 /v1/me；无 token 进登录页
B4 兼容：保留直连网关的 Debug 开关（用于本地对比），正式默认走 Server
B5 真机联调：adb reverse + 本机 server + 注册登录 + 生成 + 杀进程恢复 + 断网错误态

## 主线 C：UI 保真
C1 产出「UI 细节对比表」：逐屏列出原版截图 vs 原生截图 + 差异项（间距/字号/圆角/颜色/阴影/按压态/动画/空态/弹层位置/文案）
C2 优先级：导航与弹层 > 核心编辑/生成页 > 次要页面
C3 批量修复并逐项勾选，更新 diff-score

## 验收标准
- Server：注册/登录/额度/任务接口 curl 冒烟通过
- App：登录后才能进首页；生成走 Server；杀进程恢复；断网错误态
- 文档：README 部署步骤（server 环境变量、adb reverse、HTTPS/域名）、商业化差异表更新
- 每批次构建+装机+截图证据
