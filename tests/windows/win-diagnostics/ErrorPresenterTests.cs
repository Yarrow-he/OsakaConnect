// 用途：A3 统一错误展示。注入 shared/errors.json 的每个错误码，断言显示文案等于共享中文文案，
//       且不含堆栈标记 "at " 或异常类型名。
// 前置环境：Windows + .NET 8 SDK；产品类型 ErrorPresenter 已交付并与 ProductSurface.cs 对齐。
// 运行命令：dotnet test tests/windows/win-diagnostics -c Release
// 覆盖 acceptance：A3
// 待 CI：本机不装 .NET，未在本机运行；由 GitHub Actions windows-latest 执行。
using Xunit;

public sealed class ErrorPresenterTests
{
    [ProductFact(ProductSurface.ErrorPresenterType)]
    public void A3_KnownErrorCodesShowSharedChineseTextWithoutStackTrace()
    {
        var type = ProductSurface.ResolveOrThrow(ProductSurface.ErrorPresenterType);
        var errors = SharedVectors.LoadErrors();
        Assert.NotEmpty(errors);

        foreach (var (code, entry) in errors)
        {
            // 注入一个错误：后台 ErrorBody 的 code，文案唯一事实来源是 shared/errors.json。
            var shown = ProductSurface.Call(type, ProductSurface.ErrorPresenterMethod, code) as string;

            Assert.NotNull(shown);
            Assert.False(string.IsNullOrWhiteSpace(shown), $"{code} 未显示用户可读文案");
            Assert.Equal(entry.Zh, shown);
            Assert.DoesNotContain("at ", shown!);
            Assert.DoesNotContain("Exception", shown!);
            Assert.DoesNotContain("System.", shown!);
            Assert.DoesNotContain("StackTrace", shown!);
        }
    }
}
