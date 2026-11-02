using System.Net;
using System.Security.Authentication;
using System.Text.Json;
using OsakaAdmin;
using Xunit;

// Probe tests temporarily set the process environment, then restore it.
[assembly: CollectionBehavior(DisableTestParallelization = true)]

namespace OsakaAdmin.Tests;

public sealed class AdminContractTests : IDisposable
{
    private readonly string? previousToken = Environment.GetEnvironmentVariable("OSAKA_ADMIN_TOKEN");

    public AdminContractTests() => Environment.SetEnvironmentVariable("OSAKA_ADMIN_TOKEN", "test-token-fake");

    public void Dispose() => Environment.SetEnvironmentVariable("OSAKA_ADMIN_TOKEN", previousToken);

    public static IEnumerable<object[]> ContractVectors()
    {
        using var vectors = ReadShared("test-vectors", "admin-contract.json");
        foreach (var schema in vectors.RootElement.EnumerateObject())
        {
            if (schema.Name.StartsWith('_'))
                continue;
            foreach (var group in schema.Value.EnumerateObject())
            {
                if (group.Name.StartsWith('_'))
                    continue;
                Assert.Contains(group.Name, new[] { "valid", "invalid" });
                foreach (var sample in group.Value.EnumerateArray())
                    yield return new object[] { schema.Name, group.Name == "valid", sample.GetRawText() };
            }
        }
    }

    [Theory]
    [MemberData(nameof(ContractVectors))]
    public void SharedContractVectors(string schema, bool valid, string json)
    {
        var error = Record.Exception(() => Parse(schema, json));
        if (valid)
            Assert.Null(error);
        else
            Assert.IsType<JsonException>(error);
    }

    [Theory]
    [InlineData("HealthResponse", "null")]
    [InlineData("HealthResponse", "[]")]
    [InlineData("HealthResponse", "{\"status\":null,\"routes\":[]}")]
    [InlineData("HealthResponse", "{\"status\":\"ok\",\"routes\":null}")]
    [InlineData("HealthResponse", "{\"status\":\"ok\",\"routes\":[null]}")]
    [InlineData("HealthResponse", "{\"status\":\"ok\",\"routes\":[{\"id\":null,\"display_name\":\"x\"}]}")]
    [InlineData("HealthResponse", "{\"status\":\"ok\",\"routes\":[{\"id\":\"r\",\"display_name\":null}]}")]
    [InlineData("HealthResponse", "{\"status\":\"ok\",\"routes\":[{\"id\":\"r\"}]}")]
    [InlineData("HealthResponse", "{\"status\":\"ok\",\"routes\":[{\"id\":\"r\",\"display_name\":\"x\",\"extra\":true}]}")]
    [InlineData("VersionResponse", "{\"service\":\"osaka-server\",\"version\":null,\"protocol\":\"admin.v1\"}")]
    [InlineData("VersionResponse", "{\"service\":\"osaka-server\",\"version\":123,\"protocol\":\"admin.v1\"}")]
    [InlineData("ErrorBody", "{\"code\":\"INTERNAL\",\"message\":null}")]
    [InlineData("ErrorBody", "{\"code\":null,\"message\":\"x\"}")]
    [InlineData("ErrorBody", "{")]
    public void RejectsNullWrongTypesAndNestedUnknownFields(string schema, string json) =>
        Assert.Throws<JsonException>(() => Parse(schema, json));

    [Theory]
    [InlineData(0, false)]
    [InlineData(1, true)]
    [InlineData(64, true)]
    [InlineData(65, false)]
    public void RouteLengthCountsUnicodeCharacters(int length, bool valid)
    {
        var value = string.Concat(Enumerable.Repeat("😀", length));
        foreach (var route in new[] { new { id = value, display_name = "x" }, new { id = "r", display_name = value } })
        {
            var json = JsonSerializer.Serialize(new { status = "degraded", routes = new[] { route } });
            var error = Record.Exception(() => AdminContract.Parse<HealthResponse>(json));
            if (valid)
                Assert.Null(error);
            else
                Assert.IsType<JsonException>(error);
        }
    }

