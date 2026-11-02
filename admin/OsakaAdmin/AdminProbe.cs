using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Security.Authentication;
using System.Text.Json;
using System.Text.RegularExpressions;

namespace OsakaAdmin;

public enum ProbeStatus
{
    Success = 0,
    UsageError = 1,
    Rejected = 2,
    TlsFailure = 3,
    NetworkFailure = 4,
    SchemaFailure = 5
}

public sealed record ProbeResult(ProbeStatus Status, string Message,
    VersionResponse? Version = null, HealthResponse? Health = null)
{
    public static string FormatSummary(VersionResponse version, HealthResponse health) =>
        $"version={version.Version} health={health.Status} routes={health.Routes.Length}";
}

public sealed class AdminProbe : IDisposable
{
    private readonly HttpClient client;

    public AdminProbe(HttpMessageHandler? handler = null)
    {
        // Redirects must not downgrade HTTPS or move an authenticated probe to another endpoint.
        client = new HttpClient(handler ?? new HttpClientHandler { AllowAutoRedirect = false })
        {
            Timeout = TimeSpan.FromSeconds(15)
        };
    }

    public async Task<ProbeResult> ProbeAsync(string address)
    {
        if (!Uri.TryCreate(address, UriKind.Absolute, out var backend) ||
            backend.Scheme != Uri.UriSchemeHttps || string.IsNullOrEmpty(backend.Host) ||
            backend.UserInfo.Length != 0 || backend.Query.Length != 0 || backend.Fragment.Length != 0)
            return new(ProbeStatus.UsageError, "请输入有效的 HTTPS 后台地址（不含凭证、查询参数或片段）。");

        var token = Environment.GetEnvironmentVariable("OSAKA_ADMIN_TOKEN");
        if (string.IsNullOrWhiteSpace(token))
            return new(ProbeStatus.UsageError, ErrorText("UNAUTHENTICATED"));
        if (token.Any(c => char.IsControl(c) || c > 0x7e))
            return new(ProbeStatus.UsageError, "管理凭证环境变量格式无效。");

        try
        {
            var versionResult = await GetAsync<VersionResponse>(backend, "/v1/version", token);
            if (versionResult.Error is not null)
                return versionResult.Error;
            var healthResult = await GetAsync<HealthResponse>(backend, "/v1/health", token);
            if (healthResult.Error is not null)
                return healthResult.Error;
            return new(ProbeStatus.Success, "已连接", versionResult.Value, healthResult.Value);
        }
        catch (HttpRequestException error) when (error.HttpRequestError == HttpRequestError.SecureConnectionError ||
                                               HasAuthenticationFailure(error))
        {
            return new(ProbeStatus.TlsFailure, "TLS 连接或证书校验失败，请检查证书及后台地址。");
        }
        catch (AuthenticationException)
        {
            return new(ProbeStatus.TlsFailure, "TLS 连接或证书校验失败，请检查证书及后台地址。");
        }
        catch (Exception error) when (error is HttpRequestException or IOException or OperationCanceledException)
        {
            return new(ProbeStatus.NetworkFailure, ErrorText("BACKEND_UNREACHABLE"));
        }
        catch (Exception error) when (error is JsonException or RegexMatchTimeoutException)
        {
            return new(ProbeStatus.SchemaFailure, "后台响应不符合管理接口契约。");
        }
        catch (FormatException)
        {
            return new(ProbeStatus.UsageError, "管理凭证环境变量格式无效。");
        }
    }

    private async Task<(T? Value, ProbeResult? Error)> GetAsync<T>(Uri backend, string path, string token)
        where T : class
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, new Uri(backend, path));
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        using var response = await client.SendAsync(request);
        if (response.StatusCode is HttpStatusCode.Unauthorized or HttpStatusCode.Forbidden)
            return (null, new(ProbeStatus.Rejected, ErrorText(
                response.StatusCode == HttpStatusCode.Unauthorized ? "UNAUTHENTICATED" : "FORBIDDEN")));
        if (response.StatusCode != HttpStatusCode.OK)
            return (null, new(ProbeStatus.SchemaFailure, "后台响应不符合管理接口契约。"));
        var json = await response.Content.ReadAsByteArrayAsync();
        return (AdminContract.Parse<T>(json), null);
    }

    private static bool HasAuthenticationFailure(Exception error)
    {
        for (Exception? current = error; current is not null; current = current.InnerException)
            if (current is AuthenticationException)
                return true;
        return false;
    }

    private static string ErrorText(string code)
    {
        using var stream = typeof(AdminProbe).Assembly.GetManifestResourceStream("OsakaAdmin.Errors.json")!;
        using var errors = JsonDocument.Parse(stream);
        return errors.RootElement.GetProperty(code).GetProperty("zh").GetString()!;
    }

    public void Dispose() => client.Dispose();
}
