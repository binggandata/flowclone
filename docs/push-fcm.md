# 推送通道（FCM）接入指南

当前状态：**服务端已就绪**（`POST /v1/push/register` 登记 token；任务完成写 `notifications` 表；App 已能轮询通知）。
端侧 FCM 需要 Google 服务配置，以下为接入步骤（未配置前用「轮询 + 服务端通知」替代，功能已可用）。

## 1. 创建 Firebase 项目
1. console.firebase.google.com → 新建项目（或使用已有）
2. 添加 Android 应用：包名 `com.example.flowclone`
3. 下载 `google-services.json` 放到 `native/app/google-services.json`

## 2. Gradle 配置（native/）
`build.gradle.kts`（root）：
```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```
`app/build.gradle.kts`：
```kotlin
plugins { id("com.google.gms.google-services") }
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-messaging")
}
```

## 3. 端侧代码（骨架，可直接放入项目）
```kotlin
class FlowMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        // 上报服务端：POST /v1/push/register {token, platform:"android"}
        PushReporter.register(token)
    }
}

// 启动时获取 token
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) PushReporter.register(task.result)
}
```
`PushReporter` 用现有 Gateway 鉴权头调用 `POST /v1/push/register`（服务端已实现，字段：`{token, platform}`）。

## 4. 服务端发送（FCM HTTP v1）
- 从 Firebase 控制台 → 项目设置 → 服务账号 → 生成私钥 JSON
- 服务端把 JSON 放到 `FCM_SERVICE_ACCOUNT=/path/service-account.json`
- 在 `notify_job()` 里追加：向该用户 `push_tokens` 中所有 token 调用
  `https://fcm.googleapis.com/v1/projects/<project-id>/messages:send`
  消息体：`{"message":{"token":"...","notification":{"title":"生成完成","body":"..."},"data":{"jobId":"..."}}}`
- 需要 OAuth2 access token（服务账号 JWT 交换），可用 google-auth 库或自行签署 JWT

## 5. 环境变量（服务端）
| 变量 | 说明 |
|---|---|
| `FCM_SERVICE_ACCOUNT` | Firebase 服务账号 JSON 路径（启用推送时必填） |
| `FCM_PROJECT_ID` | Firebase 项目 ID |

## 6. 测试
- 真机安装带 FCM 的包 → 登录 → 服务端 `push_tokens` 表应出现 token
- 提交生成 → 等待完成 → 应收到系统通知；App 通知页同时存在服务端通知
