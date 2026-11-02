# win-diagnostics 测试（Windows / xUnit）

用途：覆盖 A3（统一错误展示）、A4（日志脱敏）、A15（邀请码格式），全部断言读 `shared/` 的共享向量。

> 待 CI：本机不装 .NET SDK，未在本机构建或运行；由 GitHub Actions `windows-latest` 执行。

## 前置环境

- Windows + .NET 8 SDK（目标框架 `net8.0-windows`）
- 仓库保持 `shared/errors.json`、`shared/test-vectors/*.json` 在相对仓库根的位置（测试从输出目录向上查找仓库根）
- 产品工程由写手交付后，在 `WinDiagnostics.Tests.csproj` 的 `ProductProject`（或 `-p:ProductProject=<路径>`）指向它
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
| `ErrorPresenterTests` | A3 | `shared/errors.json` | 每个错误码显示对应 `zh` 文案；不含 `at `、`Exception`、`System.`、`StackTrace` |
| `LogRedactorTests` | A4 | `shared/test-vectors/redaction.json` | 全部 case 的 `in` 经入口后精确等于 `out` |
| `InviteCodeTests` | A15 | `shared/test-vectors/invite-code.json` | `valid` 全为 true、`invalid` 全为 false（含 `null`） |

## 产品未交付时的行为

`ProductSurface.cs` 在测试发现阶段查找产品类型；找不到时该用例以跳过原因 **“待测：模块未交付”** 结束，
不失败、也不伪装通过。产品工程不存在时 `WinDiagnostics.Tests.csproj` 不引用它，因此测试工程本身仍可编译运行；
`dotnet test` 还会通过 MSBuild 目标打印 `待测：模块未交付（win-diagnostics；未找到 <路径>）`。
查看每个用例的跳过原因可加 `--logger "console;verbosity=detailed"`。

## 待写手交付后对齐（只改这些）

1. `WinDiagnostics.Tests.csproj` 的 `ProductProject` 路径。
2. `ProductSurface.cs` 顶部常量：程序集名、命名空间、`ErrorPresenter` 的入口方法名、`LogRedactor` 的入口方法名。
   类型名 `ErrorPresenter` / `LogRedactor` / `InviteCode.IsValid` 来自 brief，未改动。
3. 若入口是实例方法，测试会尝试无参构造；需要依赖注入时在此处适配。

## CI 接线提醒

现有 `.github/workflows/ci.yml` 的 `windows-dotnet` job 只扫描 `windows/` 与 `admin/` 下的工程，
不会自动发现本目录；该 workflow 归 ci-guards 模块维护，本任务不改。接线时增加一步：

```yaml
- run: dotnet test tests/windows/win-diagnostics -c Release
```
