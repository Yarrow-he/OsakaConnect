#!/usr/bin/env bash
# 用途：android/ 源码守卫——A14 生产代码只允许 Kotlin/Gradle KTS，A17 源码不含线路字面量
#       （验收点名的 “Line 01”/“OSA · 01” 与明显硬编码地址）。
# 前置环境：bash + grep/find；GitHub Actions ubuntu-latest。本机不装 JDK/SDK，不跑构建——待 CI。
# 运行命令：bash tests/android/android-shell/lang-guard.sh [--self-test]
# 覆盖 acceptance：A14、A17（Android 端）。
# 说明：AndroidManifest/res 等非代码文件与 gradlew 包装脚本不参与语言判定（A14 约束生产代码语言）。
set -euo pipefail

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT=$(cd "$HERE/../../.." && pwd)
ANDROID_DIR=${ANDROID_DIR:-$ROOT/android}

# 其他语言的生产代码扩展名（A14）；.java 明确违规（Android 只允许 Kotlin），.gradle 为 Groovy DSL。
OTHER_LANG_EXT=(java cs py js ts jsx tsx c cc cpp cxx h hpp go rs swift rb php scala dart m mm gradle)
# A17：验收点名名称 + 明显硬编码地址（IPv4/URL）；主机名形态待写手交付后按线路字段补充。
ROUTE_PATTERN='Line 01|OSA[[:space:]]*·[[:space:]]*01|([0-9]{1,3}\.){3}[0-9]{1,3}|https?://'

fail=0
report() { echo "违规：$*"; fail=1; }

scan() {
  local dir=$1 f ext bad
  while IFS= read -r -d '' f; do
    ext=${f##*.}
    ext=${ext,,}
    for bad in "${OTHER_LANG_EXT[@]}"; do
      [ "$ext" = "$bad" ] && report "A14 非 Kotlin/Gradle KTS 文件：${f#"$ROOT"/}"
    done
  done < <(find "$dir" \( -type d \( -name build -o -name .gradle -o -name .idea \) -prune \) \
    -o -type f -name '*.*' -print0)

  while IFS= read -r -d '' f; do
    if grep -nE "$ROUTE_PATTERN" "$f" >/dev/null 2>&1; then
      report "A17 源码含线路字面量：${f#"$ROOT"/}"
      grep -nE "$ROUTE_PATTERN" "$f" | sed 's/^/    /'
    fi
  done < <(find "$dir" \( -type d \( -name build -o -name .gradle -o -name .idea -o -name res \) -prune \) \
    -o -type f \( -name '*.kt' -o -name '*.kts' \) -print0)
}

if [ "${1:-}" = "--self-test" ]; then
  tmp=$(mktemp -d)
  trap 'rm -rf "$tmp"' EXIT
  mkdir -p "$tmp/android/app/src/main/kotlin/com/example"
  printf 'package com.example\n\nclass Main\n' > "$tmp/android/app/src/main/kotlin/com/example/Main.kt"
  ANDROID_DIR="$tmp/android" bash "$0" >/dev/null || { echo "自检失败：合法 Kotlin 工程被误报"; exit 1; }

  mkdir -p "$tmp/android/app/src/main/java/com/example"
  printf 'class Legacy {}\n' > "$tmp/android/app/src/main/java/com/example/Legacy.java"
  if ANDROID_DIR="$tmp/android" bash "$0" >/dev/null; then echo "自检失败：Java 文件未被拦截"; exit 1; fi
  rm -rf "$tmp/android/app/src/main/java"

  printf 'package com.example\n\nval line = "OSA · 01"\n' > "$tmp/android/app/src/main/kotlin/com/example/Route.kt"
  if ANDROID_DIR="$tmp/android" bash "$0" >/dev/null; then echo "自检失败：线路字面量未被拦截"; exit 1; fi

  echo "自检通过"
  exit 0
fi

[ -d "$ANDROID_DIR" ] || { echo "待测：模块未交付（android/ 目录不存在）"; exit 0; }

scan "$ANDROID_DIR"
[ "$fail" -eq 0 ] || exit 1
echo "通过：A14/A17（android/）"
