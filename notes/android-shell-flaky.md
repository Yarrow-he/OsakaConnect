# android-shell-flaky

## 测试（2026-11-03T05:16:32+10:00，版本 0f57782，CI head ac22407）

结论：不通过（放宽断言，非根因修复）。

1. 范围：git diff --name-only origin/main...ac22407 = MainActivityTest.kt + board.json + notes/android-shell.md；0f57782..ac22407 只含 board.json、notes/android-shell.md（记账），通过。
2. CI：run 41505317288 attempts 1/2/3 head 均为 ac22407，全部 job success。android-emulator 每次 "Finished 1 tests"（verify.log L591/L740、L1409/L1558、L2226/L2375），androidTest 只有这一个 @Test，无 @Ignore。日志不打印测试名。
3. 不通过原因（diff 行号见 logs/android-shell-flaky/2026-11-03-diff-0f57782.diff）：
   1. 删除回前台 UUID 一致性断言 assertEquals(rotatedId, activity.lifecycleId)（diff L135），改为任意 RESUMED 的 MainActivity 实例都接受（L62-73）。
   2. 删除 rotatedId 精确生命周期序列断言（L171-175），只剩首 onCreate、尾 onDestroy、与 logcat 自洽（L176-180）。
   3. 删除后台期间 assertEquals(Lifecycle.State.CREATED, scenario.state)（L114）。
   4. 回前台被重建若是产品 bug，新测试会静默通过（注释 L63 直接把重建当合法前提）。
4. 返工要求：追到回前台被重建的根因（产品侧或模拟器 HOME 方向导致的配置变化），修根因或用可证明的方式固定环境方向，保留 UUID 一致性断言。
5. 证据：logs/android-shell-flaky/2026-11-03-ci-41505317288-verify.log；DeepSeek 分析会话 logs/android-shell-flaky/2026-11-03-claude-test-analysis.log（VERDICT: FAIL）。

## 测试（2026-11-03 05:50 AEST，返工版 bb8da34，CI head e2082b9）

结论：通过（android-shell 模块）。

1. 范围：git show --stat bb8da34 只改 android/app/src/androidTest/.../MainActivityTest.kt（+74/-66）；bb8da34..e2082b9 只含 board.json、notes/android-shell.md，android/ 无差异。
2. 断言保留（logs/android-shell-flaky/2026-11-03-diff-bb8da34.diff，0f57782..bb8da34 段）：
   1. 回前台 UUID 一致 assertEquals(rotatedId, activity.lifecycleId)：L156。
   2. rotatedId 精确 10 步生命周期序列：L182-185；originalId 6 步序列 L178-181。
   3. 后台 onStop 等待 + assertEquals(Lifecycle.State.CREATED, scenario.state)：L135、L142。
   4. finally 嵌套恢复 user_rotation / accelerometer_rotation：L193-197。
   5. 新增 lifecycleId 数 <=3；无 @Ignore/重试/跳过。
3. CI run 41509704835（gh 自取）：attempts 1/2/3 head 均为 e2082b9；android-emulator 3/3 success，每次两段 "Finished 1 tests"（verify.log L38/L43、L52/L57、L66/L71）。其余 job 除 windows-dotnet 外全 success。
4. windows-dotnet 3 次均 failure：OsakaConnect.Tests.NavigationTests.SidebarSwitchesBetweenConnectionAndSettings（verify.log L46-48），属 Windows 端 win-shell-flaky，与本改动无关；整体 run 结论为 failure，合并前需该线修复或组长裁定。gh 对 attempt 2/3 的 windows-dotnet 日志返回与 attempt 1 相同时间戳，原始日志存疑，job 结论以 attempts API 为准。
5. 建议（非阻断）：lifecycleId <=3 可收紧为 ==2。
6. 证据：logs/android-shell-flaky/2026-11-03-ci-41509704835-verify.log；DeepSeek 分析 logs/android-shell-flaky/2026-11-03-claude-test-analysis-bb8da34.log（L37 VERDICT: PASS）。
