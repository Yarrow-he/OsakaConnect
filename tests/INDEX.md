# 测试索引

| 测试 | 覆盖验收 | 端 | 命令 | 最后通过 |
|---|---|---|---|---|
| tests/contract/test_contract.py | A9（契约） | 共用 | `pytest tests/contract` | 2026-11-06 main @674017f run 42202140900 contract success |
| tests/server/server-core/test_server_core.py | A8、A9（后台）、A15（后台）、A17（后台） | 服务端 | `OSAKA_SERVER_CMD="python3 -m osaka_server.app" OSAKA_INVITE_FN=osaka_server.invite.is_valid_invite_code PYTHONPATH=server pytest tests/server/server-core -q -rs` | 2026-11-06 main @674017f run 42202140900 python success |
| tests/windows/win-shell/smoke.ps1 | A1、A2、A16、A14、A17 | Windows | 见文件头，windows-latest | 2026-11-06 main @674017f run 42202140900 windows-acceptance success |
| tests/windows/win-diagnostics/*.cs（WinDiagnostics.Tests.csproj） | A3、A4、A15 | Windows | `dotnet test`，windows-latest | 2026-11-06 main @674017f run 42202140900 windows-dotnet / windows-acceptance success |
| tests/admin/admin-connect/{gen-certs,run-a10}.sh + docker-compose.yml | A10 ①②③④ | 管理端 | `bash tests/admin/admin-connect/run-a10.sh`，ubuntu + docker | 2026-11-06 main @674017f run 42202140900 a10-docker success |
| tests/admin/admin-connect/grep-guard.sh | A10（禁止放行回调）、A14 | 管理端 | `bash tests/admin/admin-connect/grep-guard.sh` | 2026-11-06 main @674017f run 42202140900 guards-acceptance success |
| scripts/ci/check-cross-refs.sh + test-check-cross-refs.sh | A11 | 共用 | `bash scripts/ci/check-cross-refs.sh`；`bash scripts/ci/test-check-cross-refs.sh` | 2026-11-06 main @674017f run 42202140900 guards-acceptance success（#82 已合） |
| tests/android/android-shell/androidTest + ci-run | A5、A6、A16 | Android | 见 workflow android / android-emulator | 2026-11-06 main @674017f run 42202140900 android + android-emulator success |
| tests/android/android-shell/lang-guard.sh | A14、A17 | Android | `bash tests/android/android-shell/lang-guard.sh` | 2026-11-06 main @674017f run 42202140900 guards-acceptance success |
| tests/android/android-diagnostics unit + instrumented | A7、A15 | Android | workflow 内 gradle / emulator | 随 android / android-emulator 同上 |
| tests/ci/pr-line-check/{pr_line_check.py,test_pr_line_check.py} | A12（PR 行数统计，提案侧） | 共用 | `python3 tests/ci/pr-line-check/test_pr_line_check.py`；线上由策略仓 `pr-size` 必过 | 策略仓 pr-size 在 PR 上跑；业务仓脚本仍作本地回归 |

A13（main 全绿）：[run 42202140900](https://github.com/Yarrow-he/OsakaConnect/actions/runs/42202140900) @ `674017f`（2026-11-06）conclusion=success。

刷新说明：pipeline-watchdog 2026-11-06 20:54 Australia/Brisbane 按 main CI 实况更新「最后通过」；旧「未跑/跳过」条目已过时。
