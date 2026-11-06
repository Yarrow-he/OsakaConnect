using OsakaConnect.Diagnostics;
using Xunit;

public sealed class InviteCodeTests
{
    [Fact]
    public void A15_AllInviteCodeVectorsPass()
    {
        var vectors = SharedVectors.LoadInviteCodes();
        Assert.NotEmpty(vectors.Valid);
        Assert.NotEmpty(vectors.Invalid);

        for (var i = 0; i < vectors.Valid.Count; i++)
            AssertVerdict(vectors.Valid[i], expected: true, $"合法向量 #{i}");

        for (var i = 0; i < vectors.Invalid.Count; i++)
            AssertVerdict(vectors.Invalid[i], expected: false, $"非法向量 #{i}");
    }

    private static void AssertVerdict(string? code, bool expected, string label)
    {
        var actual = InviteCode.IsValid(code);
        Assert.True(actual == expected, $"{label} 判定错误，期望 IsValid={expected}");
    }
}
