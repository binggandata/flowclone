#!/usr/bin/env bash
# 构建 Android APK：需要 JDK17 + Android SDK + Gradle 8.9
set -euo pipefail
cd "$(dirname "$0")/../native"
: "${JAVA_HOME:?set JAVA_HOME to JDK 17}"
: "${ANDROID_HOME:?set ANDROID_HOME to Android SDK}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
# Gradle 8.9 可来自 PATH 或 gradle wrapper
"${GRADLE:-gradle}" assembleDebug --no-daemon --console=plain
echo "APK: native/app/build/outputs/apk/debug/app-debug.apk"
