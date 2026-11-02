# 测试索引

| 测试 | 覆盖验收 | 端 | 命令 | 最后通过 |
|---|---|---|---|---|
| tests/contract/test_contract.py | A9（契约） | 共用 | `pytest tests/contract` | 2026-11-02 @872a57a run 41406112495 18 passed |
| tests/server/server-core/test_server_core.py | A8、A9（后台）、A15（后台）、A17（后台） | 服务端 | `OSAKA_SERVER_CMD="python3 -m osaka_server.app" OSAKA_INVITE_FN=osaka_server.invite.is_valid_invite_code PYTHONPATH=server pytest tests/server/server-core -q -rs` | 部分：2026-11-02 @94c35f3 17 过、6 skip；@872a57a run 41406112495 未执行本脚本（不在分支上），6 项仍未验证 |
| tests/windows/win-shell/smoke.ps1 | A1、A2、A16、A14、A17 | Windows | 见文件头，windows-latest | 阻塞：run 41403138518 未调用（2026-11-02）；A14/A17 本机静态检查通过 @02e2877 |
| tests/windows/win-diagnostics/*.cs（WinDiagnostics.Tests.csproj） | A3、A4、A15 | Windows | `dotnet test`，windows-latest | 阻塞：run 41403148691 未调用（2026-11-02） |
| tests/admin/admin-connect/{gen-certs,run-a10}.sh + docker-compose.yml | A10 ①②③④ | 管理端 | `bash tests/admin/admin-connect/run-a10.sh`，ubuntu + docker | 未跑：run 41406112495、41407456188 a10-docker 均输出“无 A10 脚本，跳过” |
| tests/admin/admin-connect/grep-guard.sh | A10（禁止放行回调）、A14 | 管理端 | `bash tests/admin/admin-connect/grep-guard.sh` | 2026-11-02 通过 @93fba9d、@02e2877、@e700fe5（修复 set -e 误判后） |
| tests/android/android-shell/androidTest/ShellNavigationTest.kt + ci-run.sh | A5、A6、A16 | Android | 见文件头，ubuntu + 模拟器 | 阻塞：模块分支 workflow 无模拟器、不调用本脚本（2026-11-02） |
| tests/android/android-shell/lang-guard.sh | A14、A17 | Android | `bash tests/android/android-shell/lang-guard.sh` | 2026-11-02 通过 @9e47998、@97d42d6 |
| tests/android/android-diagnostics/DiagnosticsVectorsTest.kt + run.sh | A7、A15 | Android | `./gradlew testDebugUnitTest` | 阻塞：workflow 不调用 run.sh（2026-11-02） |
| tests/android/android-diagnostics/{json-divergence.json,JvmJsonDivergenceTest.kt,androidTest/OrgJsonDivergenceTest.kt,run-instrumented.sh} | A9/A7 相关（JvmJson 与真机分歧，审阅 2026-11-02） | Android | `bash tests/android/android-diagnostics/run-instrumented.sh` | 待跑（等模拟器 CI） |

未覆盖：A11（跨端引用路径检查，ci-guards）、A12（行数统计，ci-guards/审阅）、A13（main 全绿链接）；A14 的 server/ 只 Python 检查与 docs 一致性；A17 的 windows/admin 以外后台 routes 列表长度已由 server-core 覆盖。