    [Theory]
    [InlineData(0, true)]
    [InlineData(200, true)]
    [InlineData(201, false)]
    public void ErrorMessageLengthBoundary(int length, bool valid)
    {
        var json = JsonSerializer.Serialize(new { code = "INTERNAL", message = new string('x', length) });
        var error = Record.Exception(() => AdminContract.Parse<ErrorBody>(json));
        if (valid)
            Assert.Null(error);
        else
            Assert.IsType<JsonException>(error);
    }

    [Fact]
    public async Task SuccessRequiresBothEndpointsAndSendsBearerHeader()
    {
        var paths = new List<string>();
        using var probe = new AdminProbe(new StubHandler(request =>
        {
            Assert.Equal(HttpMethod.Get, request.Method);
            Assert.Equal("https", request.RequestUri!.Scheme);
            Assert.Equal("backend.invalid", request.RequestUri.Host);
            Assert.Equal("Bearer", request.Headers.Authorization!.Scheme);
            Assert.Equal("test-token-fake", request.Headers.Authorization.Parameter);
            paths.Add(request.RequestUri.AbsolutePath);
            return ValidResponse(request);
        }));

        var result = await probe.ProbeAsync("https://backend.invalid");

        Assert.Equal(ProbeStatus.Success, result.Status);
        Assert.NotNull(result.Version);
        Assert.NotNull(result.Health);
        Assert.Equal(new[] { "/v1/version", "/v1/health" }, paths);
    }

    [Theory]
    [InlineData(401, "/v1/version", "UNAUTHENTICATED")]
    [InlineData(403, "/v1/version", "FORBIDDEN")]
    [InlineData(401, "/v1/health", "UNAUTHENTICATED")]
    [InlineData(403, "/v1/health", "FORBIDDEN")]
    public async Task RejectionUsesStatusAndSharedMessageEvenWithInvalidBody(int status, string path, string code)
    {
        using var probe = new AdminProbe(new StubHandler(request => request.RequestUri!.AbsolutePath == path
            ? new HttpResponseMessage((HttpStatusCode)status) { Content = new StringContent("untrusted fake stack") }
            : ValidResponse(request)));

        var result = await probe.ProbeAsync("https://backend.invalid");

        AssertFailure(result, ProbeStatus.Rejected);
        using var errors = ReadShared("errors.json");
        Assert.Equal(errors.RootElement.GetProperty(code).GetProperty("zh").GetString(), result.Message);
    }

    [Theory]
    [InlineData("/v1/version")]
    [InlineData("/v1/health")]
    public async Task InvalidSchemaNeverReturnsConnectedData(string path)
    {
        using var probe = new AdminProbe(new StubHandler(request => request.RequestUri!.AbsolutePath == path
            ? new HttpResponseMessage(HttpStatusCode.OK) { Content = new StringContent("{}") }
            : ValidResponse(request)));
        AssertFailure(await probe.ProbeAsync("https://backend.invalid"), ProbeStatus.SchemaFailure);
    }

