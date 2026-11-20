# Google Flow 移动端复刻 · UI 规格 v0

> 依据：2026-11-18 真机采集（见 `evidence/notes/phase1-observations.md`，截图在 `evidence/screens/`）。
> 目标：先做**移动端 Web（PWA）前端**，假数据跑通全部界面与交互；第二阶段再反推后端接口、换真实网关。
> 选择 Web 的理由：本机没有 Android 构建链（Flutter zip 2GB 下载不稳定），Web 可以即时迭代、手机 Chrome 直接打开、后期可用 Capacitor 套壳成 APK。

## 1. 设计 token（v0，后续对照修正）

```css
--bg: #000000;                 /* 全局背景 */
--surface: #2c2d30;            /* 卡片、按钮、输入框 */
--surface-2: #171819;          /* 次级面板/输入框内层 */
--surface-3: #3a3b3e;          /* 进度卡渐变顶 */
--text: #ffffff;
--text-secondary: #9aa0a6;     /* 次要文字 */
--text-tertiary: #808080;      /* 空态文字 */
--accent: #ff5722;             /* 头像底色（Google 橙） */
--chip-selected-bg: #ffffff;   /* 选中胶囊 */
--chip-selected-text: #000000;
--divider: rgba(255,255,255,.08);
--overlay: rgba(0,0,0,.6);

/* 字体：Google Sans 拿不到，用 Roboto/system */
--font: 'Google Sans', 'Roboto', -apple-system, 'Noto Sans SC', sans-serif;

/* 圆角 */
--r-card: 20px;   --r-sheet: 28px;  --r-pill: 999px;  --r-btn: 24px;

/* 页面尺度（参考 1080×2340 @440dpi → CSS 用 393×851 CSS px） */
--status-h: 39px;   /* 108/2.75 */
--appbar-h: 48px;   /* 132/2.75 */
--page-pad: 16px;   /* 44/2.75 */
--card-w: 112px;    /* 309/2.75 模板卡宽 */
--card-h: 196px;    /* 539/2.75 模板卡高 */
--asset-card: 195px;/* 537/2.75 素材卡（2 列网格） */
```

字号（估算）：页面大标题 30/700；区块标题（项目）30/600；正文 16；按钮 16/500；次要 14；空态 16/400。

## 2. 组件

| 组件 | 规格 |
|---|---|
| 顶部 AppBar | 高 48，左标题 30px；右侧 48×48 圆形按钮（铃铛/头像）；返回键为 48×48 圆角方块（radius 16） |
| 模板卡 | 112×196 图片卡，圆角 20，底部横向滚动；卡片下文字 16px，居中 |
| 项目列表项 | 整行 360×96；左侧 96×96 缩略图（圆角 12），右侧标题 16 + 图标计数（视频/图片）；最右 3 点 |
| 素材卡（2 列） | 195×347（9:16）；圆角 20；视频左上播放按钮；生成中：顶部右「36%」、中央「正在生成...」、右下角重试 |
| 筛选 chips | 高 36；选中白底黑字，未选 #2c2d30；图标+文字 |
| 悬浮操作条 | 底部三颗：48 圆(搜索) + 100×48 胶囊(创建，sparkle+文字) + 48 圆(上传)，浮起 24px |
| 创建面板（底部 sheet） | 全屏 sheet；左上圆形关闭；右上会话历史（仅智能体）；建议卡 3 张（3D 图标+文字，272×64）；tab 行（图片/视频/智能体，选中白胶囊）；输入框（#171819，圆角 28，placeholder「您想制作什么内容?」/「描述动作...」）；左下 +（添加媒体）；发送键白色圆形箭头 |
| 设置抽屉 | 挂在输入框下方：消耗点数行 + 分段控件（比例 5 项/数量 4 项/分辨率 2 项/时长步进/模型选择行） |
| 模型列表 | 自底向上排列：模型名 + 右侧「N 个点数/video」；底部当前选中行带 ✕ |
| 素材详情 | 顶栏：返回 + 标题/模型标签/分辨率 chip + 3 点；内容区 1/1 pager；底部两颗胶囊按钮（修改 / 添加动画效果 或 延长） |
| 底部菜单 | 圆形浮层：喜欢/不喜欢 双按钮 + 列表项（重命名/重复使用提示/收藏/复制/下载/发送/分享/YouTube/报告） |
| 通知页 | 顶栏「通知」+ 右侧「剩余点数：1040」；通知卡：标题加粗 + 时间 + 相对时间 |
| 账号页 | 账号卡（头像/名/邮箱/PRO 徽章）；点数卡 + 查看活动；列表组；开关行；链接行 |

