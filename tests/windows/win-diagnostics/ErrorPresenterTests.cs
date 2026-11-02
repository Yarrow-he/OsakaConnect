// 用途：A3 统一错误展示。注入 shared/errors.json 的每个错误码，断言显示文案等于共享中文文案，
//       且不含堆栈标记 "at " 或异常类型名。
// 前置环境：Windows + .NET 8 SDK；产品类型 ErrorPresenter 已交付并与真实签名对齐。
// 运行命令：dotnet test tests/windows/win-diagnostics -c Release
// 覆盖 acceptance：A3
// 待 CI：本机不装 .NET，未在本机运行；由 GitHub Actions windows-latest 执行。
using OsakaConnect.Diagnostics;
using Xunit;

public sealed class ErrorPresenterTests
{
    [ProductFact]
    public void A3_KnownErrorCodesShowSharedChineseTextWithoutStackTrace()
    {
        var errors = SharedVectors.LoadErrors();
        Assert.NotEmpty(errors);

        // 产品构造签名：ErrorPresenter(string errorsJson)；文案唯一事实来源是 shared/errors.json。
        var presenter = new ErrorPresenter(SharedVectors.LoadErrorsJson());

        foreach (var (code, entry) in errors)
        {
            var shown = presenter.GetMessage(code);

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
