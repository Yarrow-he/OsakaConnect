# 测试索引

| 测试 | 覆盖验收 | 端 | 命令 | 最后通过 |
|---|---|---|---|---|
| tests/contract/test_contract.py | A9（契约） | 共用 | `pytest tests/contract` | 2026-11-07 main @9de8988 run 42257087460 contract success |
| tests/server/server-core/test_server_core.py | A8、A9（后台）、A15（后台）、A17（后台） | 服务端 | `OSAKA_SERVER_CMD="python3 -m osaka_server.app" OSAKA_INVITE_FN=osaka_server.invite.is_valid_invite_code PYTHONPATH=server pytest tests/server/server-core -v -rs` | 2026-11-07 main @9de8988 run 42257087460 python job：server-core **23 passed / 0 skipped**（`-rs` 无 skip）；同 job `python -m pytest -q` 另 **191 passed / 0 skipped** |
| tests/windows/win-shell/smoke.ps1 | A1、A2、A16、A14、A17 | Windows | 见文件头，windows-latest | 2026-11-07 main @9de8988 run 42257087460 windows-acceptance success |
| tests/windows/win-diagnostics/*.cs（WinDiagnostics.Tests.csproj） | A3、A4、A15 | Windows | `dotnet test`，windows-latest | 2026-11-07 main @9de8988 run 42257087460 windows-dotnet / windows-acceptance success |
| tests/admin/admin-connect/{gen-certs,run-a10}.sh + docker-compose.yml | A10 ①②③④ | 管理端 | `bash tests/admin/admin-connect/run-a10.sh`，ubuntu + docker | 2026-11-07 main @9de8988 run 42257087460 a10-docker success |
| tests/admin/admin-connect/grep-guard.sh | A10（禁止放行回调）、A14 | 管理端 | `bash tests/admin/admin-connect/grep-guard.sh` | 2026-11-07 main @9de8988 run 42257087460 guards-acceptance success |
| scripts/ci/check-cross-refs.sh + test-check-cross-refs.sh | A11 | 共用 | `bash scripts/ci/check-cross-refs.sh`；`bash scripts/ci/test-check-cross-refs.sh` | 2026-11-07 main @9de8988 run 42257087460 guards-acceptance success（#82 已合） |
| tests/android/android-shell/androidTest + ci-run | A5、A6、A16 | Android | 见 workflow android / android-emulator | 2026-11-07 main @9de8988 run 42257087460 android + android-emulator success |
| tests/android/android-shell/lang-guard.sh | A14、A17 | Android | `bash tests/android/android-shell/lang-guard.sh` | 2026-11-07 main @9de8988 run 42257087460 guards-acceptance success |
| tests/android/android-diagnostics unit + instrumented | A7、A15 | Android | workflow 内 gradle / emulator | 2026-11-07 main @9de8988 run 42257087460：产品 `android/.../diagnostics` 已在；android-emulator `OSAKA_DIAG=yes`，`run.sh`+`run-instrumented.sh` 均 BUILD SUCCESSFUL；connectedDebugAndroidTest Finished 4；android job 单测 XML LogRedactorTest 3/0、InviteCodeTest 1/0（A7/A15） |
| tests/ci/pr-line-check/{pr_line_check.py,test_pr_line_check.py} | A12（PR 行数统计，提案侧） | 共用 | `python3 tests/ci/pr-line-check/test_pr_line_check.py`；线上由策略仓 `pr-size` 必过 | 策略仓 pr-size 在 PR 上跑；业务仓脚本仍作本地回归（本 run 不覆盖此行） |

A13（main 全绿）：[run 42257087460](https://github.com/Yarrow-he/OsakaConnect/actions/runs/42257087460) @ `9de8988`（2026-11-07）conclusion=success，8/8 job 全绿。

补充：A14 的 server/ 只有 Python 检查和 docs 一致性，没有别的覆盖。

刷新说明：测试 · Osaka 2026-11-07 Australia/Brisbane 按 main@9de8988 CI run 42257087460 实况更新「最后通过」；A7/A15（android-diagnostics）由 #104 进 main 后本 run 实跑通过；A8 server-core skip 已核清为 0；pr-line-check 行未改（本 run 不证明）。
