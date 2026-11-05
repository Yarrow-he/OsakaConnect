#!/usr/bin/env bash
# A11：各端源码不得引用其他端目录；shared/、openapi/ 允许。用法：check-cross-refs.sh [仓库根目录]
set -euo pipefail
cd "${1:-.}"
ends=(windows admin android server)
rc=0
while IFS= read -r f; do
  end=${f%%/*}
  others=""
  for e in "${ends[@]}"; do [ "$e" = "$end" ] || others+="${others:+|}$e"; done
  # 命中：跨端路径（含 ../、反斜杠）或 Python 导入
  while IFS=: read -r n _; do
    echo "::error::$f:$n 跨端引用（A11）"
    rc=1
  done < <(grep -nE "(^|[^[:alnum:]_.-])(${others})[/\\]|^[[:space:]]*(from|import)[[:space:]]+(${others})([.[:space:]]|$)" "$f")
done < <(find "${ends[@]}" -type f \( -name '*.cs' -o -name '*.csproj' -o -name '*.xaml' \
  -o -name '*.kt' -o -name '*.kts' -o -name '*.py' \) 2>/dev/null)
exit "$rc"