## 3. 页面与路由（Web 版）

| 路由 | 页面 | 关键状态 |
|---|---|---|
| `/` | 首页 | 空项目态 / 有项目列表；模板卡横滑 |
| `/project/:id` | 项目页 | 空态 / 有素材（筛选 全部/图片/视频）；生成中卡片（进度%）；底部悬浮条 |
| `#composer` | 创建面板（overlay） | 智能体 / 图片 / 视频 三模式；设置抽屉；模型列表；提示词为空时发送键禁用 |
| `#composer-animate` | 动画模式 | 首帧预览 + 描述动作输入 + 视频参数 |
| `/asset/:id` | 素材详情 | 图片（修改/动画）/ 视频（播放/延长）；菜单 |
| `/notifications` | 通知 | 列表 |
| `/account` | 账号 | 点数/开关/链接 |

## 4. 关键交互状态

- 创建面板：进入自动聚焦输入框（真机键盘弹出）；建议卡点击填充提示词；图片/视频/智能体切换改变底部 chip 与设置项；发送后关闭面板、项目网格插入「生成中」卡。
- 生图：0 点数，约 1–5s 完成；生成中卡片显示百分比与「正在生成...」；完成后变缩略图。
- 生视频：10–24 点数，约 30–60s；完成后出现播放标记与通知；详情页可播放。
- 筛选：有素材后出现；全部/图片/视频过滤。
- 菜单：素材级与项目级两套；重命名、收藏、下载、分享等均为交互占位（假数据阶段 toast 提示）。

## 5. 假数据模型（前端先定）

```ts
Project { id, title, createdAt, assets: Asset[] }
Asset { id, projectId, type: 'image'|'video', title, model, resolution, aspect, prompt,
        status: 'queued'|'generating'|'completed'|'failed', progress, createdAt,
        durationSec?, favorite?, liked?: 'like'|'dislike'|null }
GenerationJob { id, assetId, model, aspect, count, durationSec, costCredits, status, progress }
Notification { id, text, createdAt, read }
Account { name, email, plan:'PRO', credits, watermark:boolean, improveFlow:boolean }
```

## 6. 第二阶段：后端接口推断（草稿，待实测补全）

| UI 动作 | 推断接口 |
|---|---|
| 首页项目列表 | `GET /projects` |
| 新建项目 | `POST /projects` → `{id,title}` |
| 项目详情素材 | `GET /projects/{id}/assets` |
| 提交生成 | `POST /generations` `{projectId,model,prompt,aspect,resolution,count,durationSec}` → `{jobId,cost}` |
| 轮询进度 | `GET /generations/{jobId}` → `{status,progress,asset}` |
| 素材详情 | `GET /assets/{id}` |
| 重命名/收藏/喜欢 | `PATCH /assets/{id}` |
| 删除 | `DELETE /assets/{id}` |
| 通知列表 | `GET /notifications` |
| 点数 | `GET /credits` / `GET /credits/activity` |
| 模型与价格 | `GET /models?type=image|video`（含 cost、参数能力：分辨率/时长/音频） |
| 上传 | `POST /uploads`（先本地模拟） |

注意：真机显示「图片 0 点数」「视频 10/12/24 点数」，说明**计费按模型**；参数能力按模型不同（Veo Lite 无分辨率/时长选项），后端模型元数据必须携带能力矩阵。

## 7. 第二轮新增规格（S26–S30）

