using System.Text.Json;

namespace OsakaConnect.Diagnostics;

public sealed class ErrorPresenter
{
    private const string DefaultInternalMessage = "发生内部错误，请稍后重试";
    private readonly Dictionary<string, string> messages = new(StringComparer.Ordinal);

    public ErrorPresenter(string errorsJson)
    {
        try
        {
            using var document = JsonDocument.Parse(errorsJson);
            foreach (var error in document.RootElement.EnumerateObject())
            {
                var message = error.Value.GetProperty("zh").GetString();
                if (string.IsNullOrWhiteSpace(message))
                    throw new JsonException();

                messages.Add(error.Name, message);
            }

            if (!messages.ContainsKey("INTERNAL"))
                messages.Clear();
        }
        catch (Exception)
        {
            // A broken catalog must never turn exception details into UI text.
            messages.Clear();
        }
    }

    public string GetMessage(string? code = null, Exception? exception = null)
    {
        if (exception is null && code is not null && messages.TryGetValue(code, out var message))
            return message;

        return messages.TryGetValue("INTERNAL", out var fallback) ? fallback : DefaultInternalMessage;
    }
}
