# win-diagnostics 测试（Windows / xUnit）

用途：覆盖 A3（统一错误展示）、A4（日志脱敏）、A15（邀请码格式），全部断言读 `shared/` 的共享向量，
并通过产品工程 `windows/OsakaConnect.Diagnostics/OsakaConnect.Diagnostics.csproj` 直接调用真实实现。

> 待 CI：本机不装 .NET SDK，未在本机构建或运行；由 GitHub Actions `windows-latest` 执行。

## 前置环境

- Windows + .NET 8 SDK（目标框架 `net8.0-windows`）
- 仓库保持 `shared/errors.json`、`shared/test-vectors/*.json` 在相对仓库根的位置（测试从输出目录向上查找仓库根）
- 产品工程 `windows/OsakaConnect.Diagnostics/OsakaConnect.Diagnostics.csproj` 已交付；默认路径可用
  `-p:ProductProject=<路径>` 覆盖
- 测试依赖仅：xUnit 2.9.2、xunit.runner.visualstudio 2.8.2、Microsoft.NET.Test.Sdk 17.11.1（任务卡指定 xUnit；不进产品代码）

## 运行命令

```bash
dotnet test tests/windows/win-diagnostics -c Release
```

只跑某一类：

```bash
dotnet test tests/windows/win-diagnostics -c Release --filter FullyQualifiedName~ErrorPresenterTests
dotnet test tests/windows/win-diagnostics -c Release --filter FullyQualifiedName~LogRedactorTests
dotnet test tests/windows/win-diagnostics -c Release --filter FullyQualifiedName~InviteCodeTests
```

## 覆盖

| 测试 | acceptance | 共享事实来源 | 断言 |
|---|---|---|---|
| `ErrorPresenterTests` | A3 | `shared/errors.json` | 每个错误码经 `new ErrorPresenter(errorsJson).GetMessage(code)` 显示对应 `zh` 文案；不含 `at `、`Exception`、`System.`、`StackTrace` |
| `LogRedactorTests` | A4 | `shared/test-vectors/redaction.json` | 全部 case 的 `in` 经 `LogRedactor.Redact` 后精确等于 `out` |
| `InviteCodeTests` | A15 | `shared/test-vectors/invite-code.json` | `InviteCode.IsValid` 对 `valid` 全为 true、`invalid` 全为 false（含 `null`） |

产品真实接口：程序集/命名空间 `OsakaConnect.Diagnostics`；`InviteCode.IsValid(string?)` 与
`LogRedactor.Redact(string)` 为静态方法；`ErrorPresenter` 为实例类，构造函数 `ErrorPresenter(string errorsJson)`，
方法 `GetMessage(string? code = null, Exception? exception = null)`。

## 产品未交付时的行为

`WinDiagnostics.Tests.csproj` 无条件引用产品工程，并在 `EnsureProductProject` 目标中检查路径：
不存在时 MSBuild 以 `待测：模块未交付（win-diagnostics；未找到 <路径>）` 报错，构建/测试以非 0 退出。
测试代码中没有任何跳过（Skip）逻辑，产品缺失不可能变成绿色。

## CI 接线提醒

现有 `.github/workflows/ci.yml` 的 `windows-dotnet` job 只扫描 `windows/` 与 `admin/` 下的工程，
不会自动发现本目录；该 workflow 归 ci-guards 模块维护，本任务不改。接线时增加一步：

```yaml
- run: dotnet test tests/windows/win-diagnostics -c Release
```