### 7.1 上传/相机入口统一闸门
所有「编辑视频 / 编辑照片 / 为照片添加动画效果 / 拍摄视频 / 拍照 / 上传」入口，先弹**权利确认弹窗**：
标题「通知」+ 两段政策文案（必要权利 / 《使用限制政策》是两个可点链接）+ `取消` / `我同意`。同意后按入口分流：
- 图片/视频选择 → **系统 DocumentsUI 文件选择器**（本项目无需自建相册，直接调用系统相册 INTENT）
- 拍照 → 系统相机（IMAGE_CAPTURE）
- 拍摄视频 → 系统相机（VIDEO_CAPTURE）

### 7.2 项目页补充
- 上传按钮 → 底部弹出：拍摄 / 图片 / 视频（三行）
- 搜索按钮 → 全屏搜索页（关闭 X + 输入框 + 空态提示）

### 7.3 智能体（Agent）页面族
| 页面 | 结构 |
|---|---|
| 创建面板·智能体模式 | 右上「会话历史记录」；建议卡 3 张（轮换文案）；输入框 placeholder「您想制作什么内容?」→ 聊天模式后变为消息流；底部：[+] [智能体 chip] [⚙ 智能体设置] [→] |
| 聊天消息流 | 用户气泡（右） + 智能体气泡（左，多行文本）；气泡下反馈按钮：答得好 / 答得不好 / 复制 / 举报输出内容 |
| 会话历史记录 | 标题「会话」；会话卡（标题 + 日期 + ⋮）；底部「创建新会话」 |
| 智能体设置 | 请求许可（始终/永不单选）、默认模型（图片模型 / 视频模型）、默认宽高比 |

### 7.4 视频详情与延长
- 视频详情：全宽视频 + 底部悬浮控件（播放/暂停、进度线、当前时长、静音）+ 底部「修改 / 延长」；⋮ 菜单（含分享到 YouTube）。
- 延长 composer：视频预览 + **胶片条帧选择（裁剪手柄）** + 输入框「后续步骤」 + chip「延长 8 秒」 + 发送。

### 7.5 复刻版实现状态（v0.2）
已完成：首页、项目页（空态/网格/筛选/生成中）、创建面板（图片/视频/智能体/聊天）、设置抽屉、模型列表、图片详情、视频详情（控件/修改/延长入口）、素材菜单、分享面板、延长 sheet、上传 sheet、搜索页、通知、账号、智能体历史/设置、虚拟形象介绍、权利确认弹窗、真实字体图标（SVG）。
待补：系统相机/相册 picker（原生 APK 阶段）、虚拟形象英文同意页、会话历史更多项、回收站、失败/取消状态、视频胶片条。

## 8. 阶段二（后端反推）假设补充
- 智能体会话独立于项目（会话历史里有独立标题/时间）→ 推断后端有 `conversations` 资源（`GET/POST /conversations`、`POST /conversations/{id}/messages`）。
- 设置页有「请求许可、默认模型、默认宽高比」→ 推断 `GET/PATCH /settings`。
- 延长是独立任务类型（`POST /generations` 带 `mode: extend` + `sourceAssetId` + `duration`）。
- 上传/相机为空操作（本地选文件），推断媒体先走 `POST /uploads` 拿 asset id，再在生成请求里引用。

## 9. 原生 APK 实现状态（v0.4）
- 已完成页面：首页、项目页、创建面板（智能体/图片/视频）、智能体聊天、智能体会话历史、智能体设置、素材详情（图片/视频）、搜索页、延长 composer、虚拟形象介绍、通知页、账号页
- 已完成交互：项目/素材菜单、上传菜单、权利确认、分享面板、删除二次确认、通知跳转项目、假生成（进度/扣点/通知）
- 对照调优：项目标题 24sp / chips 13sp+图标 / 网格通栏 4dp 间距 / 首页标题 26sp / 头像 44dp
- 剩余差异：状态持久化、真实素材缩略图、系统相册相机、分享到 YouTube 等第三方占位、空态/失败态尚未覆盖
- 构建：`native/build.sh` → `app/build/outputs/apk/debug/app-debug.apk`（约 19MB）
