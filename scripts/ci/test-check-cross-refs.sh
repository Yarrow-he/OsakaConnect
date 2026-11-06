#!/usr/bin/env bash
# check-cross-refs.sh（A11）回归测试。用法：bash scripts/ci/test-check-cross-refs.sh
set -euo pipefail
here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
repo=$(cd "$here/../.." && pwd)
check="$repo/scripts/ci/check-cross-refs.sh"
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
fail=0
rc=0

run() { # run <样例根>：退出码写进 rc，输出写进 $tmp/out
  rc=0
  bash "$check" "$1" >"$tmp/out" 2>&1 || rc=$?
}

report() { # report <名称> <期望> <实际>
  echo "FAIL $1（期望 $2，实际 $3）"
  sed 's/^/  /' "$tmp/out"
  fail=1
}

expect() { # expect <名称> <样例根> <期望退出码|nz>
  local name=$1 root=$2 want=$3
  run "$root"
  if [ "$want" = nz ]; then
    if [ "$rc" -ne 0 ]; then echo "PASS $name"; else report "$name" "非0" "$rc"; fi
  elif [ "$rc" -eq "$want" ]; then
    echo "PASS $name"
  else
    report "$name" "$want" "$rc"
  fi
}

new_root() { # new_root <目录>：四端目录齐全的空样例根
  mkdir -p "$1"/{windows,admin,android,server}
}

expect "a) 当前仓库 exit 0" "$repo" 0

new_root "$tmp/violation"
printf 'class A { string p = "../admin/Bar.cs"; }\n' >"$tmp/violation/windows/A.cs"
expect "b) 跨端引用 exit 1" "$tmp/violation" 1

new_root "$tmp/allowed"
printf 'val p = "shared/api.md"\nval q = "../openapi/admin.v1.yaml"\n' >"$tmp/allowed/android/A.kt"
expect "c) shared/openapi 不误报" "$tmp/allowed" 0

new_root "$tmp/case"
printf '%s\n' 'class C { string p = "..\Server\x"; }' >"$tmp/case/windows/A.cs"
expect "f) 大小写变体 exit 1" "$tmp/case" 1

new_root "$tmp/missing"
rmdir "$tmp/missing/server"
expect "d) 缺端目录非 0" "$tmp/missing" nz

new_root "$tmp/unreadable"
printf 'class B {}\n' >"$tmp/unreadable/server/B.cs"
chmod 000 "$tmp/unreadable/server/B.cs"
if [ "$(id -u)" -eq 0 ]; then
  echo "SKIP e) 不可读文件（root 无法构造）"
else
  expect "e) 不可读文件非 0" "$tmp/unreadable" nz
fi

exit "$fail"
