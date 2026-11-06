using System.Globalization;
using System.Text.Json;
using OsakaConnect.Diagnostics;
using Xunit;

namespace OsakaConnect.Diagnostics.Tests;

public sealed class DiagnosticsTests
{
    private static string ReadShared(string path) => File.ReadAllText(Path.Combine(AppContext.BaseDirectory, "shared", path));

    public static IEnumerable<object[]> RedactionCases()
    {
        using var document = JsonDocument.Parse(ReadShared("test-vectors/redaction.json"));
        var cases = document.RootElement.GetProperty("cases");
        Assert.NotEmpty(cases.EnumerateArray());
        foreach (var item in cases.EnumerateArray())
            yield return new object[] { item.GetProperty("in").GetString()!, item.GetProperty("out").GetString()! };
    }

    [Theory, MemberData(nameof(RedactionCases))]
    [InlineData("token: Bearer abc.def.ghi", "token: Bearer ***")]
    [InlineData("invite=7X2KQ9MZ&password=hunter2", "invite=***&password=***")]
    [InlineData("{\"password\":\"hunter2 hunter2\",\"invite\":\"7X2KQ9MZ\"}", "{\"password\":\"***\",\"invite\":\"***\"}")]
    [InlineData("password='hunter2 hunter2' status=ok", "password='***' status=ok")]
    [InlineData("password=\"hunter2 hunter2", "password=***")]
    [InlineData("token: abc.def.ghi\nconnected route=r1", "token: ***\nconnected route=r1")]
    [InlineData("", ""), InlineData("token_count=0 invitation=none", "token_count=0 invitation=none")]
    public void RedactsSharedVectors(string input, string expected)
    {
        Assert.Equal(expected, LogRedactor.Redact(input));
        Assert.Equal(expected, LogRedactor.Redact(expected));
        var previousCulture = CultureInfo.CurrentCulture;
        try
        {
            CultureInfo.CurrentCulture = CultureInfo.GetCultureInfo("tr-TR");
            Assert.Equal(expected.ToUpperInvariant(), LogRedactor.Redact(input.ToUpperInvariant()));
        }
        finally { CultureInfo.CurrentCulture = previousCulture; }
    }

    public static IEnumerable<object?[]> InviteCases()
    {
        using var document = JsonDocument.Parse(ReadShared("test-vectors/invite-code.json"));
        foreach (var (key, expected) in new[] { ("valid", true), ("invalid", false) })
        {
            var cases = document.RootElement.GetProperty(key);
            Assert.NotEmpty(cases.EnumerateArray());
            foreach (var item in cases.EnumerateArray())
                yield return new object?[] { item.GetString(), expected };
        }
    }

    [Theory, MemberData(nameof(InviteCases))]
    [InlineData("\t abcd1234 \r\n", true), InlineData("        ", false)]
    [InlineData("ſ0000000", false), InlineData("K0000000", false), InlineData("0000000\n0", false)]
    public void ValidatesSharedInviteVectors(string? input, bool expected) => Assert.Equal(expected, InviteCode.IsValid(input));

    [Fact]
    public void PresentsAllSharedChineseMessages()
    {
        var json = ReadShared("errors.json");
        var presenter = new ErrorPresenter(json);
        using var document = JsonDocument.Parse(json);
        Assert.NotEmpty(document.RootElement.EnumerateObject());
        foreach (var error in document.RootElement.EnumerateObject())
            Assert.Equal(error.Value.GetProperty("zh").GetString(), presenter.GetMessage(error.Name));
    }

    [Theory, InlineData(null), InlineData(""), InlineData("UNKNOWN")]
    public void UnknownCodesUseCatalogInternalMessage(string? code)
    {
        const string internalMessage = "内部服务暂不可用";
        var presenter = new ErrorPresenter("{\"INTERNAL\":{\"zh\":\"" + internalMessage + "\"}}");
        Assert.Equal(internalMessage, presenter.GetMessage(code));
    }

    [Fact]
    public void ExceptionWithStackTraceUsesOnlyInternalMessage()
    {
        var presenter = new ErrorPresenter(ReadShared("errors.json"));
        Action fail = () => throw new InvalidOperationException("diagnostic failure detail");
        var exception = Assert.Throws<InvalidOperationException>(fail);
        Assert.False(string.IsNullOrEmpty(exception.StackTrace));

        var message = presenter.GetMessage("FORBIDDEN", exception);
        Assert.Equal(presenter.GetMessage("INTERNAL"), message);
        Assert.DoesNotContain(exception.Message, message);
        Assert.DoesNotContain(nameof(InvalidOperationException), message);
        Assert.DoesNotContain(exception.StackTrace!, message);
        Assert.DoesNotContain("\n", message);
    }

    [Theory, InlineData(null), InlineData(""), InlineData("not json")]
    [InlineData("null"), InlineData("[]"), InlineData("{}")]
    [InlineData("{\"INTERNAL\":{\"zh\":null}}"), InlineData("{\"INTERNAL\":{\"zh\":123}}")]
    [InlineData("{\"INTERNAL\":{\"zh\":\" \"}}"), InlineData("{\"INTERNAL\":{}}")]
    [InlineData("{\"FORBIDDEN\":{\"zh\":\"管理凭证无效\"}}")]
    [InlineData("{\"INTERNAL\":{\"zh\":\"内部服务暂不可用\"},\"FORBIDDEN\":null}")]
    [InlineData("{\"INTERNAL\":{\"zh\":\"内部服务暂不可用\"},\"INTERNAL\":{\"zh\":\"重复\"}}")]
    public void InvalidCatalogFallsBackSafely(string? json)
    {
        var presenter = new ErrorPresenter(json!);
        Assert.Equal("发生内部错误，请稍后重试", presenter.GetMessage("FORBIDDEN"));
        Assert.Equal("发生内部错误，请稍后重试", presenter.GetMessage("INTERNAL"));
        Assert.Equal("发生内部错误，请稍后重试", presenter.GetMessage(exception: new Exception("diagnostic detail")));
    }
}
