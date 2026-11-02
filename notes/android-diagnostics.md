# 派活 brief：android-diagnostics（Android）

- owner：写手 · Android　分支：`android/android-diagnostics`（从 main 拉，审阅/测试只针对该分支）
- 执行方式：用指定 CLI，**按 ponytail full 执行**；一个模块一个新会话。

## 目标
LogRedactor、InviteCode.isValid、错误展示（errors.json 文案）；为 0.2.0 预留的响应校验用 org.json 手写，本期只做 admin-contract 解析校验函数及单测。

## 只准改的文件
- `android/`
- `notes/android-diagnostics.md`（追加交付证明）、`board.json`（本模块条目）、`reports/model-issues.md`（如有）

## 接口引用
shared/errors.json；shared/test-vectors/{redaction,invite-code,admin-contract}.json。架构见 `docs/architecture.md`（含边界 schema 校验方式）。

## 验收条目
A7, A15（见 `acceptance.md`）。完成标准：JVM 单测读同一份向量全部通过；不新增 Gradle 依赖（JUnit 除外）。

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

## 进度记录（写手 · Android，2026-11-02 16:25 Brisbane）
1. 基线：android/android-shell b76cb0e（需其 Gradle 骨架；main 仅初始提交）。
2. Codex 会话日志：/home/box/osaka/logs/android-diagnostics-codex-20261102-161717.log
3. 本地检查（git diff --name-only、scripts/check-scope.sh --staged、git diff --check）：/home/box/osaka/logs/android-diagnostics-checks-20261102-161717.log，全部通过，无 Gradle 改动、无新增依赖。
4. 编译、lint、JVM 单测：未运行（本机无 JDK），待 CI。状态保持 写作中，CI 绿后才交审测。
5. 已知限制：测试源集内 JvmJson.kt 以 `package org.json` 手写替身遮蔽 android.jar 桩（shortcut 注释已写），依赖测试类路径优先于 android.jar；CI 若报重复类或解析差异需改方案。未验证 Android 真实 org.json 行为。

## 交付证明（2026-11-02）
1. CI：https://github.com/Yarrow-he/OsakaConnect/actions/runs/41403204924 ，结论 success，android job 跑 `gradle --no-daemon assembleDebug testDebugUnitTest lint`（Gradle 8.9，JDK 17）。
2. sha：远端 97d42d6（组长合入 origin/main 的合并提交），包含本地提交 6c71afc（gh compare：ahead 3，behind 0）。
3. Codex 日志：/home/box/osaka/logs/android-diagnostics-codex-20261102-161717.log ；/home/box/osaka/logs/android-diagnostics-androidx-codex-20261102-165454.log
4. 检查日志：/home/box/osaka/logs/android-diagnostics-checks-20261102-161717.log ；CI 核实：/home/box/osaka/logs/android-ci-verify-20261102.log

## 审阅（审阅 · Osaka，2026-11-02）
结论：退回（证明不足，未进入代码审阅）。
1. /home/box/osaka/logs/android-ci-verify-20261102.log 只有一句“assembleDebug testDebugUnitTest lint success”的摘要，没有原始 job 日志、没有测试数；此前出现过 gradlew 缺失的假绿（ci-android-diagnostics-20261102.log 为“无 Android 代码，跳过”），摘要不足以排除。需交 run 的原始 android job 日志（含 BUILD SUCCESSFUL 与测试数）。
2. 预检（只看 diffstat）：gradle-wrapper.properties 在模块 paths android/ 内，不超 scope；reports/model-issues.md 在 global scope 且规则要求写入，可接受。
1. android-diagnostics：JvmJson.kt 替代 org.json 的组长批准，在 notes/decisions/board 未查到（decisions/001 第68行写的是 org.json 手写校验），记为未核实；JvmJson.kt 128 行以 package org.json 遮蔽 android.jar 桩，待证明补齐后审是否过度设计。

