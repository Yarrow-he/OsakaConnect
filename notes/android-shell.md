# 派活 brief：android-shell（Android）

- owner：写手 · Android　分支：`android/android-shell`（从 main 拉，审阅/测试只针对该分支）
- 执行方式：用指定 CLI，**按 ponytail full 执行**；一个模块一个新会话。

## 目标
Kotlin 单 Activity；未激活(P1)/已激活(P2) 占位与导航；生命周期回调经 LogRedactor 记录。

## 只准改的文件
- `android/`
- `notes/android-shell.md`（追加交付证明）、`board.json`（本模块条目）、`reports/model-issues.md`（如有）

## 接口引用
design/inventory.md 第 1 节（只读参考）。架构见 `docs/architecture.md`（含边界 schema 校验方式）。

## 验收条目
A5, A6, A16, A14（见 `acceptance.md`）。完成标准：./gradlew assembleDebug testDebugUnitTest 通过；仪器测试两页切换、旋转与后台回前台不崩溃且生命周期被记录。

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

## 交付证明（2026-11-02）
1. CI：https://github.com/Yarrow-he/OsakaConnect/actions/runs/41403200131 ，结论 success，android job 跑 `gradle --no-daemon assembleDebug testDebugUnitTest lint`（Gradle 8.9，JDK 17）。
2. sha：远端 9e47998（组长合入 origin/main 的合并提交），包含本地提交 ae5f609（gh compare：ahead 3，behind 0）。
3. Codex 日志：/home/box/osaka/logs/android-shell-codex-20261102-145542.log ；/home/box/osaka/logs/android-shell-androidx-codex-20261102-165454.log
4. 检查日志：/home/box/osaka/logs/android-shell-androidx-checks-20261102-165454.log ；CI 核实：/home/box/osaka/logs/android-ci-verify-20261102.log

## 审阅（审阅 · Osaka，2026-11-02）
结论：退回（证明不足，未进入代码审阅）。
1. /home/box/osaka/logs/android-ci-verify-20261102.log 只有一句“assembleDebug testDebugUnitTest lint success”的摘要，没有原始 job 日志、没有测试数；此前出现过 gradlew 缺失的假绿（ci-android-shell-20261102.log 为“无 Android 代码，跳过”），摘要不足以排除。需交 run 的原始 android job 日志（含 BUILD SUCCESSFUL 与测试数）。
2. 预检（只看 diffstat）：gradle-wrapper.properties 在模块 paths android/ 内，不超 scope；reports/model-issues.md 在 global scope 且规则要求写入，可接受。

## 返工证明（rework=1，写手 · Android）
1. 原始 CI 日志：/home/box/osaka/logs/ci-41403200131-android.log（run 41403200131 android job 125139336542）。第 264 行 `> Task :app:testDebugUnitTest`，第 278 行 `BUILD SUCCESSFUL in 2m 41s`。
2. 单测数：日志里没有用例计数（Gradle 默认不打印 tests completed，workflow 也没上传测试结果 XML）。如实记为“无计数”。
3. 原摘要 /home/box/osaka/logs/android-ci-verify-20261102.log 作废，以原始日志为准。

## 审阅 返工1（审阅 · Osaka，2026-11-02）
结论：通过（代码层面无必须改）。证明已接受：ci-41403200131-android.log 中 compileDebugKotlin(248)、compileDebugUnitTestKotlin(260)、testDebugUnitTest(264)、lintDebug/lint(274-275) 均真执行，无 NO-SOURCE/SKIPPED/UP-TO-DATE；BUILD SUCCESSFUL(278)。
待组长：android/app/build.gradle.kts:28-32 的 junit 4.13.2、androidx.test core/runner/ext-junit 未在决策 002 中批准，需补进白名单（可机器化：依赖白名单扩到 gradle）。
建议改：
1. 日志无单测计数，由组长改 workflow 输出测试数（可机器化：CI 汇总 test-results XML）。
2. settings.gradle.kts:16-19 的 useAndroidX 与 gradle.properties:1 重复，删前者及过时注释（可机器化：grep 检查）。
3. build.gradle.kts:14 “These commands have NOT been run” 已过时，删除（不可机器化）。
权限：Manifest 无 uses-permission，exported 仅 LAUNCHER；无密钥/IP/账号；scope 合规。
日志：/home/box/osaka/logs/review-android-shell-20261102.log；diff：/home/box/osaka/logs/review-android-shell-diff.txt。

