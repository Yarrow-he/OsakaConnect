#!/usr/bin/env bash
# 按 board.json scope 检查改动范围。用法：check-scope.sh [--staged | <base-ref>]
# 受保护配置只允许在 lead/setup-* 分支（须 Hao Yin 批 PR）改；skills 目录例外。
# 模块分支（<端>/<模块>）还要求改动落在该模块 paths 内。
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
if [ "${1:-}" = "--staged" ] || [ -z "${1:-}" ]; then range=(--cached); else range=("$1"...HEAD); fi
files=$(git -c core.quotepath=false diff --name-only "${range[@]}")
deleted=$(git -c core.quotepath=false diff --name-only --diff-filter=D "${range[@]}")
[ -z "$files" ] && exit 0
branch=$(git rev-parse --abbrev-ref HEAD)
# 合并进行中：暂存内容与对方分支（MERGE_HEAD）完全相同的文件
# 视为原样取自对方分支（如 main），不再按本分支 scope 检查；其余文件照常检查。
frommerge=""
# 自动合并时 git 尚未写 MERGE_HEAD，由 pre-merge-commit 钩子经 CHECK_SCOPE_MERGE_REF 传入对方分支。
# CHECK_SCOPE_MERGE_REF 只在 git 合并流程中（git 目录有 AUTO_MERGE）才采信，平时手设无效。
mref=$(git rev-parse -q --verify MERGE_HEAD 2>/dev/null || true)
if [ -z "$mref" ] && [ -n "${CHECK_SCOPE_MERGE_REF:-}" ] && [ -f "$(git rev-parse --git-path AUTO_MERGE)" ]; then
  mref=$(git rev-parse -q --verify "$CHECK_SCOPE_MERGE_REF^{commit}" 2>/dev/null || true)
fi
if [ "${range[0]}" = "--cached" ] && [ -n "$mref" ]; then
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    # 比较 模式+对象（含可执行位）；双方都不存在也算相同
    a=$(git ls-files -s -- "$f" | awk '{print $1, $2}')
    m=$(git ls-tree "$mref" -- "$f" | awk '{print $1, $3}')
    [ "$a" = "$m" ] && frommerge+="$f"$'\n'
  done <<< "$files"
fi
FROMMERGE="$frommerge" DELETED="$deleted" FILES="$files" BRANCH="$branch" python3 - <<'PY'
import json, os, sys
b = json.load(open("board.json"))
sc = b["scope"]; files = os.environ["FILES"].split("\n"); br = os.environ["BRANCH"]
mod = next((m for m in b["modules"] if br.endswith("/" + m["id"])), None)
dele = set(os.environ["DELETED"].split("\n")) - {""}
frommerge = set(os.environ["FROMMERGE"].split("\n")) - {""}
bad = []
for f in files:
    # 删除 scope 外的旧路径允许（如移除废弃目录），受保护配置的删除仍拦截
    if f in dele and not any(f.startswith(p) for p in sc["protected"]): continue
    skill = f.startswith((".codex/skills/", ".claude/skills/"))
    prot = any(f.startswith(p) for p in sc["protected"])
    # 原样取自对方分支的文件跳过 scope 检查；受保护配置不豁免
    if f in frommerge and not prot: continue
    if prot and br.startswith("lead/setup-"): continue
    if not skill and any(f.startswith(p) for p in sc["protected"]) and not br.startswith("lead/setup-"): bad.append(f + "（受保护配置）")
    elif not skill and not any(f == p or f.startswith(p) for p in sc["global"]): bad.append(f + "（不在 scope）")
    elif mod and not any(f.startswith(p) for p in mod["paths"] + ["notes/", "board.json", "reports/", "memory/", "tests/INDEX.md"]):
        bad.append(f + f"（超出模块 {mod['id']}）")
if bad:
    print("check-scope 失败：\n  " + "\n  ".join(bad)); sys.exit(1)
PY