## 返工证明（rework=1，写手 · Android）
1. 原始 CI 日志：/home/box/osaka/logs/ci-41403204924-android.log（run 41403204924 android job）。第 269 行 `> Task :app:testDebugUnitTest`，第 281 行 `BUILD SUCCESSFUL in 2m 43s`。
2. 单测数：日志里没有用例计数（Gradle 默认不打印，workflow 没上传测试结果 XML）。如实记为“无计数”。
3. JvmJson.kt 批准：此前没有书面记录。组长补拍板见 decisions/002-test-deps-and-jvmjson.md 决定第 2 条（2026-11-02 17:35 AEST，暂时接受，附三项条件）。该文件目前在主仓库未提交。
4. 替代原因：Android 自带的 org.json 在 JVM 单测里是空桩，没法跑；JvmJson.kt 只用 Kotlin 标准库，仅在 test 源集。
5. 相对 android-shell 的增量：三点 diff（/home/box/osaka/logs/android-diagnostics-vs-shell-files.txt）因为存在多个 merge base，会混进 shell 文件。按 shell 基线整理的两点 diff 见 /home/box/osaka/logs/android-diagnostics-vs-shell-files-2dot.txt：diagnostics 下 4 个主文件、6 个测试文件，外加 notes。

## 审阅 返工1（审阅 · Osaka，2026-11-02）
结论：必须改（返工2）。证明已接受：ci-41403204924-android.log 中 compileDebugKotlin(248)、compileDebugUnitTestKotlin(264)、testDebugUnitTest(269)、lint(278) 真执行；BUILD SUCCESSFUL(281)。
必须改（全部可机器化：单测进 CI）：
1. JvmJson.kt:32/46/61 递归无深度上限，深嵌套抛 StackOverflowError（Error），绕过 AdminContract.kt 只 catch JSONException 的防线；加深度上限（如 64）抛 JSONException，配 "[".repeat(10000) 反例。
2. 非法转义/\u 无反例：补 "\q"、"\u12"、"\u00zz"、串尾孤立反斜杠。
3. 重复键无反例（JvmJson.kt:52 已拒）：补拒收例。
4. 数字畸形/溢出无反例：补 01、1.、-、+1、1e，及超 Long 数值的行为断言。
5. 字符串内裸控制字符无反例（JvmJson.kt:96 已拒）：补拒收例。
建议改：
1. 补一条 MB 级整体超长输入反例（决策 002“超长输入”现仅字段级 65/201）。
2. JvmJson.kt:106 Regex 每次重编译，提为常量。
3. LogRedactor.kt 不认 JSON 带引号键形式，0.2.0 前先扩 shared 向量。
4. 日志无单测计数，组长改 workflow。
APK：JvmJson 在 src/test，不进正式 APK（静态可判）。交测试验证：真机 org.json 与 JvmJson 行为差异（重复键 last-wins 会接受、NaN/Infinity、裸控制字符、BOM），用模拟器跑同向量+上述反例。
日志：/home/box/osaka/logs/review-android-diagnostics-20261102.log；diff：/home/box/osaka/logs/review-android-diagnostics-diff.txt。

## 返工2（写手 · Android，2026-11-02T17:47:02+10:00）
1. 提交 b691b21：JvmJson.kt 嵌套深度上限 64（超了抛 JSONException）；数字 Regex 提为常量 NUMBER_FORMAT；超出 Long 范围的整数原来会静默转成 Double，现在改为拒收。新增 JvmJsonTest.kt，覆盖 "[".repeat(10000)、深度 64/65、非法转义、重复键、畸形数字、Long 边界、裸控制字符；AdminContractTest.kt 补 1 MB 输入反例。AdminContract.kt 未改：超深嵌套现在抛 JSONException，已有的 catch 能接住。
2. 任务卡：/home/box/osaka/logs/android-diagnostics-rework2-card.md
3. Codex 日志：/home/box/osaka/logs/android-diagnostics-rework2-codex-20261102-174502.log
4. check 日志：/home/box/osaka/logs/android-diagnostics-rework2-checks-20261102-174502.log（scope 通过；本地无 JDK，未编译。括号计数不平衡是字符串和字符字面量里的括号造成的，不是语法错误）
5. CI 要等组长推送后才能跑；看板保持「写作中」，CI 绿了再改成待审测。

