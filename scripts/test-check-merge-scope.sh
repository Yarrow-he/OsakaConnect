#!/usr/bin/env bash
# 用途：check-merge-scope.sh 与 check-scope.sh 合并行为自测（临时仓库，不联网）。
# 运行：bash scripts/test-check-merge-scope.sh
set -uo pipefail
SRC=$(cd "$(dirname "$0")/.." && pwd); T=$(mktemp -d); trap 'rm -rf "$T"' EXIT
cd "$T" && git init -q -b main && git config user.email t@t && git config user.name t
mkdir -p scripts server tests reports && cp "$SRC/scripts/check-merge-scope.sh" "$SRC/scripts/check-scope.sh" scripts/
echo '{"scope":{"global":["server/","tests/","scripts/","board.json","reports/",".codex/"],"protected":[".codex/"]},"modules":[{"id":"server-core","paths":["server/"]}]}' > board.json
printf 'x\ny\nz\n' > tests/u.txt; echo 1 > reports/r.md; git add -A; git commit -qm base
fail=0; ok() { echo "通过 $1"; }; ng() { echo "失败 $1"; fail=1; }
cms() { CHECK_BRANCH=server/server-core scripts/check-merge-scope.sh main HEAD >/dev/null 2>&1; }
git checkout -qb server/server-core; echo s > server/a.py; echo b >> reports/r.md; git add -A; git commit -qm mod
git checkout -q main; printf 'x\ny\nZ\n' > tests/u.txt; echo c >> reports/r.md; git add -A; git commit -qm m1
git checkout -q server/server-core
# 1. 冲突合并，模块外文件原样取自 main：本地 check-scope 放行，CI 放行
git merge -q --no-edit main >/dev/null 2>&1; printf '1\nb\nc\n' > reports/r.md; git add -A
scripts/check-scope.sh --staged >/dev/null && ok "1a 本地合并放行" || ng "1a"
git commit -qm merge1 --no-verify; cms && ok "1b CI 原样合并放行" || ng "1b"
# 2. 合并时改了模块外文件 → CI 拦截
git checkout -q main; echo m2 >> reports/r.md; git commit -qam m2; git checkout -q server/server-core
git merge -q --no-commit main >/dev/null 2>&1; echo tamper >> tests/u.txt; git add -A; git commit -qm merge2 --no-verify
cms && ng "2 CI 未拦截合并夹带" || ok "2 CI 拦截合并夹带的模块外改动"
git reset -q --hard HEAD~1
# 3. 合并时夹带受保护配置 → 本地与 CI 都拦截
git merge -q --no-commit main >/dev/null 2>&1; mkdir -p .codex; echo k > .codex/c; git add -A
scripts/check-scope.sh --staged >/dev/null && ng "3a 本地未拦受保护配置" || ok "3a 本地拦受保护配置"
git commit -qm merge3 --no-verify; cms && ng "3b" || ok "3b CI 拦受保护配置"
git reset -q --hard HEAD~1
# 4. 非合并提交照旧：模块外改动本地拦截
echo t >> tests/u.txt; git add -A
scripts/check-scope.sh --staged >/dev/null && ng "4 非合并提交被放宽" || ok "4 非合并提交照旧拦截"
git reset -q --hard
# 5. 非模块分支直接通过
git checkout -qb lead/x; CHECK_BRANCH=lead/x scripts/check-merge-scope.sh main HEAD >/dev/null && ok "5 非模块分支通过" || ng "5"
exit $fail
