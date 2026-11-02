using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Text.RegularExpressions;

namespace OsakaAdmin;

public sealed record HealthResponse
{
    public required string Status { get; init; }
    public required Route[] Routes { get; init; }
}

public sealed record Route
{
    public required string Id { get; init; }
    [JsonPropertyName("display_name")]
    public required string DisplayName { get; init; }
}

public sealed record VersionResponse
{
    public required string Service { get; init; }
    public required string Version { get; init; }
    public required string Protocol { get; init; }
}

public sealed record ErrorBody
{
    public required string Code { get; init; }
    public required string Message { get; init; }
}

public static class AdminContract
{
    private static readonly JsonSerializerOptions Options = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower,
        UnmappedMemberHandling = JsonUnmappedMemberHandling.Disallow
    };

    public static T Parse<T>(string json) where T : class =>
        Validate(JsonSerializer.Deserialize<T>(json, Options));

    public static T Parse<T>(ReadOnlySpan<byte> json) where T : class =>
        Validate(JsonSerializer.Deserialize<T>(json, Options));

    private static T Validate<T>(T? value) where T : class
    {
        var valid = value switch
        {
            HealthResponse health => (health.Status is "ok" or "degraded") &&
                health.Routes is { Length: >= 1 } && health.Routes.All(ValidRoute),
            Route route => ValidRoute(route),
            VersionResponse version => version.Service == "osaka-server" &&
                version.Protocol == "admin.v1" && version.Version is not null &&
                Regex.IsMatch(version.Version, @"^\d+\.\d+\.\d+$", RegexOptions.ECMAScript,
                    TimeSpan.FromSeconds(1)),
            ErrorBody error => (error.Code is "UNAUTHENTICATED" or "FORBIDDEN" or
                "INVALID_INVITE_CODE" or "BACKEND_UNREACHABLE" or "INTERNAL") &&
                HasLength(error.Message, 0, 200),
            _ => false
        };
        if (!valid)
            throw new JsonException("响应不符合管理接口契约。");
        return value!;
    }

    private static bool ValidRoute(Route? route) => route is not null &&
        HasLength(route.Id, 1, 64) && HasLength(route.DisplayName, 1, 64);

    private static bool HasLength(string? value, int min, int max)
    {
        if (value is null)
            return false;
        var length = value.EnumerateRunes().Count();
        return length >= min && length <= max;
    }
}
