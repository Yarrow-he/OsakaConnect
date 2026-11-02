#!/usr/bin/env bash
# 待 CI：本机无 JDK/SDK，不跑构建；以下命令只在 GitHub Actions（ubuntu-latest）执行。
# 用途：android-diagnostics JVM 单测——A7 LogRedactor 脱敏（redaction.json 全部 case）、A15 InviteCode.isValid（invite-code.json 全部正反例）、admin-contract.json 解析校验正反例（0.2.0 预检）。
# 前置环境：ubuntu-latest + JDK 17 + Android SDK；仓库根含 android/（可执行 gradlew、app 模块）与 shared/test-vectors/。
# 运行命令：bash tests/android/android-diagnostics/run.sh
# 行为：产品未交付时打印“待测：模块未交付”并以 3 退出（3 = 待测/未交付，不算通过）；交付后把本目录测试拷入 app 测试源集并执行 ./gradlew testDebugUnitTest，
#       再按 test-results 的 tests-skipped 统计执行数，为 0 或没有结果文件则 exit 1。
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$(cd "$here/../../.." && pwd)"
log_dir="$root/tests/results"
mkdir -p "$log_dir"
exec > >(tee "$log_dir/android-diagnostics-$(date +%F).log") 2>&1

if [ ! -x "$root/android/gradlew" ] || [ ! -d "$root/android/app" ]; then
  echo "待测：模块未交付（缺少 android/gradlew 或 android/app）"
  exit 3
fi

dest="$root/android/app/src/test/java/com/osakaconnect/diagnostics"
mkdir -p "$dest"
cp "$here"/*.kt "$dest/"
echo "测试源已拷入 $dest"

cd "$root/android"
./gradlew testDebugUnitTest --console=plain

results_dir="$root/android/app/build/test-results/testDebugUnitTest"
shopt -s nullglob
xmls=("$results_dir"/*.xml)
shopt -u nullglob
if [ "${#xmls[@]}" -eq 0 ]; then
  echo "测试执行数为 0：未找到 $results_dir/*.xml"
  exit 1
fi

# JUnit XML 每个 <testsuite> 带 tests/skipped；执行数 = tests - skipped（全 skip 也视为未执行）。
read -r total_tests total_skipped <<< "$(awk '/<testsuite[ >]/ {
  if (match($0, /tests="[0-9]+"/)) t += substr($0, RSTART + 7, RLENGTH - 8)
  if (match($0, /skipped="[0-9]+"/)) s += substr($0, RSTART + 9, RLENGTH - 10)
} END { print t + 0, s + 0 }' "${xmls[@]}")"
executed=$((total_tests - total_skipped))
if [ "$executed" -le 0 ]; then
  echo "测试执行数为 0（tests=$total_tests, skipped=$total_skipped）：用例全部未执行或被跳过"
  exit 1
fi
echo "测试执行数：$executed（tests=$total_tests, skipped=$total_skipped）"
