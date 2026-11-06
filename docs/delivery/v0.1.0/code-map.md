# 第 1 期（0.1.0）代码地图

对照 `docs/architecture.md` 与 main 目录树整理。接口以 `openapi/admin.v1.yaml` 和 `shared/` 为准；各端只读共用层，不互引源码。

## 1. shared-protocol（共用协议）

1. **目录**：`openapi/`、`shared/`、`tests/contract/`
2. **职责**：管理协议唯一事实来源；错误码、邀请码规则、契约/脱敏/邀请码测试向量（含反例）；契约测试保证三端手写校验同源。
3. **对外接口**：`openapi/admin.v1.yaml`；`shared/errors.json`；`shared/invite-code.md`；`shared/test-vectors/{admin-contract,invite-code,redaction}.json`；`shared/FROZEN.sha256`
4. **依赖**：无

## 2. server-core（服务端）

1. **目录**：`server/`（包 `server/osaka_server/`：`app.py`、`invite.py`、`schema.py`）；验收测 `tests/server/server-core/`
2. **职责**：独立启动后台；`/v1/health`、`/v1/version`；管理身份校验；邀请码格式校验；出站前按 schema 自检（标准库手写，不加 jsonschema 运行时）。
3. **对外接口**：实现 `openapi/admin.v1.yaml`；`osaka_server.invite.is_valid_invite_code`
4. **依赖**：shared-protocol

## 3. win-shell（Windows 壳）

1. **目录**：`windows/OsakaConnect/`（`App.xaml.cs`、`MainWindow`、`ConnectionPage`、`SettingsPage`）；测 `windows/OsakaConnect.Tests/`、`tests/windows/win-shell/smoke.ps1`
2. **职责**：WPF 应用入口；连接(W1)、设置(W4) 占位页与侧栏导航；启动自检 `--self-test`
3. **对外接口**：无（进程入口）
4. **依赖**：win-diagnostics

## 4. win-diagnostics（Windows 诊断）

1. **目录**：`windows/OsakaConnect.Diagnostics/`（`ErrorPresenter.cs`、`LogRedactor.cs`、`InviteCode.cs`）；测 `windows/OsakaConnect.Diagnostics.Tests/`、`tests/windows/win-diagnostics/`
2. **职责**：统一错误展示（错误码→可读文案，不含堆栈）；日志脱敏；邀请码 8 位 Crockford 格式校验
3. **对外接口**：`ErrorPresenter`、`LogRedactor`、`InviteCode.IsValid`
4. **依赖**：shared-protocol（只读 JSON 向量与错误码）

## 5. admin-connect（管理端）

1. **目录**：`admin/OsakaAdmin/`（`MainWindow`、`AdminProbe`、`AdminContract`、`ProbeCommand`）；探针 `admin/OsakaAdmin.Probe/`、`admin/Dockerfile.probe`；测 `admin/OsakaAdmin.Tests/`、`tests/admin/admin-connect/`
2. **职责**：管理连接页——填后台地址，令牌只从环境变量 `OSAKA_ADMIN_TOKEN` 取；查版本/健康；边界校验响应；强制证书链+主机名校验；被拒/证书失败时不显示已连接；A10 四案 docker 端到端
3. **对外接口**：无（独立管理进程）
4. **依赖**：shared-protocol；运行时连 server-core

## 6. android-shell（Android 壳）

1. **目录**：`android/app/src/main/java/com/osakaconnect/shell/`（`MainActivity.kt`、`Page.kt`）；仪器测与 lang-guard 在 `tests/android/android-shell/`
2. **职责**：应用入口；未激活(P1)/已激活(P2) 占位页与导航；生命周期记录
3. **对外接口**：无
4. **依赖**：android-diagnostics

## 7. android-diagnostics（Android 诊断）

1. **目录**：`android/app/src/main/java/com/osakaconnect/diagnostics/`（`LogRedactor.kt`、`InviteCode.kt`、`ErrorMessages.kt`、`AdminContract.kt`）；单测同包 `android/app/src/test/.../diagnostics/`；仪器测 `tests/android/android-diagnostics/`
2. **职责**：日志脱敏、错误展示、邀请码格式校验（与 Windows/服务端同一份向量）
3. **对外接口**：`LogRedactor`、`InviteCode.isValid`、相关错误文案
4. **依赖**：shared-protocol

## 8. ci-guards（CI 与门禁）

1. **目录**：`.github/workflows/`（`ci.yml`、`policy-selftest.yml`）；`scripts/check-scope.sh`；`scripts/ci/check-cross-refs.sh`；`tests/ci/pr-line-check/`；各端 grep/lang-guard
2. **职责**：构建测试 CI；契约 job；A10 docker 端到端；改动范围检查；技术栈/跨端引用/硬编码线路检查；PR 行数统计（提案侧）
3. **对外接口**：`scripts/check-scope.sh`；CI workflow jobs（windows-acceptance、android-emulator、a10-docker、guards-acceptance 等）
4. **依赖**：全部业务模块（门禁覆盖）

## 边界（交付时别踩）

1. 各端只读 `shared/`、`openapi/`，不互相引用其他端源码（A11）。
2. 后台入口调业务函数，业务不反向导入入口。
3. 线路以列表表示，本期长度 1，客户端不硬编码线路（A17）。
4. 密钥、令牌只走环境变量，不进仓库、不进日志。
