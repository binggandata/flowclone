#!/bin/bash
# 用法: tools/cap.sh <步骤名>  → 截图 + uiautomator 树，存 evidence/
ADB=$HOME/Library/Android/sdk/platform-tools/adb
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
name="${1:?name}"
ts=$(date +%H%M%S)
$ADB exec-out screencap -p > "$ROOT/evidence/screens/${name}-${ts}.png" 2>/dev/null
$ADB shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
$ADB exec-out cat /sdcard/ui.xml > "$ROOT/evidence/ui/${name}-${ts}.xml" 2>/dev/null
echo "screens/${name}-${ts}.png"
