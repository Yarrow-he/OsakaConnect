using System;
using System.Runtime.ExceptionServices;
using System.Threading;
using System.Windows;
using System.Windows.Automation;
using System.Windows.Controls;
using Xunit;

namespace OsakaConnect.Tests;

public class NavigationTests
{
    [Fact]
    public void SidebarSwitchesBetweenConnectionAndSettings()
    {
        Exception? failure = null;
        var thread = new Thread(() =>
        {
            try
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
            }
            catch (Exception exception)
            {
                failure = exception;
            }
        }) { IsBackground = true };

        thread.SetApartmentState(ApartmentState.STA);
        thread.Start();
        Assert.True(thread.Join(TimeSpan.FromSeconds(10)), "WPF navigation test timed out.");
        if (failure is not null)
        {
            ExceptionDispatchInfo.Capture(failure).Throw();
        }
    }
}
