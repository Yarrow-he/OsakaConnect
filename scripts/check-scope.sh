#!/usr/bin/env bash
# 按 board.json scope 检查改动范围。用法：check-scope.sh [--staged | <base-ref>]
# 受保护配置只允许在 lead/setup-* 分支（须 Hao Yin 批 PR）改；skills 目录例外。
# 模块分支（<端>/<模块>）还要求改动落在该模块 paths 内。
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
if [ "${1:-}" = "--staged" ] || [ -z "${1:-}" ]; then files=$(git -c core.quotepath=false diff --cached --name-only)
else files=$(git -c core.quotepath=false diff --name-only "$1"...HEAD); fi
[ -z "$files" ] && exit 0
branch=$(git rev-parse --abbrev-ref HEAD)
FILES="$files" BRANCH="$branch" python3 - <<'PY'
import json, os, sys
b = json.load(open("board.json"))
sc = b["scope"]; files = os.environ["FILES"].split("\n"); br = os.environ["BRANCH"]
mod = next((m for m in b["modules"] if br.endswith("/" + m["id"])), None)
bad = []
for f in files:
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
