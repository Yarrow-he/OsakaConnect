#!/usr/bin/env bash
# A11：各端源码不得引用其他端目录；shared/、openapi/ 允许。用法：check-cross-refs.sh [仓库根目录]
set -euo pipefail
root=${1:-.}
[ -d "$root" ] || { echo "::error::根目录不存在：$root"; exit 2; }
cd "$root" || { echo "::error::无法进入根目录：$root"; exit 2; }
ends=(windows admin android server)
for e in "${ends[@]}"; do
  [ -d "$e" ] || { echo "::error::缺少端目录：$e"; exit 2; }
done
rc=0
# 先收集 find 结果并检查退出码：进程替换里的失败不会传回
files=$(find "${ends[@]}" -type f \( -name '*.cs' -o -name '*.csproj' -o -name '*.xaml' \
  -o -name '*.kt' -o -name '*.kts' -o -name '*.py' \)) ||
  { echo "::error::find 失败"; exit 2; }
while IFS= read -r f; do
  [ -n "$f" ] || continue
  end=${f%%/*}
  others=""
  for e in "${ends[@]}"; do [ "$e" = "$end" ] || others+="${others:+|}$e"; done
  # 命中：跨端路径（含 ../、反斜杠）或 Python 导入；grep 退出码 1 为无匹配，>=2 为错误
  if hits=$(grep -niE "(^|[^[:alnum:]_.-])(${others})[/\\]|^[[:space:]]*(from|import)[[:space:]]+(${others})([.[:space:]]|$)" "$f"); then
    while IFS=: read -r n _; do
      echo "::error::$f:$n 跨端引用（A11）"
      rc=1
    done <<< "$hits"
  else
    s=$?
    [ "$s" -eq 1 ] || { echo "::error::$f grep 失败（退出码 $s）"; rc=2; }
  fi
done <<< "$files"
exit "$rc"
