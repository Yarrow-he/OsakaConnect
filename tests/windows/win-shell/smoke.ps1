# 用途：win-shell（Windows .NET 8 WPF）黑盒冒烟与静态约束测试。
#   A1  --self-test 启动自检退出码 0。
#   A2/A16 正常启动后点导航“连接/设置”，断言页面标识 W1/W4 可见。
#   A14 windows/ 下只允许 *.csproj/*.cs/*.xaml。
#   A17 windows/ 源码不含线路字面量 “Line 01” / “OSA · 01”。
# 前置环境：Windows + PowerShell 7；.NET 8 产物已由 CI 构建（本脚本不构建）。
# 运行命令：pwsh -NoProfile -File tests/windows/win-shell/smoke.ps1
# 覆盖 acceptance：A1、A2、A16、A14、A17。
# 待 CI：本机无 .NET/桌面会话，未运行；目标 GitHub Actions windows-latest。
# 产品未交付时输出“待测：模块未交付”并退出 3（CI 红）。

$ErrorActionPreference = 'Stop'
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$WindowsRoot = Join-Path $RepoRoot 'windows'

# ---- 待写手交付后对齐 ----
# 入口 exe；可用 OSAKA_WIN_EXE 覆盖。留空时按 csproj 的 AssemblyName 在 bin/ 下自动查找。
$ExePath = if ($env:OSAKA_WIN_EXE) { $env:OSAKA_WIN_EXE } else { '' }
# 主窗口标题；留空则只等主窗口出现，不校验标题。
$WindowTitle = if ($env:OSAKA_WIN_WINDOW_TITLE) { $env:OSAKA_WIN_WINDOW_TITLE } else { '' }
# 导航项显示名 → 设计稿页标识（design/inventory.md：W1、W4）。
# 约定页标识出现在该页某个元素的 AutomationId 或 Name 上；若写手用其他方式暴露，改这里。
$NavPages = @(
    @{ Name = '连接'; PageId = 'W1' },
    @{ Name = '设置'; PageId = 'W4' }
)

$failures = [System.Collections.Generic.List[string]]::new()
function Fail($Message) { $script:failures.Add($Message); Write-Host "FAIL: $Message" -ForegroundColor Red }
function Pass($Message) { Write-Host "PASS: $Message" -ForegroundColor Green }

if (-not (Test-Path -LiteralPath $WindowsRoot -PathType Container)) {
    Write-Host '待测：模块未交付（windows/ 目录不存在）'
    exit 3
}

# A14：只允许 *.csproj/*.cs/*.xaml。
# shortcut: 跳过 bin/obj（构建产物），CI 在构建后扫描源码树。
$sourceFiles = Get-ChildItem -LiteralPath $WindowsRoot -Recurse -File |
    Where-Object { $_.FullName -notmatch '[\\/](bin|obj)[\\/]' }
$allowedExtensions = @('.csproj', '.cs', '.xaml')
$badFiles = $sourceFiles | Where-Object { $allowedExtensions -notcontains $_.Extension.ToLowerInvariant() }
if ($badFiles) {
    $badFiles | ForEach-Object { Fail "A14 非允许文件：$($_.FullName.Substring($RepoRoot.Length + 1))" }
} else {
    Pass "A14 windows/ 只含 *.csproj/*.cs/*.xaml（$($sourceFiles.Count) 个文件）"
}

# A17：源码不含线路字面量（资源文件不在本任务范围）。
$hits = $sourceFiles |
    Where-Object { $_.Extension.ToLowerInvariant() -in @('.cs', '.xaml') } |
    Select-String -SimpleMatch -Pattern 'Line 01', 'OSA · 01'
if ($hits) {
    $hits | ForEach-Object { Fail "A17 线路字面量：$($_.Path.Substring($RepoRoot.Length + 1)):$($_.LineNumber): $($_.Line.Trim())" }
} else {
    Pass 'A17 windows/ 源码不含 “Line 01” / “OSA · 01”'
}

function Resolve-Exe {
    if ($ExePath) {
        if (Test-Path -LiteralPath $ExePath -PathType Leaf) { return (Resolve-Path -LiteralPath $ExePath).Path }
        return $null
    }
    $projects = Get-ChildItem -LiteralPath $WindowsRoot -Recurse -File -Filter *.csproj |
        Where-Object { $_.FullName -notmatch '[\\/](bin|obj)[\\/]' }
    foreach ($project in $projects) {
        $name = $project.BaseName
        $assembly = Select-String -LiteralPath $project.FullName -Pattern '<AssemblyName>\s*([^<]+)' | Select-Object -First 1
        if ($assembly) { $name = $assembly.Matches[0].Groups[1].Value.Trim() }
        $candidate = Get-ChildItem -LiteralPath $WindowsRoot -Recurse -File -Filter "$name.exe" -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match '[\\/]bin[\\/]' } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if ($candidate) { return $candidate.FullName }
    }
    return $null
}

