using System;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Threading;
using Xunit;

namespace OsakaConnect.Tests;

[CollectionDefinition("WPF")]
public sealed class WpfTestCollection : ICollectionFixture<WpfTestHost>
{
}

public sealed class WpfTestHost : IDisposable
{
    private readonly TaskCompletionSource<Dispatcher> ready = new(TaskCreationOptions.RunContinuationsAsynchronously);
    private readonly CancellationTokenSource shutdown = new();
    private readonly Lazy<Thread> thread;

    public WpfTestHost()
    {
        thread = new Lazy<Thread>(() =>
        {
            var worker = new Thread(Run) { IsBackground = true };
            worker.SetApartmentState(ApartmentState.STA);
            worker.Start();
            return worker;
        });
    }

    public async Task InvokeAsync(Action action)
    {
        _ = thread.Value;
        var dispatcher = await ready.Task.ConfigureAwait(false);
        await dispatcher.InvokeAsync(action).Task.ConfigureAwait(false);
    }

    private void Run()
    {
        try
        {
            var dispatcher = Dispatcher.CurrentDispatcher;
            using var registration = shutdown.Token.Register(() =>
                dispatcher.BeginInvoke(DispatcherPriority.Send, new Action(dispatcher.InvokeShutdown)));
            _ = new Application { ShutdownMode = ShutdownMode.OnExplicitShutdown };
            ready.SetResult(dispatcher);
            Dispatcher.Run();
        }
        catch (Exception exception)
        {
            ready.TrySetException(exception);
        }
    }

    public void Dispose()
    {
        shutdown.Cancel();
        if (thread.IsValueCreated && !thread.Value.Join(TimeSpan.FromSeconds(10)))
        {
            throw new TimeoutException("WPF test host shutdown timed out.");
        }
        shutdown.Dispose();
    }
}
