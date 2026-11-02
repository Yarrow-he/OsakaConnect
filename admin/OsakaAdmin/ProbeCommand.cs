using System.IO;

namespace OsakaAdmin;

public static class ProbeCommand
{
    public static async Task<int> RunAsync(string[] args, TextWriter output)
    {
        if (args.Length != 2 || args[0] != "--probe")
            return (int)ProbeStatus.UsageError;

        using var probe = new AdminProbe();
        var result = await probe.ProbeAsync(args[1]);
        if (result.Status == ProbeStatus.Success)
            output.WriteLine(ProbeResult.FormatSummary(result.Version!, result.Health!));
        return (int)result.Status;
    }
}
