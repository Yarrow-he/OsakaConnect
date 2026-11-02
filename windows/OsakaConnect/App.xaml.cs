using System;
using System.Windows;
using System.Windows.Automation;

namespace OsakaConnect;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);

        if (Array.IndexOf(e.Args, "--self-test") >= 0)
        {
            var exitCode = 1;
            try
            {
                var connection = new ConnectionPage();
                var settings = new SettingsPage();
                if (AutomationProperties.GetAutomationId(connection) == "W1" &&
                    AutomationProperties.GetAutomationId(settings) == "W4")
                {
                    exitCode = 0;
                }
            }
            catch (Exception)
            {
                // Construction failures must also produce the self-test failure exit code.
                exitCode = 1;
            }

            Shutdown(exitCode);
            return;
        }

        MainWindow = new MainWindow();
        MainWindow.Show();
    }
}
