#!/usr/bin/env bash
# 用途：A14 技术栈检查（windows/admin 只出现 C#/.NET 源码，android 只出现 Kotlin/KTS，
#       server 只出现 Python；资源与配置文件忽略；docs/architecture.md 技术栈一致），
#       以及 A10 源码 grep：不得出现 ServerCertificateCustomValidationCallback 等
#       证书校验放行回调（任一此类回调都视为违例，返回值 true 自然包含在内）。
# 前置环境：bash、find、grep；产品代码已 checkout（GitHub Actions ubuntu-latest；待 CI）。
# 运行命令：bash tests/admin/admin-connect/grep-guard.sh
# 覆盖 acceptance：A10（无跳过证书校验的回调）、A14（技术栈锁定）。
set -euo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)
VIOLATIONS=0

violation() { echo "违例：$*"; VIOLATIONS=$((VIOLATIONS + 1)); }

present=0
for d in windows admin android server; do [ -d "$ROOT/$d" ] && present=1; done
if [ "$present" -eq 0 ]; then
  echo "待测：模块未交付（windows/ admin/ android/ server/ 均不存在）"
  exit 0
fi

# A14 按“生产源码扩展名”判定；json/xml/png/gradle.properties 等资源与配置不在此列。
is_source_file() {
  local ext
  ext=$(printf '%s' "${1##*.}" | tr '[:upper:]' '[:lower:]')
  case "$ext" in
    cs|csproj|xaml|fs|fsx|vb|py|pyw|java|kt|kts|scala|groovy|gradle|js|jsx|mjs|cjs|ts|tsx|\
    c|h|cc|cpp|cxx|hpp|rs|go|rb|php|swift|m|mm|dart|lua|pl|r|jl|ex|exs|erl|hs|clj|sh|bash)
      return 0 ;;
    *) return 1 ;;
  esac
}

check_lang() { # $1=目录 $2=允许的源码扩展名（|ext|ext|） $3=端名
  local dir="$1" allowed="$2" label="$3" f ext
  if [ ! -d "$dir" ]; then
    echo "待测：模块未交付（$label 目录不存在）"
    return
  fi
  if [ -z "$(find "$dir" -type f -print -quit)" ]; then
    echo "待测：模块未交付（$label 目录为空）"
    return
  fi
  while IFS= read -r f; do
    is_source_file "$f" || continue
    ext=$(printf '%s' "${f##*.}" | tr '[:upper:]' '[:lower:]')
    case "$allowed" in
      *"|$ext|"*) ;;
      *) violation "${f#"$ROOT"/}（$label 不允许 .$ext 源码）" ;;
    esac
  done < <(find "$dir" -type f | sort)
}

check_lang "$ROOT/windows" "|cs|csproj|xaml|" "Windows"
check_lang "$ROOT/admin" "|cs|csproj|xaml|" "管理端"
check_lang "$ROOT/android" "|kt|kts|" "Android"
check_lang "$ROOT/server" "|py|" "后台"

ARCH="$ROOT/docs/architecture.md"
if [ -f "$ARCH" ]; then
  section=$(awk '/^## 技术栈/{flag=1;next}/^## /{flag=0}flag' "$ARCH")
  for word in ".NET" "Kotlin" "Python"; do
    printf '%s' "$section" | grep -Fq "$word" || violation "docs/architecture.md 技术栈一节缺少 $word"
  done
fi

CALLBACK_PATTERN='ServerCertificateCustomValidationCallback|ServerCertificateValidationCallback|DangerousAcceptAnyServerCertificateValidator|RemoteCertificateValidationCallback'
check_callbacks() {
  local dirs=() d hits
  for d in windows admin android server; do
    [ -d "$ROOT/$d" ] && dirs+=("$ROOT/$d")
  done
  [ ${#dirs[@]} -eq 0 ] && return 0
  hits=$(grep -RInE \
    --exclude-dir=bin --exclude-dir=obj --exclude-dir=build --exclude-dir=.gradle --exclude-dir=__pycache__ \
    "$CALLBACK_PATTERN" "${dirs[@]}" 2>/dev/null || true)
  [ -n "$hits" ] || return 0
  while IFS= read -r line; do
    violation "证书校验放行回调：${line#"$ROOT"/}"
  done <<< "$hits"
}
check_callbacks

if [ "$VIOLATIONS" -ne 0 ]; then
  echo "grep-guard 失败：$VIOLATIONS 项违例（A10/A14）" >&2
  exit 1
fi
echo "grep-guard 通过（A10 无证书放行回调；A14 技术栈与 docs 一致）"
