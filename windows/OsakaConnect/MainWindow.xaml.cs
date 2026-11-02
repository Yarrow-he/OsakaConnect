using System.Windows;
using System.Windows.Controls;

namespace OsakaConnect;

public partial class MainWindow : Window
{
    public MainWindow()
    {
        InitializeComponent();
        Navigation.SelectedIndex = 0;
    }

    private void Navigation_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        PageHost.Content = Navigation.SelectedIndex switch
        {
            0 => new ConnectionPage(),
            1 => new SettingsPage(),
            _ => null
        };
    }
}
