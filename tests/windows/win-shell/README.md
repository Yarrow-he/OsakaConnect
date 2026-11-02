# win-shell 测试说明

- 脚本：`smoke.ps1`，覆盖 A1、A2/A16、A14、A17；面向 GitHub Actions `windows-latest`。
- 运行：`pwsh -NoProfile -File tests/windows/win-shell/smoke.ps1`
- 未交付行为：`windows/` 不存在时输出“待测：模块未交付”并退出 0。
- 本机不装 .NET、不构建；本仓库本机未运行，**待 CI**。

## 待写手交付后对齐（脚本顶部集中定义）

| 项 | 默认/约定 | 覆盖方式 |
|---|---|---|
| 入口 exe | 按 csproj `AssemblyName` 在 `windows/**/bin/**/*.exe` 自动查找 | `OSAKA_WIN_EXE` |
| 主窗口标题 | 不校验 | `OSAKA_WIN_WINDOW_TITLE` |
| 导航项显示名 | `连接`、`设置` | 改 `$NavPages` |
| 页标识 | 页面元素 `AutomationId` 或 `Name` 为 `W1`/`W4` | 改 `$NavPages`/`Find-Element` |

A2/A16 用 PowerShell + Windows UI Automation 驱动真实窗口，断言的是用户可见的导航与页标识，不调用产品内部函数。

## 为什么没有 xUnit 测试项目

UI 自动化在 `windows-latest` 上用系统自带 UI Automation 即可完成，xUnit 还需额外测试项目与 NuGet 依赖，按最小修改不建；静态项（A14/A17）是同脚本内的断言。

## 静态检查口径

- A14：扫描 `windows/` 下非 `bin/`、`obj/` 文件，只允许 `.csproj`/`.cs`/`.xaml`。
- A17：扫描 `.cs`/`.xaml`，禁止字面量 `Line 01`、`OSA · 01`；资源文件（若后续新增）不在本任务扫描范围。
