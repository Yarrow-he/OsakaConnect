# 测试索引

| 测试 | 覆盖验收 | 端 | 命令 | 最后通过 |
|---|---|---|---|---|
| tests/contract/test_contract.py | A9（契约） | 共用 | `pytest tests/contract` | 未跑（本机缺 jsonschema/openapi-spec-validator） |
| tests/server/server-core/test_server_core.py | A8、A9（后台）、A15（后台）、A17（后台） | 服务端 | `OSAKA_SERVER_CMD="python3 -m osaka_server.app" OSAKA_INVITE_FN=osaka_server.invite.is_valid_invite_code PYTHONPATH=server pytest tests/server/server-core -q -rs` | 部分：2026-11-02 @94c35f3 17 过、6 skip（缺 jsonschema，HTTP 用例未完整验证） |
| tests/windows/win-shell/smoke.ps1 | A1、A2、A16、A14、A17 | Windows | 见文件头，windows-latest | 未跑（待 CI） |
| tests/windows/win-diagnostics/*.cs（WinDiagnostics.Tests.csproj） | A3、A4、A15 | Windows | `dotnet test`，windows-latest | 未跑（待 CI） |
| tests/admin/admin-connect/{gen-certs,run-a10}.sh + docker-compose.yml | A10 ①②③④ | 管理端 | `bash tests/admin/admin-connect/run-a10.sh`，ubuntu + docker | 未跑（待 CI，本机无 docker） |
| tests/admin/admin-connect/grep-guard.sh | A10（禁止放行回调）、A14 | 管理端 | `bash tests/admin/admin-connect/grep-guard.sh` | 未跑实测：2026-11-02 输出“待测：模块未交付” |
| tests/android/android-shell/androidTest/ShellNavigationTest.kt + ci-run.sh | A5、A6、A16 | Android | 见文件头，ubuntu + 模拟器 | 阻塞：模块分支 workflow 无模拟器、不调用本脚本（2026-11-02） |
| tests/android/android-shell/lang-guard.sh | A14、A17 | Android | `bash tests/android/android-shell/lang-guard.sh` | 2026-11-02 通过 @9e47998、@97d42d6 |
| tests/android/android-diagnostics/DiagnosticsVectorsTest.kt + run.sh | A7、A15 | Android | `./gradlew testDebugUnitTest` | 阻塞：workflow 不调用 run.sh（2026-11-02） |

未覆盖：A11（跨端引用路径检查，ci-guards）、A12（行数统计，ci-guards/审阅）、A13（main 全绿链接）；A14 的 server/ 只 Python 检查与 docs 一致性；A17 的 windows/admin 以外后台 routes 列表长度已由 server-core 覆盖。
