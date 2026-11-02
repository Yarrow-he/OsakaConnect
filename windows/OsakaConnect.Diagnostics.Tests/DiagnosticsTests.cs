using System.Globalization;
using System.Text.Json;
using OsakaConnect.Diagnostics;
using Xunit;

namespace OsakaConnect.Diagnostics.Tests;

public sealed class DiagnosticsTests
{
    private static string ReadShared(string relativePath) =>
        File.ReadAllText(Path.Combine(AppContext.BaseDirectory, "shared", relativePath));

    public static IEnumerable<object[]> RedactionCases()
    {
        using var document = JsonDocument.Parse(ReadShared("test-vectors/redaction.json"));
        foreach (var item in document.RootElement.GetProperty("cases").EnumerateArray())
            yield return new object[] { item.GetProperty("in").GetString()!, item.GetProperty("out").GetString()! };
    }

    [Theory]
    [MemberData(nameof(RedactionCases))]
    public void RedactsSharedVectors(string input, string expected)
    {
        Assert.Equal(expected, LogRedactor.Redact(input));
        Assert.Equal(expected, LogRedactor.Redact(expected));
    }

    [Theory]
    [MemberData(nameof(RedactionCases))]
    public void RedactsKeysRegardlessOfCaseAndCulture(string input, string expected)
    {
        var previousCulture = CultureInfo.CurrentCulture;
        try
        {
            CultureInfo.CurrentCulture = CultureInfo.GetCultureInfo("tr-TR");
            Assert.Equal(expected.ToUpperInvariant(), LogRedactor.Redact(input.ToUpperInvariant()));
        }
        finally
        {
            CultureInfo.CurrentCulture = previousCulture;
        }
    }

    [Theory]
    [InlineData("token: Bearer abc.def.ghi", "token: Bearer ***")]
    [InlineData("invite=7X2KQ9MZ&password=hunter2", "invite=***&password=***")]
    [InlineData("{\"password\":\"hunter2 hunter2\",\"invite\":\"7X2KQ9MZ\"}", "{\"password\":\"***\",\"invite\":\"***\"}")]
    [InlineData("password='hunter2 hunter2' status=ok", "password='***' status=ok")]
    [InlineData("password=\"hunter2 hunter2", "password=***")]
    [InlineData("token: abc.def.ghi\nconnected route=r1", "token: ***\nconnected route=r1")]
    [InlineData("", "")]
    [InlineData("token_count=0 invitation=none", "token_count=0 invitation=none")]
    public void RedactsCompleteValuesAndPreservesSurroundingText(string input, string expected) =>
        Assert.Equal(expected, LogRedactor.Redact(input));

    public static IEnumerable<object?[]> InviteCases()
    {
        using var document = JsonDocument.Parse(ReadShared("test-vectors/invite-code.json"));
        foreach (var item in document.RootElement.GetProperty("valid").EnumerateArray())
            yield return new object?[] { item.GetString(), true };
        foreach (var item in document.RootElement.GetProperty("invalid").EnumerateArray())
            yield return new object?[] { item.GetString(), false };
    }

    [Theory]
    [MemberData(nameof(InviteCases))]
    public void ValidatesSharedInviteVectors(string? input, bool expected) =>
        Assert.Equal(expected, InviteCode.IsValid(input));

    [Theory]
    [InlineData("\t abcd1234 \r\n", true)]
    [InlineData("        ", false)]
    [InlineData("ſ0000000", false)]
    [InlineData("K0000000", false)]
    [InlineData("0000000\n0", false)]
    public void ValidatesInviteWhitespaceAndAscii(string input, bool expected) =>
        Assert.Equal(expected, InviteCode.IsValid(input));

    [Fact]
    public void PresentsAllSharedChineseMessages()
    {
        var json = ReadShared("errors.json");
        var presenter = new ErrorPresenter(json);
        using var document = JsonDocument.Parse(json);
        foreach (var error in document.RootElement.EnumerateObject())
            Assert.Equal(error.Value.GetProperty("zh").GetString(), presenter.GetMessage(error.Name));
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("UNKNOWN")]
    public void UnknownCodesUseInternalMessage(string? code)
    {
        var presenter = new ErrorPresenter(ReadShared("errors.json"));
        Assert.Equal(presenter.GetMessage("INTERNAL"), presenter.GetMessage(code));
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
        Assert.Contains("内部错误", message);
        Assert.DoesNotContain(exception.Message, message);
        Assert.DoesNotContain(nameof(InvalidOperationException), message);
        Assert.DoesNotContain(exception.StackTrace!, message);
        Assert.DoesNotContain("\n", message);
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("not json")]
    [InlineData("[]")]
    [InlineData("{}")]
    [InlineData("{\"INTERNAL\":{\"zh\":null}}")]
    [InlineData("{\"INTERNAL\":{\"zh\":123}}")]
    [InlineData("{\"INTERNAL\":{\"zh\":\" \"}}")]
    [InlineData("{\"INTERNAL\":{}}")]
    [InlineData("{\"FORBIDDEN\":{\"zh\":\"管理凭证无效\"}}")]
    public void InvalidCatalogFallsBackSafely(string? json)
    {
        var reference = new ErrorPresenter(ReadShared("errors.json"));
        var presenter = new ErrorPresenter(json!);
        Assert.Equal(reference.GetMessage("INTERNAL"), presenter.GetMessage("FORBIDDEN"));
        Assert.Equal(reference.GetMessage("INTERNAL"), presenter.GetMessage("INTERNAL"));
    }
}
