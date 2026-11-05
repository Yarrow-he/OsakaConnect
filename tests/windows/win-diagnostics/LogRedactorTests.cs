using OsakaConnect.Diagnostics;
using Xunit;

public sealed class LogRedactorTests
{
    [Fact]
    public void A4_AllRedactionVectorsAreMasked()
    {
        var vectors = SharedVectors.LoadRedaction();
        Assert.NotEmpty(vectors.Cases);

        foreach (var item in vectors.Cases)
            Assert.Equal(item.Out, LogRedactor.Redact(item.In));
    }
}
