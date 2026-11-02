#!/usr/bin/env bash
# 用途：android-diagnostics 分歧验证——同一组向量分别喂给 JVM 单测源集里的 org.json 替身（JvmJson）
#       与设备上的真 org.json，断言两者解析成败一致、成功时值一致（A9/A7 相关；审阅 2026-11-02）。
# 向量：tests/android/android-diagnostics/json-divergence.json（JVM 测试运行时向上查找并读取）；
#       androidTest/OrgJsonDivergenceTest.kt 内嵌同一组数据（见文件注释）。
# 前置环境：GitHub Actions ubuntu-latest + JDK 17 + Android SDK + 已启动的模拟器（API >= 26）；
#           本机无 JDK/SDK，不跑构建。等组长的模拟器 CI 合入后才跑。
# 运行命令：bash tests/android/android-diagnostics/run-instrumented.sh
# 行为：产品未交付（缺少 android/gradlew 或 android/app）时打印“待测：模块未交付”并以 3 退出（CI 红）；
#       交付后把 JVM 测试拷入 app/src/test/java/…，androidTest 拷入 app/src/androidTest/java/…，
#       执行 ./gradlew testDebugUnitTest connectedDebugAndroidTest。
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$(cd "$here/../../.." && pwd)"
log_dir="$root/tests/results"
mkdir -p "$log_dir"
exec > >(tee "$log_dir/android-diagnostics-instrumented-$(date +%F).log") 2>&1

if [ ! -x "$root/android/gradlew" ] || [ ! -d "$root/android/app" ]; then
  echo "待测：模块未交付（缺少 android/gradlew 或 android/app）"
  exit 3
fi

jvm_dest="$root/android/app/src/test/java/com/osakaconnect/diagnostics"
android_dest="$root/android/app/src/androidTest/java/com/osakaconnect/diagnostics"
mkdir -p "$jvm_dest" "$android_dest"
cp "$here/JvmJsonDivergenceTest.kt" "$jvm_dest/"
cp "$here/androidTest/OrgJsonDivergenceTest.kt" "$android_dest/"
echo "JVM 测试已拷入 $jvm_dest"
echo "androidTest 已拷入 $android_dest"

cd "$root/android"
./gradlew testDebugUnitTest connectedDebugAndroidTest --console=plain
