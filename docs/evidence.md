# 证据与对比（说明）

本仓库**不包含**开发过程中的原版截图、语义树、真机对比图与生成内容（体积大，且原版 UI 截图可能涉及第三方版权）。

复现方式：
1. 真机上按 `skills/bggg-copy-app/references/recon-protocol.md` 采集：截图 + `uiautomator dump`
2. 用 `tools/ui.py` 解析可点击元素；`tools/cap.sh` 归档截图
3. 按 `spec/ui-diff-table.md`、`spec/behavior-audit.md` 记录原版 vs 复刻
4. 生成对比并排图与验收报告（模板见 skill `templates/`）

> 对比方法论与 FlowClone 的实测结论见 `docs/replication-retrospective.md` 与 `docs/development-log.md`。
