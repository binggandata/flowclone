# bggg-copy-app

Android App 高保真复刻方法论 skill（净室优先），由 FlowClone（Google Flow 复刻）项目沉淀。

## 安装（DeepSeek Harness）

把本目录复制到用户级 skill 目录即可被 DSH 发现：

```bash
mkdir -p ~/.agents/skills
cp -R bggg-copy-app ~/.agents/skills/bggg-copy-app
```

也支持项目级：`<project>/.dsh/skills/bggg-copy-app/`。

## 使用

在会话中直接说“按 bggg-copy-app 复刻这个 App”，或让 Agent 加载 `bggg-copy-app` skill。入口文件：`SKILL.md`。

## 目录

- `SKILL.md`：入口（工作流、硬性规则、资源索引）
- `references/`：接入协议、解析、深度标准、效果级验收、重建、踩坑、DSH 并行、交付清单、案例
- `templates/`：项目计划、探索日志、UI 差异表、行为审计、验收报告
- `scripts/`：ADB 检查、UI 树解析、截图归档

## 许可

MIT。方法论与模板由 FlowClone 项目实践整理；引用 REA / craft-anything 仅作方法论对照，未包含其代码。
