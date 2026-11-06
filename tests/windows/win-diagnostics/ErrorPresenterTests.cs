using OsakaConnect.Diagnostics;
using Xunit;

public sealed class ErrorPresenterTests
{
    [Fact]
    public void A3_KnownErrorCodesShowSharedChineseTextWithoutStackTrace()
    {
        var presenter = new ErrorPresenter(SharedVectors.ReadErrorsJson());
        var errors = SharedVectors.LoadErrors();
        Assert.NotEmpty(errors);

        foreach (var (code, entry) in errors)
        {
            var shown = presenter.GetMessage(code);

            Assert.False(string.IsNullOrWhiteSpace(shown), $"{code} 未显示用户可读文案");
            Assert.Equal(entry.Zh, shown);
            Assert.DoesNotContain("at ", shown);
            Assert.DoesNotContain("Exception", shown);
            Assert.DoesNotContain("System.", shown);
            Assert.DoesNotContain("StackTrace", shown);
        }
    }

    [Fact]
    public void A3_ExceptionWithStackTraceShowsOnlyInternalMessage()
    {
        var presenter = new ErrorPresenter(SharedVectors.ReadErrorsJson());
        Action fail = () => throw new InvalidOperationException("diagnostic failure detail");
        var exception = Assert.Throws<InvalidOperationException>(fail);
        Assert.False(string.IsNullOrEmpty(exception.StackTrace));

        var shown = presenter.GetMessage("FORBIDDEN", exception);
        Assert.Equal(SharedVectors.LoadErrors()["INTERNAL"].Zh, shown);
        Assert.DoesNotContain(exception.Message, shown);
        Assert.DoesNotContain(nameof(InvalidOperationException), shown);
        Assert.DoesNotContain(exception.StackTrace!, shown);
        Assert.DoesNotContain("at ", shown);
        Assert.DoesNotContain("Exception", shown);
        Assert.DoesNotContain("System.", shown);
        Assert.DoesNotContain("StackTrace", shown);
    }
}
