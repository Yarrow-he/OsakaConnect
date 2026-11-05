using System;
using System.Threading.Tasks;
using System.Windows.Automation;
using System.Windows.Controls;
using Xunit;

namespace OsakaConnect.Tests;

[Collection("WPF")]
public class NavigationTests
{
    private readonly WpfTestHost host;

    public NavigationTests(WpfTestHost host)
    {
        this.host = host;
    }

    [Fact]
    public async Task SidebarSwitchesBetweenConnectionAndSettings()
    {
        var timeout = Task.Delay(TimeSpan.FromSeconds(10));
        var execution = host.InvokeAsync(() =>
        {
            var window = new MainWindow();
            try
            {
                var navigation = Assert.IsType<ListBox>(window.FindName("Navigation"));
                var host = Assert.IsType<ContentControl>(window.FindName("PageHost"));
                Assert.Equal(2, navigation.Items.Count);
                Assert.Equal("连接", Assert.IsType<ListBoxItem>(navigation.Items[0]).Content);
                Assert.Equal("设置", Assert.IsType<ListBoxItem>(navigation.Items[1]).Content);

                Assert.Equal(0, navigation.SelectedIndex);
                Assert.Equal("W1", AutomationProperties.GetAutomationId(
                    Assert.IsType<ConnectionPage>(host.Content)));

                navigation.SelectedIndex = 1;
                Assert.Equal("W4", AutomationProperties.GetAutomationId(
                    Assert.IsType<SettingsPage>(host.Content)));

                navigation.SelectedIndex = 0;
                Assert.Equal("W1", AutomationProperties.GetAutomationId(
                    Assert.IsType<ConnectionPage>(host.Content)));
            }
            finally
            {
                window.Close();
            }
        });

        Assert.True(await Task.WhenAny(timeout, execution) == execution, "WPF navigation test timed out.");
        await execution;
    }
}
