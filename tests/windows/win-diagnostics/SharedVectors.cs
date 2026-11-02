// 用途：按相对仓库根路径读取 shared/errors.json 与 shared/test-vectors/*.json。
// 前置环境：Windows + .NET 8 SDK；仓库结构保持 shared/ 在根目录。
// 运行命令：dotnet test tests/windows/win-diagnostics -c Release
// 覆盖 acceptance：A3（errors.json）、A4（redaction.json）、A15（invite-code.json）。
using System.Text.Json;
using System.Text.Json.Serialization;

internal static class SharedVectors
{
    private static readonly JsonSerializerOptions Options = new() { PropertyNameCaseInsensitive = true };

    private static string RepoRoot()
    {
        for (DirectoryInfo? dir = new(AppContext.BaseDirectory); dir is not null; dir = dir.Parent)
        {
            if (Directory.Exists(Path.Combine(dir.FullName, "shared", "test-vectors")))
                return dir.FullName;
        }

        throw new InvalidOperationException("找不到仓库根：shared/test-vectors 不存在");
    }

    internal static Dictionary<string, ErrorEntry> LoadErrors() =>
        JsonSerializer.Deserialize<Dictionary<string, ErrorEntry>>(
            File.ReadAllText(Path.Combine(RepoRoot(), "shared", "errors.json")), Options)
        ?? throw new InvalidOperationException("shared/errors.json 解析为空");

    internal static RedactionVectors LoadRedaction() =>
        JsonSerializer.Deserialize<RedactionVectors>(
            File.ReadAllText(Path.Combine(RepoRoot(), "shared", "test-vectors", "redaction.json")), Options)
        ?? throw new InvalidOperationException("redaction.json 解析为空");

    internal static InviteCodeVectors LoadInviteCodes() =>
        JsonSerializer.Deserialize<InviteCodeVectors>(
            File.ReadAllText(Path.Combine(RepoRoot(), "shared", "test-vectors", "invite-code.json")), Options)
        ?? throw new InvalidOperationException("invite-code.json 解析为空");
}

internal sealed class ErrorEntry
{
    public int Http { get; set; }
    public string Zh { get; set; } = "";
}

internal sealed class RedactionVectors
{
    [JsonPropertyName("mask")] public string Mask { get; set; } = "";
    [JsonPropertyName("cases")] public List<RedactionCase> Cases { get; set; } = new();
}

internal sealed class RedactionCase
{
    [JsonPropertyName("in")] public string In { get; set; } = "";
    [JsonPropertyName("out")] public string Out { get; set; } = "";
}

internal sealed class InviteCodeVectors
{
    [JsonPropertyName("valid")] public List<string?> Valid { get; set; } = new();
    [JsonPropertyName("invalid")] public List<string?> Invalid { get; set; } = new();
}
