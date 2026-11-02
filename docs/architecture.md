# 架构地图（0.1.0）

动手前先查本文件；与代码不符就上报组长。接口以 `openapi/admin.v1.yaml`（唯一事实来源）和 `shared/` 为准。

## 技术栈（A14）
Windows 客户端、管理端：C#/.NET（WPF）。Android：Kotlin。后台：Python。

## 目录与模块
| 模块 | 端 | 目录 | 职责 | 对外接口 | 依赖 |
|---|---|---|---|---|---|
| shared-protocol | 共用 | `openapi/`、`shared/`、`tests/contract/` | OpenAPI 管理协议、错误码、邀请码规则、契约/脱敏/邀请码测试向量（含反例）、契约测试 | `openapi/admin.v1.yaml`、`shared/test-vectors/admin-contract.json`、`shared/errors.json`、`shared/invite-code.md`、`shared/test-vectors/*.json` | 无 |
| server-core | 服务端 | `server/` | 后台独立启动；`/v1/health`、`/v1/version`；管理身份校验；邀请码格式校验函数 | 实现 openapi/admin.v1.yaml；响应出站前按 schema 自检（标准库手写校验） | shared-protocol |
| win-shell | Windows | `windows/` | 应用入口；连接(W1)、设置(W4) 占位页与侧栏导航；启动自检 `--self-test` | 无（进程入口） | win-diagnostics |
| win-diagnostics | Windows | `windows/` | 统一错误展示（错误码→可读文案，不含堆栈）；日志脱敏入口；邀请码格式校验 | `ErrorPresenter`、`LogRedactor`、`InviteCode.IsValid` | shared-protocol（只读 JSON 向量与错误码） |
| admin-connect | 管理端 | `admin/` | 管理连接页：填后台地址，令牌只从环境变量 `OSAKA_ADMIN_TOKEN` 取；查询版本/健康；边界校验响应；强制证书链+主机名校验；被拒/证书失败时不显示已连接 | 无 | shared-protocol、server-core（运行时） |
| android-shell | Android | `android/` | 应用入口；未激活(P1)/已激活(P2) 占位页与导航；生命周期记录 | 无 | android-diagnostics |
| android-diagnostics | Android | `android/` | 日志脱敏、错误展示、邀请码格式校验（同一份向量） | `LogRedactor`、`InviteCode.isValid` | shared-protocol |
| ci-guards | 共用 | `.github/`、`scripts/`、`ci/` | 构建测试 CI、契约测试 job、A10 docker 端到端（临时 CA、服务名 `osaka-server`）、改动范围检查、技术栈/引用路径/硬编码线路检查 | `scripts/check-scope.sh` | 全部 |

## 边界规则
1. 各端只读 `shared/`、`openapi/`，不互相引用其他端源码（A11）。
2. 后台入口调用业务函数，业务不反向导入入口。
3. 线路以列表表示，本期长度为 1，客户端不硬编码线路（A17）。
4. 密钥、令牌只走环境变量，不进仓库、不进日志。

## 工具链与模型（非产品依赖）
写手：Codex gpt-6-astra high。审阅：Claude Code + GLM（`.claude/settings.json`）。测试：Claude Code + DeepSeek（`.claude/settings.tester.json`）。key 一律只引用环境变量。

## 边界 schema 校验方式（Hao Yin 2026-11-02：不新增重依赖）
| 端 | 做法 | 理由 |
|---|---|---|
| 服务端 Python | 标准库 `json` + 手写字段校验函数，单测用 `admin-contract.json` 正反例 | 两个接口、字段少，jsonschema 只作 CI 测试依赖，不进运行时 |
| Windows/管理端 C# | `System.Text.Json` 反序列化到 `record` + `required` 成员 + `JsonUnmappedMemberHandling.Disallow`，再手写枚举/长度/正则检查 | .NET 8 内置，不加 NuGet 包 |
| Android Kotlin | Android 内置 `org.json.JSONObject` 解析 + 手写校验（拒未知字段） | 平台自带，不加 Moshi/kotlinx-serialization 插件 |
| CI | `tests/contract`：openapi-spec-validator 校验 spec，jsonschema 跑正反例 | 仅测试依赖，保证三端手写校验与 spec 同源 |
TLS：三端都用平台默认证书校验（.NET `HttpClient` 默认、Android 默认 TrustManager），禁止 `ServerCertificateCustomValidationCallback` 返回 true 或自定义放行 TrustManager；CI 临时 CA 通过系统/测试信任库注入。