## 测试 v2（测试 · Osaka，2026-11-02 23:25 AEST）@1b48848
1. 结论：不通过，不判返工。定性：测试/CI 接线问题，不是 shell 产品 bug。
2. CI：run 41450041148，android-emulator 失败。shell 自身项通过：connectedDebugAndroidTest「BUILD SUCCESSFUL in 1m 38s」，ci-run.sh「通过：A5 启动无崩溃」。
3. 关键报错行：「JvmJsonDivergenceTest > jvmJsonMatchesVectorExpectations FAILED」「java.lang.RuntimeException at JvmJsonDivergenceTest.kt:61」「5 tests completed, 1 failed, 3 skipped」「> Task :app:testDebugUnitTest FAILED」。
4. 根因：workflow 在 shell 分支也跑 tests/android/android-diagnostics/run.sh，run.sh 只拷 tests/android/android-diagnostics/*.kt；JvmJson.kt 只在 android-diagnostics 分支的 android/app/src/test，shell 分支没有，org.json 落到 android.jar 空桩，第61行 JSONTokener 抛 RuntimeException。3 个 skip 是 DiagnosticsVectorsTest 因 shell 无 LogRedactor/InviteCode 被 assume 跳过。
5. 建议（测试侧/组长）：android-emulator 在 shell 分支不跑 diagnostics 两步，或 JvmJson 随测试目录提供；run.sh 缺依赖时明确报错。
6. 命令：gh run view 41450041148 -R Yarrow-he/OsakaConnect --log-failed
7. 日志：/home/box/osaka/repo/logs/android-shell/2026-11-02-test-v2.log；CLI 日志：/home/box/osaka/artifacts/cli-logs/retest-v2-3modules-2026-11-02.log
8. 复现：android-shell 分支 CI android-emulator job，或 cd android && ./gradlew testDebugUnitTest（先执行 run.sh 拷入测试）。
## 2026-11-02 合入 main
1. 合入 origin/main（9662b8e，含 PR #15）为 454063d，已推送。
2. CI：https://github.com/Yarrow-he/OsakaConnect/actions/runs/41455806499 ，待测试复测。

## 偶发失败修复 android-shell-flaky-1 @0f57782（2026-11-03T04:55:38+10:00）
1. 根因：测试假设回前台时仍是 rotatedId 同一实例；HOME 与 Activity 请求方向不同，回前台时方向切回可能触发系统再次重建，产生新 lifecycleId（CI 日志 /home/box/osaka/logs/main-ci/41492274773-failed.log:507、41494947318-failed.log:451）。属测试假设错误，产品代码无缺陷。
2. 改动：仅 android/app/src/androidTest/java/com/osakaconnect/shell/MainActivityTest.kt。用 ActivityLifecycleMonitorRegistry 按实例记录回调；回前台后每个 resumed 实例都校验 P2 与导航；逐实例比对观察到的回调与 OsakaLifecycle 日志。未加重试/跳过，未删测试；原“rotatedId 固定 10 步序列”断言改为逐实例比对（请审阅确认不算放宽）。无新依赖（androidx.test:runner 已有）。
3. Codex 日志：/home/box/osaka/logs/android-shell-flaky/codex-fix-*.log、/home/box/osaka/logs/android-shell-flaky/codex-nocomments-*.log；任务卡 /home/box/osaka/logs/android-shell-flaky/card1.md、card2.md；Codex 静态检查 /home/box/osaka/logs/android-shell-flaky/codex-static-checks/。
4. 验证限制：本机无 JDK/Gradle/Android SDK，安装需联网（禁止），本地 assembleDebug/testDebugUnitTest/lint 与模拟器 3 连跑均未执行。证据路径改为 CI android 与 android-emulator job，需组长推送分支后触发；3 连跑需组长对同一提交 rerun 3 次。