    [Fact]
    public async Task MalformedUtf8IsRejected()
    {
        var json = System.Text.Encoding.UTF8.GetBytes(
            "{\"service\":\"osaka-server\",\"version\":\"0.1.0\",\"protocol\":\"admin.v1\"}");
        json[2] = 0xff;
        using var probe = new AdminProbe(new StubHandler(_ => new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new ByteArrayContent(json)
        }));
        AssertFailure(await probe.ProbeAsync("https://backend.invalid"), ProbeStatus.SchemaFailure);
    }

    [Theory]
    [InlineData(204)]
    [InlineData(302)]
    [InlineData(500)]
    public async Task UnexpectedHttpStatusIsNotSuccess(int status)
    {
        using var probe = new AdminProbe(new StubHandler(_ => new HttpResponseMessage((HttpStatusCode)status)));
        AssertFailure(await probe.ProbeAsync("https://backend.invalid"), ProbeStatus.SchemaFailure);
    }

    [Theory]
    [InlineData("tls", ProbeStatus.TlsFailure)]
    [InlineData("hostname", ProbeStatus.TlsFailure)]
    [InlineData("network", ProbeStatus.NetworkFailure)]
    [InlineData("timeout", ProbeStatus.NetworkFailure)]
    public async Task TransportFailureIsClassifiedWithoutExceptionDetails(string kind, ProbeStatus expected)
    {
        Exception error = kind switch
        {
            "tls" => new HttpRequestException(HttpRequestError.SecureConnectionError, "fake private detail"),
            "hostname" => new HttpRequestException("fake private detail", new AuthenticationException("fake mismatch")),
            "timeout" => new TaskCanceledException("fake private detail"),
            _ => new HttpRequestException(HttpRequestError.NameResolutionError, "fake private detail")
        };
        using var probe = new AdminProbe(new StubHandler(_ => throw error));
        var result = await probe.ProbeAsync("https://backend.invalid");
        AssertFailure(result, expected);
        Assert.DoesNotContain("fake", result.Message);
    }

    [Theory]
    [InlineData("")]
    [InlineData("not a URL")]
    [InlineData("http://backend.invalid")]
    [InlineData("https://fake:fake@backend.invalid")]
    [InlineData("https://backend.invalid?token=fake")]
    [InlineData("https://backend.invalid#fragment")]
    public async Task InvalidAddressNeverSendsRequest(string address)
    {
        using var probe = NoRequestsProbe();
        AssertFailure(await probe.ProbeAsync(address), ProbeStatus.UsageError);
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData(" ")]
    [InlineData("test-token-fake\r\nX-Fake: fake")]
    public async Task MissingOrInvalidTokenNeverSendsRequest(string? token)
    {
        Environment.SetEnvironmentVariable("OSAKA_ADMIN_TOKEN", token);
        using var probe = NoRequestsProbe();
        AssertFailure(await probe.ProbeAsync("https://backend.invalid"), ProbeStatus.UsageError);
    }

    private static AdminProbe NoRequestsProbe() => new(new StubHandler(_ =>
        throw new InvalidOperationException("No request should be sent.")));

    private static void AssertFailure(ProbeResult result, ProbeStatus expected)
    {
        Assert.Equal(expected, result.Status);
        Assert.Null(result.Version);
        Assert.Null(result.Health);
        Assert.DoesNotContain("已连接", result.Message);
        Assert.DoesNotContain("test-token-fake", result.Message);
    }

    private static HttpResponseMessage ValidResponse(HttpRequestMessage request)
    {
        var schema = request.RequestUri!.AbsolutePath switch
        {
            "/v1/version" => "VersionResponse",
            "/v1/health" => "HealthResponse",
            _ => throw new InvalidOperationException("Unexpected endpoint.")
        };
        using var vectors = ReadShared("test-vectors", "admin-contract.json");
        return new(HttpStatusCode.OK)
        {
            Content = new StringContent(vectors.RootElement.GetProperty(schema).GetProperty("valid")[0].GetRawText())
        };
    }

    private static JsonDocument ReadShared(params string[] path) =>
        JsonDocument.Parse(File.ReadAllText(Path.Combine(new[] { AppContext.BaseDirectory, "shared" }.Concat(path).ToArray())));

    private static object Parse(string schema, string json) => schema switch
    {
        "HealthResponse" => AdminContract.Parse<HealthResponse>(json),
        "VersionResponse" => AdminContract.Parse<VersionResponse>(json),
        "ErrorBody" => AdminContract.Parse<ErrorBody>(json),
        _ => throw new InvalidOperationException($"Unknown schema: {schema}")
    };

    private sealed class StubHandler(Func<HttpRequestMessage, HttpResponseMessage> respond) : HttpMessageHandler
    {
        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken) =>
            Task.FromResult(respond(request));
    }
}