## 审阅 返工2（审阅 · Osaka，2026-11-02）
结论：通过。证明已接受：ci-41408582664-android.log 中 compileDebugKotlin、compileDebugUnitTestKotlin、testDebugUnitTest、lint 真执行（非 NO-SOURCE/SKIPPED/UP-TO-DATE），第 281 行 BUILD SUCCESSFUL，head 8f73f21。
上轮必须改五条全部到位：
1. 深度上限：JvmJson.kt:3/39，64 过、65 拒；JvmJsonTest.kt:14-33 含 "[".repeat(10000)。递归只有 nextValue↔readObject/readArray，StackOverflowError 到不了 AdminContract.kt:58。
2. 非法转义：JvmJsonTest.kt:37-39。
3. 重复键：JvmJsonTest.kt:44-45。
4. 畸形数字与超 Long：JvmJsonTest.kt:51-52，边界 60-63。
5. 裸控制字符：JvmJsonTest.kt:68-70。
另核：Regex 已提常量（JvmJson.kt:4）；只改 4 个文件，均在范围内；未动 .claude/.codex；无密钥；无新依赖；改动最小。
建议改：
1. AdminContractTest.kt:10-13 rejectsMegabyteResponse 实际走 message 字段 200 上限（AdminContract.kt:71），不是整体长度检查；要么组长拍板加整体上限，要么改准用例名。可机器化。
2. 上轮建议改 3（LogRedactor 引号键）、4（CI 单测计数）仍挂账。
日志：/home/box/osaka/logs/review-android-diagnostics-r2-20261102.log；diff：/home/box/osaka/logs/review-android-diagnostics-r2-diff.txt。

## 追加改动（决策 002 补充第 3 条，组长追加，不计返工，2026-11-02）
1. AdminContract.parse 入口加整体上限 64 KiB（65536 字节 UTF-8），超出抛 IllegalArgumentException("Response too large")，字段检查保留；单测：65537 字节拒收、65536 字节通过。
2. 原 rejectsMegabyteResponse 实际撞 message 200 字符上限，改名 rejectsOversizedMessageField。
3. LogRedactor 根因：正则要求键名后紧跟 [:=]，带引号的 JSON 键在键与冒号间多一个 `"`，且值带引号，因而不匹配。修正为允许键两侧可选引号、引号值整体替换为 "***"；补复现与幂等用例。
4. 任务卡 logs/android-diagnostics-addon-card.md；Codex 日志 logs/android-diagnostics-addon-codex-20261102-175856.log。本地未编译，待 CI。

## 审阅：追加增量 8f73f21..a03cab0（2026-11-02）
结论：通过，无必须改。
1. 证明：ci-41410083845-android.log 第 281 行 BUILD SUCCESSFUL；compileDebugKotlin（235）、testDebugUnitTest（266）、lint（lintAnalyzeDebug 等）真执行；无单测计数。
2. 64 KiB 上限：AdminContract.kt:53 在 try 外抛 IllegalArgumentException，与其余校验同类型；main 源集无其他调用方，不存在只 catch JSONException 被绕过的问题。65536 过、65537 拒，用例含多字节字符，区分了字节与字符。
3. LogRedactor.kt:4-9：引号值分支两个备选互斥，无 ReDoS；幂等有用例；redaction.json 8 例行为不变。
4. 范围内 4 个 android 文件加 notes，无依赖、配置改动，无真实密钥。
建议改：
1. AdminContractTest.kt:11 rejectsOversizedMessageField 现在先被整体上限拦下，名不副实；改名或加 message 断言。
2. 引号键用例未进 shared/test-vectors/redaction.json；Windows LogRedactor.cs:8 另支持单引号、遇引号截断值，三端行为不一致。转组长（与 win-diagnostics 建议改 1 重复）。
日志：logs/review-android-diagnostics-addon-20261102.log

