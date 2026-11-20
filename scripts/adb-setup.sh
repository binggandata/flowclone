#!/usr/bin/env bash
# 手机联调：USB 调试授权后执行；服务端 8081 → 手机 127.0.0.1:8081
set -euo pipefail
adb devices
adb reverse tcp:8081 tcp:8081
echo "OK: 手机可通过 127.0.0.1:8081 访问本机服务端"
