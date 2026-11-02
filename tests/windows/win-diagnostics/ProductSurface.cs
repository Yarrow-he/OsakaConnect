// 用途：win-diagnostics 产品入口的唯一对齐点。产品未交付时，测试在发现阶段以
//       “待测：模块未交付”跳过，而不是编译失败或伪装通过。
// 前置环境：Windows + .NET 8 SDK；产品工程已由写手交付。
// 运行命令：dotnet test tests/windows/win-diagnostics -c Release
// 覆盖 acceptance：A3、A4、A15（供各测试文件引用）。
using System.Reflection;
using Xunit;

// 待写手交付后对齐：命名空间、程序集名与入口方法名是 brief 占位值，不是最终事实。
// 写手交付后只改这里（及 WinDiagnostics.Tests.csproj 的 ProductProject）。
internal static class ProductSurface
{
    internal const string ProductAssembly = "OsakaConnect.Windows";
    internal const string Namespace = "OsakaConnect.Windows.Diagnostics";

    internal const string ErrorPresenterType = Namespace + ".ErrorPresenter";
    internal const string ErrorPresenterMethod = "Present"; // 待写手交付后对齐：错误码 → 中文文案

    internal const string LogRedactorType = Namespace + ".LogRedactor";
    internal const string LogRedactorMethod = "Redact"; // 待写手交付后对齐：日志文本 → 脱敏文本

    internal const string InviteCodeType = Namespace + ".InviteCode";
    internal const string InviteCodeIsValidMethod = "IsValid"; // brief 已给出

    internal const string NotDelivered = "待测：模块未交付";

    internal static Type? TryResolve(string fullTypeName)
    {
        foreach (var assembly in AppDomain.CurrentDomain.GetAssemblies())
        {
            if (assembly.IsDynamic)
                continue;
            if (assembly.GetType(fullTypeName, throwOnError: false) is { } found)
                return found;
        }

        try
        {
            return Assembly.Load(new AssemblyName(ProductAssembly))
                .GetType(fullTypeName, throwOnError: false);
        }
        catch (Exception)
        {
            return null;
        }
    }

    internal static Type ResolveOrThrow(string fullTypeName) =>
        TryResolve(fullTypeName)
        ?? throw new InvalidOperationException(
            $"{NotDelivered}（找不到 {fullTypeName}；请对齐 ProductSurface 顶部常量）。");

    // 入口按类型/方法名反射调用，避免在产品未交付时引用不存在的编译期符号。
    internal static object? Call(Type type, string methodName, params object?[] args)
    {
        var method = type
            .GetMethods(BindingFlags.Public | BindingFlags.Static | BindingFlags.Instance)
            .FirstOrDefault(m => m.Name == methodName && ShapeMatches(m, args))
            ?? throw new InvalidOperationException(
                $"找不到入口 {type.FullName}.{methodName}/{args.Length}；请对齐 ProductSurface 顶部常量。");

        var target = method.IsStatic ? null : Activator.CreateInstance(type);
        return method.Invoke(target, args);
    }

    private static bool ShapeMatches(MethodInfo method, object?[] args)
    {
        var parameters = method.GetParameters();
        if (parameters.Length != args.Length)
            return false;

        for (var i = 0; i < parameters.Length; i++)
        {
            var value = args[i];
            if (value is null)
            {
                if (parameters[i].ParameterType.IsValueType)
                    return false;
            }
            else if (!parameters[i].ParameterType.IsInstanceOfType(value))
            {
                return false;
            }
        }

        return true;
    }
}

// 产品类型不存在时报告 skip；存在时正常执行。
public sealed class ProductFactAttribute : FactAttribute
{
    public ProductFactAttribute(string productType)
    {
        if (ProductSurface.TryResolve(productType) is null)
            Skip = ProductSurface.NotDelivered;
    }
}
