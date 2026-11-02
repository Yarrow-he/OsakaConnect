#!/usr/bin/env bash
# 待 CI：本机无 JDK/SDK，不跑构建；以下命令只在 GitHub Actions（ubuntu-latest）执行。
# 用途：android-diagnostics JVM 单测——A7 LogRedactor 脱敏（redaction.json 全部 case）、A15 InviteCode.isValid（invite-code.json 全部正反例）、admin-contract.json 解析校验正反例（0.2.0 预检）。
# 前置环境：ubuntu-latest + JDK 17 + Android SDK；仓库根含 android/（可执行 gradlew、app 模块）与 shared/test-vectors/。
# 运行命令：bash tests/android/android-diagnostics/run.sh
# 行为：产品未交付时打印“待测：模块未交付”并以 0 退出；交付后把本目录测试拷入 app 测试源集并执行 ./gradlew testDebugUnitTest。
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$(cd "$here/../../.." && pwd)"
log_dir="$root/tests/results"
mkdir -p "$log_dir"
exec > >(tee "$log_dir/android-diagnostics-$(date +%F).log") 2>&1

if [ ! -x "$root/android/gradlew" ] || [ ! -d "$root/android/app" ]; then
  echo "待测：模块未交付（缺少 android/gradlew 或 android/app）"
  exit 0
fi

dest="$root/android/app/src/test/java/com/osakaconnect/diagnostics"
mkdir -p "$dest"
cp "$here"/*.kt "$dest/"
echo "测试源已拷入 $dest"

cd "$root/android"
./gradlew testDebugUnitTest --console=plain