$exe = Resolve-Exe
if (-not $exe) {
    Fail 'A1/A2/A16 未找到入口 exe（CI 未构建或需用 OSAKA_WIN_EXE 对齐路径）'
} else {
    Write-Host "入口：$exe"

    # A1：--self-test 30s 内退出且退出码 0。
    $selfTest = Start-Process -FilePath $exe -ArgumentList '--self-test' -WorkingDirectory (Split-Path $exe) -PassThru
    if (-not $selfTest.WaitForExit(30000)) {
        $selfTest.Kill()
        Fail 'A1 --self-test 30s 未退出'
    } elseif ($selfTest.ExitCode -ne 0) {
        Fail "A1 --self-test 退出码 $($selfTest.ExitCode)"
    } else {
        Pass 'A1 --self-test 退出码 0'
    }

    # A2/A16：正常启动 → 逐个点导航项 → 断言页标识可见。
    Add-Type -AssemblyName UIAutomationClient
    Add-Type -AssemblyName UIAutomationTypes

    function Find-Element($Root, $AutomationId, $ElementName) {
        $conditions = @()
        if ($AutomationId) {
            $conditions += [System.Windows.Automation.PropertyCondition]::new(
                [System.Windows.Automation.AutomationElement]::AutomationIdProperty, $AutomationId)
        }
        if ($ElementName) {
            $conditions += [System.Windows.Automation.PropertyCondition]::new(
                [System.Windows.Automation.AutomationElement]::NameProperty, $ElementName)
        }
        if ($conditions.Count -eq 0) { return $null }
        $condition = if ($conditions.Count -eq 1) {
            $conditions[0]
        } else {
            [System.Windows.Automation.OrCondition]::new([System.Windows.Automation.Condition[]]$conditions)
        }
        return $Root.FindFirst([System.Windows.Automation.TreeScope]::Descendants, $condition)
    }

    function Wait-Element($Root, $AutomationId, $ElementName, $TimeoutSec = 10) {
        $deadline = (Get-Date).AddSeconds($TimeoutSec)
        do {
            $element = Find-Element $Root $AutomationId $ElementName
            if ($element -and -not $element.Current.IsOffscreen) { return $element }
            Start-Sleep -Milliseconds 200
        } while ((Get-Date) -lt $deadline)
        return $null
    }

    $app = Start-Process -FilePath $exe -WorkingDirectory (Split-Path $exe) -PassThru
    try {
        $deadline = (Get-Date).AddSeconds(30)
        while ($app.MainWindowHandle -eq [IntPtr]::Zero -and -not $app.HasExited -and (Get-Date) -lt $deadline) {
            Start-Sleep -Milliseconds 200
            $app.Refresh()
        }
        if ($app.HasExited) {
            Fail "A2/A16 正常启动后进程退出，退出码 $($app.ExitCode)"
        } elseif ($app.MainWindowHandle -eq [IntPtr]::Zero) {
            Fail 'A2/A16 30s 内未出现主窗口'
        } else {
            $window = [System.Windows.Automation.AutomationElement]::FromHandle($app.MainWindowHandle)
            if ($WindowTitle -and $window.Current.Name -ne $WindowTitle) {
                Fail "A2/A16 窗口标题为“$($window.Current.Name)”，期望“$WindowTitle”"
            }
            foreach ($page in $NavPages) {
                $nav = Wait-Element $window $null $page.Name
                if (-not $nav) { Fail "A2/A16 未找到导航项“$($page.Name)”"; continue }

                $clicked = $false
                try {
                    $nav.GetCurrentPattern([System.Windows.Automation.InvokePattern]::Pattern).Invoke()
                    $clicked = $true
                } catch {
                    try {
                        $nav.GetCurrentPattern([System.Windows.Automation.SelectionItemPattern]::Pattern).Select()
                        $clicked = $true
                    } catch { }
                }
                if (-not $clicked) { Fail "A2/A16 导航项“$($page.Name)”不支持 Invoke/Select"; continue }

                $marker = Wait-Element $window $page.PageId $page.PageId
                if ($marker) {
                    Pass "A2/A16 页面 $($page.PageId)（$($page.Name)）标识可见"
                } else {
                    Fail "A2/A16 切换到“$($page.Name)”后未见页标识 $($page.PageId)"
                }
            }
        }
    } finally {
        if (-not $app.HasExited) { $app.Kill() }
    }
}

if ($failures.Count -gt 0) {
    Write-Host "`n失败 $($failures.Count) 项："
    $failures | ForEach-Object { Write-Host " - $_" }
    exit 1
}
Write-Host '全部通过。'
exit 0
