# 用途：决策 008 自测。guards 只放流程检查；grep-guard、lang-guard 必须仍在 guards-acceptance 里执行，
#       且两个脚本在无产品代码时仍 exit 3（不被改成通过）。运行：python3 scripts/test-ci-guards-split.py
import pathlib, re, sys
root = pathlib.Path(__file__).resolve().parent.parent
# 不依赖 pyyaml：按两格缩进的任务名切分 jobs 段
text = (root / ".github/workflows/ci.yml").read_text().split("\njobs:\n", 1)[1]
jobs = dict(re.findall(r"^  ([\w-]+):\n((?:(?:    .*|\s*#.*|)\n)*)", text, re.M))
run = lambda j: jobs.get(j, "")
fail = []
for script in ("tests/admin/admin-connect/grep-guard.sh", "tests/android/android-shell/lang-guard.sh"):
    if script not in run("guards-acceptance"): fail.append(f"{script} 不在 guards-acceptance")
    if script in run("guards"): fail.append(f"{script} 仍在 guards")
    if re.search(r"continue-on-error|^\s+if:", run("guards-acceptance"), re.M): fail.append("guards-acceptance 含可跳过/可失败设置")
    if not re.search(r"exit 3", (root / script).read_text()): fail.append(f"{script} 无产品时不再 exit 3")
if "check-scope.sh" not in run("guards"): fail.append("scope 检查不在 guards")
print("\n".join(fail) or "通过：guards 拆分自测")
sys.exit(1 if fail else 0)
