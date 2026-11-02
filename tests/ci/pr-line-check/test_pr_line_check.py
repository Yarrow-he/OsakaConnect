#!/usr/bin/env python3
# 用途：pr_line_check.py 自测（临时 git 仓库，不联网）。
# 前置条件：Python 3.8+，git 在 PATH；从仓库内运行。
# 运行：python3 tests/ci/pr-line-check/test_pr_line_check.py
#   1. 未超 200 行时通过。
#   2. 超 200 行时失败并列出明细。
#   3. 被排除文件（记账、测试、锁、生成、二进制）不计入。
#   4. 仅 PR_LABELS 的 scaffold 标签豁免构建配置，仅设置 PR_TITLE 不豁免。
#   5. 文件名含 Tests 不再排除，目录段 Tests、*.Tests 仍排除。
import os
import pathlib
import subprocess
import sys
import tempfile
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent / "pr_line_check.py"


def git(cwd, *args):
    return subprocess.run(
        ["git", *args], cwd=cwd, check=True, capture_output=True, text=True
    )


class PrLineCheckTest(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self._tmp.cleanup)
        self.repo = pathlib.Path(self._tmp.name)
        git(self.repo, "init", "-q", "-b", "main")
        git(self.repo, "config", "user.email", "test@example.com")
        git(self.repo, "config", "user.name", "test")
        (self.repo / "seed.txt").write_text("seed\n")
        git(self.repo, "add", "-A")
        git(self.repo, "commit", "-qm", "base")
        git(self.repo, "branch", "base")

    def write(self, rel, text):
        path = self.repo / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)

    def commit(self):
        git(self.repo, "add", "-A")
        git(self.repo, "commit", "-qm", "change")

    def run_check(self, base="base", **env):
        environ = {k: v for k, v in os.environ.items() if k not in ("PR_LABELS", "PR_TITLE")}
        environ.update({k: str(v) for k, v in env.items()})
        return subprocess.run(
            [sys.executable, str(SCRIPT), base],
            cwd=self.repo,
            capture_output=True,
            text=True,
            env=environ,
        )

    @staticmethod
    def lines(n):
        return "".join(f"line {i}\n" for i in range(n))

    def test_under_limit_passes(self):
        self.write("server/app.py", self.lines(150))
        self.commit()
        result = self.run_check()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("计入行数：150", result.stdout)

    def test_over_limit_fails_with_detail(self):
        self.write("server/app.py", self.lines(201))
        self.commit()
        result = self.run_check()
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        self.assertIn("server/app.py", result.stdout)
        self.assertIn("计入行数：201", result.stdout)

    def test_excluded_files_not_counted(self):
        self.write("board.json", self.lines(300))
        self.write("notes/x.md", self.lines(300))
        self.write("skills/INDEX.md", self.lines(300))
        self.write("tests/ci/x.py", self.lines(300))
        self.write("server/Foo.Tests/x.cs", self.lines(300))
        self.write("server/thing_test.py", self.lines(300))
        self.write("shared/test-vectors/x.json", self.lines(300))
        self.write("server/package-lock.json", self.lines(300))
        self.write("server/obj/gen.cs", self.lines(300))
        (self.repo / "server" / "blob.bin").write_bytes(b"\x00" * 64)
        self.commit()
        result = self.run_check()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("计入行数：0", result.stdout)

    def test_scaffold_label_exempts_build_config(self):
        self.write("android/app/build.gradle.kts", self.lines(300))
        self.commit()
        blocked = self.run_check()
        self.assertEqual(blocked.returncode, 1, blocked.stdout + blocked.stderr)
        allowed = self.run_check(PR_LABELS="scaffold,ci")
        self.assertEqual(allowed.returncode, 0, allowed.stdout + allowed.stderr)
        self.assertIn("计入行数：0", allowed.stdout)

    def test_scaffold_title_does_not_exempt_build_config(self):
        self.write("server/pyproject.toml", self.lines(300))
        self.commit()
        result = self.run_check(PR_TITLE="scaffold service")
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        self.assertIn("计入行数：300", result.stdout)

    def test_tests_filename_not_excluded(self):
        self.write("server/App.Tests.cs", self.lines(10))
        self.commit()
        result = self.run_check()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("计入行数：10", result.stdout)


if __name__ == "__main__":
    unittest.main()
