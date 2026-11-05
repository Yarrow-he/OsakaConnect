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
