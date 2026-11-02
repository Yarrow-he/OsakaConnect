#!/usr/bin/env bash
# 用途：校验模块分支上的合并提交（决策 007，B 方案）。对 <base>..<tip> 里每个合并提交 M，
#       取 M 与 M^2（合入的一侧，通常是 main）不同的文件，必须落在本模块 scope 内；受保护配置一律拦截。
# 用法：CHECK_BRANCH=<分支名> scripts/check-merge-scope.sh <base-ref> [<tip>]
# 非模块分支（lead/、tests/、main 等）直接通过。
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
base=$1; tip=${2:-HEAD}; branch=${CHECK_BRANCH:?需要 CHECK_BRANCH}
merges=$(git rev-list --merges "$base..$tip")
[ -z "$merges" ] && { echo "check-merge-scope：$base..$tip 内无合并提交"; exit 0; }
bad=0
for m in $merges; do
  files=$(git -c core.quotepath=false diff --name-only "$m^2" "$m")
  [ -z "$files" ] && continue
  out=$(git show "$m:board.json" | BRANCH="$branch" FILES="$files" python3 -c '
import json, os, sys
b = json.load(sys.stdin); sc = b["scope"]; br = os.environ["BRANCH"]
mod = next((x for x in b["modules"] if br.endswith("/" + x["id"])), None)
if mod is None: sys.exit(0)
extra = ["notes/", "board.json", "reports/", "memory/", "tests/INDEX.md"]
for f in os.environ["FILES"].split("\n"):
    if any(f.startswith(p) for p in sc["protected"]): print(f + "（受保护配置）")
    elif not any(f.startswith(p) for p in mod["paths"] + extra): print(f + "（超出模块 " + mod["id"] + "）")
') || { echo "check-merge-scope：读取 $m 的 board.json 失败"; exit 1; }
  if [ -n "$out" ]; then echo "合并提交 $(git rev-parse --short "$m") 与合入侧不同的越界文件："; sed 's/^/  /' <<<"$out"; bad=1; fi
done
[ $bad = 0 ] && echo "check-merge-scope：通过（$(wc -w <<<"$merges") 个合并提交）"
exit $bad
