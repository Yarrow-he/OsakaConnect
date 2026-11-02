#!/usr/bin/env bash
# 用途：check-scope.sh 合并规则自测（临时仓库，不联网、不碰真实仓库）。
# 运行：bash scripts/test-check-scope-merge.sh
set -euo pipefail
SRC=$(cd "$(dirname "$0")/.." && pwd)
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT
cd "$T" && git init -q -b main && git config user.email t@t && git config user.name t
mkdir -p scripts .githooks server tests && cp "$SRC/scripts/check-scope.sh" scripts/ && cp "$SRC/.githooks/"* .githooks/
cat > board.json <<'J'
{"scope":{"global":["server/","tests/","scripts/",".githooks/","board.json","reports/"],"protected":[".codex/"]},"modules":[{"id":"server-core","paths":["server/"]}]}
J
mkdir -p reports; echo a > tests/t.txt; echo 1 > reports/r.md; printf 'x\ny\nz\n' > tests/u.txt
git add -A && git commit -qm base && git config core.hooksPath .githooks
git checkout -qb server/server-core && echo s > server/a.py && echo b >> reports/r.md && git add -A && git commit -qm mod
git checkout -q main && echo main >> tests/t.txt && echo c >> reports/r.md && git add -A && git commit -qm main --no-verify
git checkout -q server/server-core
fail=0
# 1. 冲突合并，tests/t.txt 原样取自 main → 提交应通过
git merge -q --no-ff --no-edit main >/dev/null 2>&1 || true
printf '1\nb\nc\n' > reports/r.md && git add reports/r.md
if git commit -q --no-edit; then echo "通过 1：原样取自 main 的模块外文件放行"; else echo "失败 1"; fail=1; fi
# 2. 合并时改动模块外文件（与 main 不同）→ 应拦截
git checkout -q main && echo m2 >> tests/t.txt && git commit -qam m2 --no-verify && git checkout -q server/server-core
git merge -q --no-ff --no-commit main >/dev/null 2>&1 || true
echo tamper >> tests/t.txt && git add tests/t.txt
if git commit -q --no-edit 2>/dev/null; then echo "失败 2：被改动的模块外文件未拦截"; fail=1; else echo "通过 2：改动过的模块外文件被拦截"; fi
git merge --abort
# 3. 无冲突自动合并时 pre-merge-commit 钩子生效：先在本分支越界暂存（模拟），用 --no-commit 后由钩子检查
git merge -q --no-ff --no-edit main && echo "通过 3：干净自动合并经 pre-merge-commit 放行" || { echo "失败 3"; fail=1; }
# 4. 无冲突自动合并、结果与 main 不同的模块外文件 → pre-merge-commit 应拦截（证明钩子确实执行）
git checkout -q main && printf 'x\ny\nZ\n' > tests/u.txt && git commit -qam u2 --no-verify
git checkout -q server/server-core && printf 'X\ny\nz\n' > tests/u.txt && git commit -qam u3 --no-verify
out=$(git merge --no-ff --no-edit main 2>&1) && r=0 || r=1
if [ $r = 0 ] || ! grep -q "tests/u.txt（超出模块" <<<"$out" || grep -q CONFLICT <<<"$out"; then echo "失败 4：自动合并改出的模块外内容未拦截"; fail=1; else echo "通过 4：pre-merge-commit 拦截自动合并改出的模块外内容"; git merge --abort 2>/dev/null || true; fi
# 5. 受保护配置即使原样取自 main 也拦截（main 的 u.txt 先对齐，确保无冲突且只剩 .codex/c 一处违例）
git checkout -q main && printf 'X\ny\nZ\n' > tests/u.txt && mkdir -p .codex && echo k > .codex/c && git add -A && git commit -qm p --no-verify && git checkout -q server/server-core
out=$(git merge --no-ff --no-edit main 2>&1) && r=0 || r=1
if [ $r = 0 ] || ! grep -q ".codex/c（受保护配置）" <<<"$out" || grep -q CONFLICT <<<"$out" || grep -q "tests/u.txt（" <<<"$out"; then echo "失败 5：$out"; fail=1; else echo "通过 5：受保护配置不豁免"; fi
exit $fail
