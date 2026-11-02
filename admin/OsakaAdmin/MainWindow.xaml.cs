using System.Windows;

namespace OsakaAdmin;

public partial class MainWindow : Window
{
    public MainWindow()
    {
        InitializeComponent();
    }

    private async void Connect_Click(object sender, RoutedEventArgs e)
    {
        ConnectButton.IsEnabled = false;
        BackendAddress.IsEnabled = false;
        ResultText.Text = string.Empty;
        try
        {
            using var probe = new AdminProbe();
            var result = await probe.ProbeAsync(BackendAddress.Text);
            ResultText.Text = result.Status == ProbeStatus.Success
                ? $"已连接\n版本：{result.Version!.Version}\n健康：{result.Health!.Status}\n" +
                  string.Join("\n", result.Health.Routes.Select(route => $"{route.DisplayName}（{route.Id}）"))
                : result.Message;
        }
        finally
        {
            ConnectButton.IsEnabled = true;
            BackendAddress.IsEnabled = true;
        }
    }
}
