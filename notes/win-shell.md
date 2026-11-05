# 派活 brief：win-shell（Windows）

- owner：写手 · Windows　分支：`windows/win-shell`（从 main 拉，审阅/测试只针对该分支）
- 执行方式：用指定 CLI，**按 ponytail full 执行**；一个模块一个新会话。

## 目标
.NET 8 WPF 应用入口；侧栏导航与 连接(W1)/设置(W4) 占位页，页标识与设计稿编号一致；--self-test 启动自检退出码 0。

## 只准改的文件
- `windows/`
- `notes/win-shell.md`（追加交付证明）、`board.json`（本模块条目）、`reports/model-issues.md`（如有）

## 接口引用
无网络接口；design/inventory.md 第 2 节（仓库外 /home/box/osaka/design，只读参考）。架构见 `docs/architecture.md`（含边界 schema 校验方式）。

## 验收条目
A1, A2, A16, A14（见 `acceptance.md`）。完成标准：dotnet build/test 通过；UI 测试切换两页断言页标识 W1/W4；--self-test 退出码 0；源码无线路名字面量。

## 禁止事项
- 改清单外文件（`scripts/check-scope.sh` 会拦）；改 `.codex/`、`.claude/` 或任何规则/权限配置
- 新增依赖（未经组长批准）；跳过证书校验；硬编码线路
- 密钥、令牌、私钥写进代码/配置/日志/测试/提交；示例只用明显假值
- `--no-verify`、git merge/push/rebase/强推/删分支；联网；登录或改动真实服务器

## 交付要求
交付时把看板 state 改为 待审测并追加 history；在本文件末尾追加：
- 编译、lint、测试命令及各自输出文件路径
- 本次 CLI 会话日志路径
- 改动文件清单（`git diff --name-only main...`）与已知限制

## win-shell-flaky-1 诊断（2026-11-03，分支 windows/win-shell-flaky-1）
1. 现象：main CI 41492274773、41494947318 中 SidebarSwitchesBetweenConnectionAndSettings 在 NavigationTests.cs:55 的 10 秒 Join 超时，无异常；41403138518 曾 2 秒通过。根因未确认。
2. 诊断提交 df2740a：只改 windows/OsakaConnect.Tests/NavigationTests.cs，用 ITestOutputHelper 输出各阶段耗时，超时时输出最后完成阶段、ThreadState、Dispatcher 状态；10 秒超时与原断言不变。
3. Codex 日志：logs/win-shell/flaky-1-codex.log；/no-comments：logs/win-shell/flaky-1-nocomments-codex.log（无注释需删）；任务卡 logs/win-shell/flaky-1-card.md。
4. 本机无 .NET SDK，未编译未测试；需 CI（windows-latest）验证并读取测试输出定位卡住阶段。

## win-shell-flaky-2 只读分析（2026-11-03）
1. CI 41501414079（logs/win-shell/flaky-1-ci-41501414079.log:12-13）：到 "Thread started" 累计 2962ms，之后卡在 NavigationTests.cs:37 new MainWindow()；ThreadState=Background，Dispatcher 存在且未关闭。
2. Codex 只读分析（logs/win-shell/flaky-2-analysis-codex.log，任务卡 flaky-2-card.md）：产品代码 MainWindow.xaml.cs:8-21、ConnectionPage/SettingsPage.xaml.cs:7-10、App.xaml.cs、两个 csproj 中未发现 .Result/.Wait()、同步 IO/网络、配置或日志读取、Dispatcher.Invoke、静态构造函数或锁。
3. 构造链：InitializeComponent → SelectedIndex=0 → SelectionChanged → new ConnectionPage → PageHost.Content 赋值，均在 new MainWindow() 内。
4. 结论：根因未能在产品代码中确认；ThreadState=Background 只反映 IsBackground，不能区分 CPU 运行与框架/原生等待。下一轮在构造链上加分阶段计时（诊断提交，最终 PR 前 revert）。
5. 诊断提交 d29f6c5（Codex：logs/win-shell/flaky-3-codex.log，任务卡 flaky-3-card.md）：新增 windows/OsakaConnect/DiagnosticTrace.cs，在 MainWindow 构造、SelectionChanged、ConnectionPage 构造打点，测试输出全部打点。DiagnosticTrace 时钟从首次 Mark 起算，与测试 Stopwatch 基准不同。本机无 SDK，未编译；等组长推分支跑 CI。最终 PR 需 revert df2740a 与 d29f6c5。

