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
merging=0
if [ "${range[0]}" = "--cached" ] && git rev-parse -q --verify MERGE_HEAD >/dev/null; then merging=1; fi
MERGING="$merging" DELETED="$deleted" FILES="$files" BRANCH="$branch" python3 - <<'PY'
import json, os, sys
b = json.load(open("board.json"))
sc = b["scope"]; files = os.environ["FILES"].split("\n"); br = os.environ["BRANCH"]
mod = next((m for m in b["modules"] if br.endswith("/" + m["id"])), None)
dele = set(os.environ["DELETED"].split("\n")) - {""}
# 合并进行中（仅 --staged 且存在 MERGE_HEAD）：本地只拦受保护配置，scope 由 CI check-merge-scope 校验（决策 007）
merging = os.environ["MERGING"] == "1"
bad = []
for f in files:
    if merging and not any(f.startswith(p) for p in sc["protected"]): continue
    # 删除 scope 外的旧路径允许（如移除废弃目录），受保护配置的删除仍拦截
    if f in dele and not any(f.startswith(p) for p in sc["protected"]): continue
    skill = f.startswith((".codex/skills/", ".claude/skills/"))
    prot = any(f.startswith(p) for p in sc["protected"])
    if prot and br.startswith("lead/setup-"): continue
    if not skill and any(f.startswith(p) for p in sc["protected"]) and not br.startswith("lead/setup-"): bad.append(f + "（受保护配置）")
    elif not skill and not any(f == p or f.startswith(p) for p in sc["global"]): bad.append(f + "（不在 scope）")
    elif mod and not any(f.startswith(p) for p in mod["paths"] + ["notes/", "board.json", "reports/", "memory/", "tests/INDEX.md"]):
        bad.append(f + f"（超出模块 {mod['id']}）")
if bad:
    print("check-scope 失败：\n  " + "\n  ".join(bad)); sys.exit(1)
PY
# rt8
