// 用途：A15 邀请码格式。逐条跑 shared/test-vectors/invite-code.json 的全部正反例
//       （含长度 7/9、I/L/O/U、小写、首尾空白、null、全角/横杠/空格）。
// 前置环境：Windows + .NET 8 SDK；产品类型 InviteCode.IsValid(string?) 已交付。
// 运行命令：dotnet test tests/windows/win-diagnostics -c Release
// 覆盖 acceptance：A15
// 待 CI：本机不装 .NET，未在本机运行；由 GitHub Actions windows-latest 执行。
using OsakaConnect.Diagnostics;
using Xunit;

public sealed class InviteCodeTests
{
    [ProductFact]
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

        Assert.True(
            actual == expected,
            $"{label} 判定错误：{Show(code)}，期望 IsValid={expected}");
    }

    private static string Show(string? code) => code is null ? "<null>" : $"\"{code}\"";
}