## win-shell-flaky-4（2026-11-03，CI 41503200257）
1. 现象：第 1 次 307ms 通过；第 2 次超时，最后打点 "MainWindow.InitializeComponent before"（2996ms，线程 15），线程存活、Dispatcher 未关闭；new MainWindow() 到进入构造体约 3s。日志 repo/logs/win-shell/flaky-2-ci-41503200257-{1,2}.log。
2. Codex 只读分析（logs/win-shell/flaky-4-analysis-codex.log）：四份 XAML 无 ResourceDictionary/StaticResource/DynamicResource/Style/Brush，无手写 pack URI 或 Application.Current；卡点在生成的 InitializeComponent（推测为 Application.LoadComponent/BAML 加载或默认主题资源初始化）内部，资源死锁无证据；约 3s 包含 JIT、类型加载与 Window 基类构造，现有打点分不开。
3. 诊断提交 5e8b555（logs/win-shell/flaky-4-codex.log，任务卡 flaky-4-card.md）：InitializeComponent 前后记录 Application.Current 是否为空、其 Dispatcher 线程及 CheckAccess、ApartmentState；STA 入口首个打点对齐时钟；NoInlining 的 CreateMainWindow 入口打点；MainWindow 静态构造打点。改动文件：DiagnosticTrace.cs、MainWindow.xaml.cs、NavigationTests.cs。本机无 SDK 未编译。最终 PR 前 revert df2740a、d29f6c5、5e8b555。

## win-shell-flaky-5（2026-11-03，CI 41505344052）
1. 现象：head 0992278 windows-dotnet 3/3 通过，测试耗时 8s/2s/10s，接近 10s；打点只在失败时显示，通过时无数据。日志 repo/logs/win-shell/flaky-3-ci-41505344052-{1,2,3}.log。
2. 诊断提交 d95f110（logs/win-shell/flaky-5-codex.log，任务卡 flaky-5-card.md）：新增 windows/OsakaConnect.Tests/xunit.runner.json（showLiveOutput: true），csproj 复制到输出；ITestOutputHelper 实时输出，通过时也显示全部打点。NavigationTests.cs 未改。本机无 SDK，是否在 normal 级别控制台可见需 CI 确认。
3. 最终 PR 前 revert df2740a、d29f6c5、5e8b555、d95f110。

## win-shell-flaky-6（2026-11-03，CI 41507285651）
1. 现象：第 1 次超时，打点全在线程 16：静态构造 0ms，构造体入口 3550ms，InitializeComponent before 3551ms 且 Application.Current 为 null，after 未出现；第 2、3 次通过（2s、5s），showLiveOutput 未输出。日志 repo/logs/win-shell/flaky-4-ci-41507285651-{1,2,3}.log。
2. 通过时不显示的原因（Codex 推断，未在 Windows 验证）：ITestOutputHelper 内容附在测试结果上，vstest console logger 不展开通过测试的输出；showLiveOutput 发的是 informational 消息，被默认 console logger 级别过滤。
3. 诊断提交 cda2f8c（logs/win-shell/flaky-6-codex.log，任务卡 flaky-6-card.md）：NavigationTests.cs 新增 WriteDiagnostic，同时写 ITestOutputHelper 和 Console.Error（前缀 [NavigationTests]）；testhost 的 stderr 会作为 warning 转发到控制台。只改 NavigationTests.cs。

### 根因修复设计（草案，待组长批准，未实现）
1. 证据：卡点在 InitializeComponent 内（BAML 加载），线程是 STA，Application.Current 为 null，测试线程没有运行消息循环。推测（未证实）：在没有 Application、也没有运行 Dispatcher 循环的裸 STA 线程上加载 XAML 和主题资源时会偶发卡顿或很慢；JIT、冷启动和 CI 机器负载同样是候选原因。
2. 方案：新增 windows/OsakaConnect.Tests/WpfTestHost.cs（xUnit class/collection fixture），启动一个共享 STA 线程；在线程里先创建唯一的 Application（ShutdownMode=OnExplicitShutdown，ResourceAssembly 指向产品程序集），再跑 Dispatcher.Run()。测试通过 Dispatcher.InvokeAsync 排队执行建窗、导航断言和关窗，异常回传到测试线程；fixture 释放时 InvokeShutdown。
3. 约束：保留 10 秒预算，并且这 10 秒要包含宿主就绪的时间，不能把冷启动挪到计时外面；同一进程只能创建一个 Application，以后其他 WPF 测试类共用同一个 collection。暂无证据需要改产品代码。
4. 改动文件：NavigationTests.cs、新增 WpfTestHost.cs。动手前先 revert df2740a、d29f6c5、5e8b555、d95f110、cda2f8c。
5. 风险：共享状态残留、线程归属和关闭顺序；如果根因其实是 JIT 或机器负载，这个方案可能消除不了问题。
6. 验证：CI 命令不变，独立连续跑至少 10 次 windows-dotnet，全部通过、零跳过，记下每次耗时；仍超时就抓阻塞线程栈，再评估是否要改产品代码。

