// 用途：产品已由写手交付（程序集/命名空间 OsakaConnect.Diagnostics），测试直接用编译期引用；
//       产品工程缺失时由 WinDiagnostics.Tests.csproj 的 EnsureProductProject 目标构建失败。
// 前置环境：Windows + .NET 8 SDK；产品工程 windows/OsakaConnect.Diagnostics/ 已交付。
// 运行命令：dotnet test tests/windows/win-diagnostics -c Release
using Xunit;

// 产品用例标记；不再携带任何跳过逻辑——缺失产品时构建失败，测试无从跳过。
public sealed class ProductFactAttribute : FactAttribute
{
}
