// 用途：A4 日志脱敏入口。逐条跑 shared/test-vectors/redaction.json 的全部 case，
//       断言输出与向量 out 完全一致（token/邀请码/密钥被掩码，普通日志不变）。
// 前置环境：Windows + .NET 8 SDK；产品类型 LogRedactor.Redact(string) 已交付。
// 运行命令：dotnet test tests/windows/win-diagnostics -c Release
// 覆盖 acceptance：A4
// 待 CI：本机不装 .NET，未在本机运行；由 GitHub Actions windows-latest 执行。
using OsakaConnect.Diagnostics;
using Xunit;

public sealed class LogRedactorTests
{
    [ProductFact]
    public void A4_AllRedactionVectorsAreMasked()
    {
        var vectors = SharedVectors.LoadRedaction();
        Assert.NotEmpty(vectors.Cases);

        for (var i = 0; i < vectors.Cases.Count; i++)
        {
            var item = vectors.Cases[i];
            var redacted = LogRedactor.Redact(item.In);

            Assert.NotNull(redacted);
            Assert.Equal(item.Out, redacted);
        }
    }
}