## win-shell-flaky-6diag（2026-11-03，CI 41509492518）
1. 现象：3/3 通过（9s、9s、2s），日志 repo/logs/win-shell/flaky-5-ci-41509492518-{1,2,3}.log 中 "[NavigationTests]" 出现 0 次（rg -c 确认）；测试段只有 "Passed! ... Total: 1" 汇总，stderr 和 showLiveOutput 都没转发。CI 不上传测试结果文件。
2. 诊断提交 b0aad42（logs/win-shell/flaky-6diag-codex.log，任务卡 flaky-6diag-card.md）：OsakaConnect.Tests.csproj 加 <VSTestLogger>console%3Bverbosity=detailed</VSTestLogger>，作用等同 dotnet test --logger "console;verbosity=detailed"，只影响本测试项目，测试代码不变。
3. 依据（未在 Windows 验证）：console logger 在 detailed 级别会为通过的测试打印 "Standard Output Messages"，即 ITestOutputHelper 的内容。若 CI 仍无输出，说明 SDK 没读这个属性，需组长决定改 workflow（属 CI 改动，走评审）。
4. 最终 PR 前 revert df2740a、d29f6c5、5e8b555、d95f110、cda2f8c、b0aad42。

## win-shell-flaky-7 修复交付（2026-11-03，CI 41511270330 确认卡点）
1. 根因：测试在新建的裸 STA 线程上 new MainWindow()，没有 Application，也没有运行 Dispatcher 循环，InitializeComponent 偶发卡住超过 10 秒。
2. 诊断提交全部 git revert：b0aad42→ef9ddcf、cda2f8c→b09850c、d95f110→887e577（xunit.runner.json 由 d95f110 新增，已删除，未放宽 A14 白名单）、5e8b555→bf7810e、d29f6c5→151dff4、df2740a→68f2e37。revert 后 windows/ 与 a8a002f 完全一致。
3. 修复 c5290ad：新增 windows/OsakaConnect.Tests/WpfTestHost.cs（xUnit collection fixture，共享 STA 线程，先设 ResourceAssembly 并创建 Application(OnExplicitShutdown)，再 Dispatcher.Run；测试经 InvokeAsync 排队执行，异常原样回传，Dispose 时 InvokeShutdown 并 Join）。NavigationTests.cs 的断言全部保留，测试数仍为 1；10 秒计时在宿主启动前开始，宿主启动也算在内。产品代码和 csproj 未改。
4. 对 a8a002f 的 windows/ 改动：NavigationTests.cs (+35/-41)、WpfTestHost.cs (+66)。
5. Codex：logs/win-shell/flaky-7-codex.log（任务卡 flaky-7-card.md）；/no-comments：logs/win-shell/flaky-7-nocomments-codex.log（无注释）。
6. 本机没有 .NET SDK，未编译、未跑 lint 和测试；需 CI windows-dotnet 连续多次（目标 10 次）全过，并跑 windows-acceptance。

## win-shell-flaky-8 返工 1（2026-11-03，CI 41515025076）
1. 原因：WpfTestHost.cs:46 赋值 Application.ResourceAssembly 时抛 InvalidOperationException（该属性已被设置，不能再改），每次运行都会复现。证据 repo/logs/win-shell/flaky-7-ci-41515025076-1.log:175-181。
2. 修复 6ee7af6：删掉这一行赋值。组件 URI 自带程序集名（/OsakaConnect;component/...），资源加载不依赖 ResourceAssembly；main 上原测试没有 Application 也通过过（CI 41403138518）。NavigationTests.cs 没改，断言、10 秒超时、测试数都不变。
3. Codex：logs/win-shell/flaky-8-codex.log（任务卡 flaky-8-card.md）；/no-comments：logs/win-shell/flaky-8-nocomments-codex.log。
4. 本机没有 .NET SDK，未编译未测试，需 CI windows-dotnet 验证。
