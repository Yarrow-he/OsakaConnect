# win-diagnostics 验收测试

覆盖 A3（统一错误展示）、A4（日志脱敏）、A15（邀请码格式）。测试直接调用
`OsakaConnect.Diagnostics` 的 `ErrorPresenter(string)` / `GetMessage`、
`LogRedactor.Redact` 和 `InviteCode.IsValid`。

需要 .NET 8 SDK。测试工程无条件引用
`windows/OsakaConnect.Diagnostics/OsakaConnect.Diagnostics.csproj`；
缺少产品时 `EnsureProductProject` 报错，构建或测试命令以非零退出码结束。
没有反射替身、跳过属性或条件空编译。

```bash
dotnet test tests/windows/win-diagnostics/WinDiagnostics.Tests.csproj -c Release
```

共享数据从输出目录向上查找仓库根后读取，运行时须保留仓库的 `shared/` 目录。

| 测试 | 共享数据 | 断言 |
|---|---|---|
| `ErrorPresenterTests` | `shared/errors.json` | 所有码返回对应中文；带堆栈异常只返回 INTERNAL，不含异常详情 |
| `LogRedactorTests` | `shared/test-vectors/redaction.json` | 全部 case 精确匹配输出，非敏感文本不变 |
| `InviteCodeTests` | `shared/test-vectors/invite-code.json` | 全部正反例（包括 null）符合预期 |
