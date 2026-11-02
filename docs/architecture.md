# 架构地图（0.1.0）

动手前先查本文件；与代码不符就上报组长。接口以 `proto/` 和 `shared/` 为准。

## 技术栈（A14）
Windows 客户端、管理端：C#/.NET（WPF）。Android：Kotlin。后台：Python。

## 目录与模块
| 模块 | 端 | 目录 | 职责 | 对外接口 | 依赖 |
|---|---|---|---|---|---|
| shared-protocol | 共用 | `proto/`、`shared/` | 管理协议、错误码、邀请码规则、脱敏与邀请码测试向量 | `proto/admin/v1/admin.proto`、`shared/errors.json`、`shared/invite-code.md`、`shared/test-vectors/*.json` | 无 |
| server-core | 服务端 | `server/` | 后台独立启动；`/v1/health`、`/v1/version`；管理身份校验；邀请码格式校验函数 | admin.proto 的 HTTP/JSON 映射 | shared-protocol |
| win-shell | Windows | `windows/` | 应用入口；连接(W1)、设置(W4) 占位页与侧栏导航；启动自检 `--self-test` | 无（进程入口） | win-diagnostics |
| win-diagnostics | Windows | `windows/` | 统一错误展示（错误码→可读文案，不含堆栈）；日志脱敏入口；邀请码格式校验 | `ErrorPresenter`、`LogRedactor`、`InviteCode.IsValid` | shared-protocol（只读 JSON 向量与错误码） |
| admin-connect | 管理端 | `admin/` | 管理连接页：填后台地址，令牌只从环境变量 `OSAKA_ADMIN_TOKEN` 取；查询版本/健康；被拒时显示拒绝且不显示已连接 | 无 | shared-protocol、server-core（运行时） |
| android-shell | Android | `android/` | 应用入口；未激活(P1)/已激活(P2) 占位页与导航；生命周期记录 | 无 | android-diagnostics |
| android-diagnostics | Android | `android/` | 日志脱敏、错误展示、邀请码格式校验（同一份向量） | `LogRedactor`、`InviteCode.isValid` | shared-protocol |
| ci-guards | 共用 | `.github/`、`scripts/` | 构建测试 CI、改动范围检查、技术栈/引用路径/硬编码线路检查 | `scripts/check-scope.sh` | 全部 |

## 边界规则
1. 各端只读 `shared/`、`proto/`，不互相引用其他端源码（A11）。
2. 后台入口调用业务函数，业务不反向导入入口。
3. 线路以列表表示，本期长度为 1，客户端不硬编码线路（A17）。
4. 密钥、令牌只走环境变量，不进仓库、不进日志。

## 工具链与模型（非产品依赖）
写手：Codex gpt-6-astra high。审阅：Claude Code + GLM（`.claude/settings.json`）。测试：Claude Code + DeepSeek（`.claude/settings.tester.json`）。key 一律只引用环境变量。
