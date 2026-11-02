using System.Windows;

namespace OsakaAdmin;

public partial class App : Application
{
    protected override async void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        if (e.Args.Length > 0)
        {
            if (e.Args.Length != 2 || e.Args[0] != "--probe")
            {
                Shutdown((int)ProbeStatus.UsageError);
                return;
            }

            using var probe = new AdminProbe();
            var result = await probe.ProbeAsync(e.Args[1]);
            Shutdown((int)result.Status);
            return;
        }

        ShutdownMode = ShutdownMode.OnMainWindowClose;
        MainWindow = new MainWindow();
        MainWindow.Show();
    }
}
