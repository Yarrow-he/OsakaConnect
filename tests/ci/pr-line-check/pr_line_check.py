#!/usr/bin/env python3
# 用途：统计 PR 中产品+界面代码的改动行数（added+deleted），超过 200 行则检查失败。
# 前置条件：Python 3.8+，git 在 PATH；在 git 仓库内运行；base ref 可解析（默认 origin/main）。
# 运行：python3 tests/ci/pr-line-check/pr_line_check.py [base]
#   1. 统计 `git diff --numstat <base>...HEAD`。
#   2. 排除记账、文档、测试、锁、生成、测试数据和二进制文件。
#   3. PR_LABELS 含 scaffold 或 PR_TITLE 以“脚手架”/“scaffold”开头时，构建配置不计入。
#   4. 计入行数 >200 时打印明细并以 1 退出，否则打印总数并以 0 退出。
import fnmatch
import os
import subprocess
import sys

LIMIT = 200

# 路径段（目录名）排除
EXCLUDED_DIRS = {
    "notes", "memory", "reports", "decisions", "logs",
    "build", "bin", "obj", "generated",
    "testdata", "fixtures",
}
LOCK_NAMES = {"package-lock.json", "gradle.lockfile", "packages.lock.json"}
GENERATED_GLOBS = ("*.g.cs", "*.min.js")
TEST_GLOBS = ("*_test.*", "*.test.*")
BUILD_GLOBS = (
    "*.gradle", "*.gradle.kts", "*.csproj", "*.sln", "*.props", "*.targets",
    "requirements*.txt", "settings.gradle*",
)
BUILD_NAMES = {"pyproject.toml", "setup.cfg", "package.json", "gradle.properties"}


def is_excluded(path):
    p = path.lower()
    parts = p.split("/")
    name = parts[-1]

    if p.endswith(".md"):  # 所有文档，含 skills/INDEX.md
        return True
    if path == "board.json":
        return True
    if any(part in EXCLUDED_DIRS for part in parts[:-1]):
        return True
    # 测试：目录 tests/ 或路径含 Tests，或 src/test/、androidTest/
    if "tests" in parts[:-1] or "Tests" in path:
        return True
    if "src/test/" in path or "androidTest/" in path:
        return True
    if any(fnmatch.fnmatch(name, glob) for glob in TEST_GLOBS):
        return True
    # 锁文件
    if name in LOCK_NAMES or fnmatch.fnmatch(name, "*.lock"):
        return True
    # 生成文件
    if any(fnmatch.fnmatch(name, glob) for glob in GENERATED_GLOBS):
        return True
    return False


def is_build_config(path):
    name = path.rsplit("/", 1)[-1].lower()
    return name in BUILD_NAMES or any(fnmatch.fnmatch(name, g) for g in BUILD_GLOBS)


def scaffold_exempt():
    labels = os.environ.get("PR_LABELS", "")
    if "scaffold" in {label.strip().lower() for label in labels.split(",")}:
        return True
    title = os.environ.get("PR_TITLE", "").lower()
    return title.startswith("脚手架") or title.startswith("scaffold")


def main():
    base = sys.argv[1] if len(sys.argv) > 1 else "origin/main"
    try:
        out = subprocess.run(
            ["git", "diff", "--numstat", f"{base}...HEAD"],
            check=True, capture_output=True, text=True,
        ).stdout
    except subprocess.CalledProcessError as exc:
        sys.stderr.write(exc.stderr)
        return 2

    exempt = scaffold_exempt()
    counted = {}
    for line in out.splitlines():
        added, deleted, path = line.split("\t", 2)
        if added == "-" or deleted == "-":  # 二进制
            continue
        if is_excluded(path):
            continue
        if exempt and is_build_config(path):
            continue
        counted[path] = counted.get(path, 0) + int(added) + int(deleted)

    total = sum(counted.values())
    if total > LIMIT:
        print(f"计入行数：{total}（上限 {LIMIT}）")
        for path, lines in sorted(counted.items(), key=lambda kv: -kv[1]):
            print(f"  {lines}\t{path}")
        return 1
    print(f"计入行数：{total}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
