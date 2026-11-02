#!/usr/bin/env bash
# 用途：android-shell CI 冒烟（A5）：assembleDebug、安装 APK、启动 Activity，检查进程存活、
#       adb logcat 无 FATAL EXCEPTION，并冒烟确认生命周期回调日志（A6 的日志入口）。
# 前置环境：GitHub Actions ubuntu-latest + 已启动的 Android 模拟器（adb devices 可见）；
#           JDK/Android SDK 由 CI 提供。本机不装 JDK/SDK、不跑构建——待 CI。
# 运行命令：bash tests/android/android-shell/ci-run.sh
# 覆盖 acceptance：A5；A6（生命周期日志冒烟；两页切换断言在 androidTest 草稿）。
# 产物：默认写到本目录 results/，便于 CI artifact 上传（可用 RESULT_DIR 覆盖）。
set -euo pipefail

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT=$(cd "$HERE/../../.." && pwd)
ANDROID_DIR=${ANDROID_DIR:-$ROOT/android}
RESULT_DIR=${RESULT_DIR:-$HERE/results}

# 待写手交付后对齐：包名、启动 Activity、生命周期日志 tag，均可用环境变量覆盖。
APP_ID=${OSAKA_ANDROID_APP_ID:-com.osaka.connect}
MAIN_ACTIVITY=${OSAKA_ANDROID_MAIN_ACTIVITY:-.MainActivity}
LIFECYCLE_TAG=${OSAKA_ANDROID_LIFECYCLE_TAG:-OsakaLifecycle}
MAIN_COMPONENT="$APP_ID/$MAIN_ACTIVITY"

step() { echo "== $*"; }
die() { echo "失败：$*"; exit 1; }

# 产品代码未交付：明确提示后以退出码 0 结束，不伪装通过（此路径不创建产物）。
[ -d "$ANDROID_DIR" ] || { echo "待测：模块未交付（android/ 目录不存在）"; exit 0; }
[ -x "$ANDROID_DIR/gradlew" ] || { echo "待测：模块未交付（android/gradlew 不存在）"; exit 0; }

mkdir -p "$RESULT_DIR"
LOG="$RESULT_DIR/ci-run-$(date +%Y%m%d-%H%M%S).log"
exec > >(tee "$LOG") 2>&1

step "检查模拟器"
command -v adb >/dev/null || die "CI 环境缺少 adb"
adb start-server >/dev/null
timeout 180 adb wait-for-device || die "等待设备超时"
adb devices | awk 'NR > 1 && $2 == "device" { found = 1 } END { exit !found }' \
  || die "没有已连接的模拟器/设备"

step "assembleDebug"
(cd "$ANDROID_DIR" && ./gradlew --no-daemon --stacktrace assembleDebug)
APK=$(find "$ANDROID_DIR" -type f -path '*/build/outputs/apk/debug/*.apk' | head -n 1)
[ -n "$APK" ] || die "assembleDebug 未产出 debug APK"

step "安装 $(basename "$APK")"
adb install -r "$APK" >/dev/null

step "启动 $MAIN_COMPONENT"
adb logcat -c || true
adb shell am force-stop "$APP_ID" || true
adb shell am start -W -n "$MAIN_COMPONENT" | tee "$RESULT_DIR/am-start.log"
grep -q "Status: ok" "$RESULT_DIR/am-start.log" || die "Activity 启动未返回 Status: ok"
sleep 3

PID=$(adb shell pidof "$APP_ID" 2>/dev/null | tr -d '\r' || true)
[ -n "$PID" ] || die "启动后进程不存在（可能已崩溃）：$APP_ID"

step "检查 logcat 崩溃与生命周期日志"
adb logcat -d > "$RESULT_DIR/logcat-full.log" || true
adb logcat -d -b crash > "$RESULT_DIR/logcat-crash.log" || true
CRASH=$(grep -nE "FATAL EXCEPTION|Process: $APP_ID" \
  "$RESULT_DIR/logcat-crash.log" "$RESULT_DIR/logcat-full.log" || true)
[ -z "$CRASH" ] || { echo "$CRASH"; die "logcat 发现 FATAL EXCEPTION"; }

adb logcat -d -s "$LIFECYCLE_TAG" > "$RESULT_DIR/logcat-lifecycle.log" || true
for token in oncreate onresume; do
  grep -qi "$token" "$RESULT_DIR/logcat-lifecycle.log" \
    || die "生命周期日志缺少 $token（tag=$LIFECYCLE_TAG；tag/格式待写手交付后对齐）"
done

if find "$ANDROID_DIR" -type d -path '*/src/androidTest' -print -quit | grep -q .; then
  step "connectedDebugAndroidTest（A6/A16）"
  (cd "$ANDROID_DIR" && ./gradlew --no-daemon connectedDebugAndroidTest)
else
  echo "提示：androidTest 源集未就绪，跳过仪器测试（草稿见 androidTest/ShellNavigationTest.kt，待写手交付后对齐）"
fi

echo "通过：A5 启动无崩溃；生命周期日志已记录（日志：$LOG）"
