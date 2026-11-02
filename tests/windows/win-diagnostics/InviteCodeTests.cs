// 用途：A15 邀请码格式。逐条跑 shared/test-vectors/invite-code.json 的全部正反例
//       （含长度 7/9、I/L/O/U、小写、首尾空白、null、全角/横杠/空格）。
// 前置环境：Windows + .NET 8 SDK；产品类型 InviteCode.IsValid 已交付并与 ProductSurface.cs 对齐。
// 运行命令：dotnet test tests/windows/win-diagnostics -c Release
// 覆盖 acceptance：A15
// 待 CI：本机不装 .NET，未在本机运行；由 GitHub Actions windows-latest 执行。
using Xunit;

public sealed class InviteCodeTests
{
    [ProductFact(ProductSurface.InviteCodeType)]
    public void A15_AllInviteCodeVectorsPass()
    {
        var type = ProductSurface.ResolveOrThrow(ProductSurface.InviteCodeType);
        var vectors = SharedVectors.LoadInviteCodes();
        Assert.NotEmpty(vectors.Valid);
        Assert.NotEmpty(vectors.Invalid);

        for (var i = 0; i < vectors.Valid.Count; i++)
            AssertVerdict(type, vectors.Valid[i], expected: true, $"合法向量 #{i}");

        for (var i = 0; i < vectors.Invalid.Count; i++)
            AssertVerdict(type, vectors.Invalid[i], expected: false, $"非法向量 #{i}");
    }

    private static void AssertVerdict(Type type, string? code, bool expected, string label)
    {
        var result = ProductSurface.Call(type, ProductSurface.InviteCodeIsValidMethod, code);

        Assert.True(
            result is bool actual && actual == expected,
            $"{label} 判定错误：{Show(code)}，期望 IsValid={expected}");
    }

    private static string Show(string? code) => code is null ? "<null>" : $"\"{code}\"";
}