## 测试 v2（测试 · Osaka，2026-11-02 23:25 AEST）@a8a30d1
1. 结论：不通过，暂不判返工，需组长定口径。定性：真机 org.json 与测试替身 JvmJson 对重复键行为不一致；测试向量按 JvmJson 写，断言与真机矛盾。
2. CI：run 41450047955，android-emulator 失败（connectedDebugAndroidTest FAILED）。
3. 关键报错行：「OrgJsonDivergenceTest > deviceOrgJsonMatchesJvmExpectations[emulator-5554 - 11] FAILED」「java.lang.AssertionError: duplicate-keys: 真机 org.json 解析成败与 JvmJson 不一致（input={"code":"INTERNAL","code":"FORBIDDEN","message":"x"}，真机结果={"code":"FORBIDDEN","message":"x"}） expected:<false> but was:<true>」。
4. 根因：JvmJson.kt 第52行拒重复键，json-divergence.json 据此写 jvm.ok=false；真机 Android org.json 后值覆盖。断言在首个分歧处停止，其余 case（NaN、裸控制字符、BOM）真机结果未知。
5. 口径：A7/A15 未要求拒重复键，按现口径是测试向量/断言要改（测试侧）。若协议要求拒重复键，则为产品返工（AdminContract 解析前查重复键）。请组长定。
6. 命令：gh run view 41450047955 -R Yarrow-he/OsakaConnect --log-failed
7. 日志：/home/box/osaka/repo/logs/android-diagnostics/2026-11-02-test-v2.log；CLI 日志：/home/box/osaka/artifacts/cli-logs/retest-v2-3modules-2026-11-02.log
8. 复现：android-diagnostics 分支 CI android-emulator job（API 30 模拟器），bash tests/android/android-diagnostics/run-instrumented.sh。

## 组长派单：重复 JSON 键取后值（不计返工，2026-11-02）
1. 改动：JvmJson.kt 删 checkJson(!values.containsKey(key))，重复键后值覆盖；JvmJsonTest.kt rejectsDuplicateKeys 改为 duplicateKeysUseLastValue，断言 {"a":1,"a":2} 的 a 为 2L。
2. 命令：codex exec -s workspace-write（任务卡：仅两文件，禁其他文件/联网/配置，follow ponytail full）；git diff --name-only 仅两文件。
3. 构建：本机无 java/gradle，assembleDebug testDebugUnitTest lint 未执行，待 CI 验证（未交审阅）。
4. 日志：/home/box/osaka/logs/android-diagnostics/codex-dupkey-20261102-232719.log；/home/box/osaka/logs/android-diagnostics/build-dupkey-20261102-232719.log。
5. 向量文件 json-divergence.json 由测试改。

## 返工 3（2026-11-03T00:26:56+10:00）
1. 原因：设备 org.json 与 JvmJson 在 3 个向量上不符（run 41455904368，OrgJsonDivergenceTest.kt:77）。组长裁定 JvmJson 对齐设备。
2. 改动（提交 a94496d）：JvmJson 字符串内接受裸控制字符；跳过开头 BOM。测试 acceptsUnescapedControlCharactersAndLeadingBom 替换 rejectsUnescapedControlCharacters。
3. 命令：codex exec -s workspace-write，日志 logs/android-diagnostics/codex-rework3-20261103-002555.log；git diff --name-only 仅 2 个文件。
4. 本地构建：无 kotlinc/java/gradle，跳过，日志 logs/android-diagnostics/build-rework3-20261103-002555.log。构建证明待 CI（android、android-emulator）。
