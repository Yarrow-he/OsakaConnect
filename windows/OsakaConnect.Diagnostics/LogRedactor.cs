using System.Text.RegularExpressions;

namespace OsakaConnect.Diagnostics;

public static class LogRedactor
{
    private static readonly Regex SensitiveValue = new(
        """(?<prefix>\b(?:token|invite|api_key|password)["']?[ \t]*[:=][ \t]*(?:Bearer[ \t]+)?|\bBearer[ \t]+)(?<value>"[^"\r\n]*"?|'[^'\r\n]*'?|[^\s,;&}\]"']+)""",
        RegexOptions.IgnoreCase | RegexOptions.CultureInvariant | RegexOptions.NonBacktracking);

    public static string Redact(string text)
    {
        return SensitiveValue.Replace(text, match =>
        {
            var value = match.Groups["value"].Value;
            var mask = "***";
            if (value.Length >= 2 && value[0] is '"' or '\'' && value[^1] == value[0])
                mask = value[0] + mask + value[^1];

            return match.Groups["prefix"].Value + mask;
        });
    }
}
