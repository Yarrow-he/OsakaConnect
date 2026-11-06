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

## 看板收口（pipeline-watchdog 2026-11-07T01:41:33+1000）

1. PR #104 已合入 main（merge `44f3f70`，head `d999a2c`）。
2. 过时重开 PR #103 已用 `as_lead.sh close` 关闭（被 #104 取代）。
3. ErrorMessages 反向断言建议留后续小 PR。


## 交付证明（2026-11-07，android/android-diagnostics-2）

android-diagnostics 产品从未合入 main。本地旧 tip `f66b7da0c7f5be0785220701fe99c6bd2088b48f` 上有产品和单测，测试终判曾引用 `a285acfb980bcc2fbaa51018bc56c7d51bfeaa4f`；这些历史记录不代表本次构建通过。基线 `origin/main @ f5b740ec62963123fdff4b845a1cca74270be201` 没有 `android/app/src/main/java/com/osakaconnect/diagnostics/`，远端 `android/android-diagnostics` 已删除。

本次从 origin/main 新建分支 `android/android-diagnostics-2`，工作树为 `/home/box/osaka/wt/android-diagnostics-2`，未复用旧 `wt/android-diagnostics`。产品来源提交为 `f66b7da0c7f5be0785220701fe99c6bd2088b48f`：用 `git checkout f66b7da0c7f5be0785220701fe99c6bd2088b48f --` 后接下列 11 个 diagnostics 路径迁入产品与单测，再将 `MainActivity.recordLifecycle` 接到 `LogRedactor`。本次未改 `board.json`。

相对上述 origin/main 基线的改动文件如下；前 11 项为迁入路径，后两项为接线和本节追加：

- `android/app/src/main/java/com/osakaconnect/diagnostics/AdminContract.kt`
- `android/app/src/main/java/com/osakaconnect/diagnostics/ErrorMessages.kt`
- `android/app/src/main/java/com/osakaconnect/diagnostics/InviteCode.kt`
- `android/app/src/main/java/com/osakaconnect/diagnostics/LogRedactor.kt`
- `android/app/src/test/java/com/osakaconnect/diagnostics/AdminContractTest.kt`
- `android/app/src/test/java/com/osakaconnect/diagnostics/ErrorMessagesTest.kt`
- `android/app/src/test/java/com/osakaconnect/diagnostics/InviteCodeTest.kt`
- `android/app/src/test/java/com/osakaconnect/diagnostics/JvmJson.kt`
- `android/app/src/test/java/com/osakaconnect/diagnostics/JvmJsonTest.kt`
- `android/app/src/test/java/com/osakaconnect/diagnostics/LogRedactorTest.kt`
- `android/app/src/test/java/com/osakaconnect/diagnostics/SharedVectors.kt`
- `android/app/src/main/java/com/osakaconnect/shell/MainActivity.kt`：新增 `import com.osakaconnect.diagnostics.LogRedactor`；`recordLifecycle` 改为 `Log.i("OsakaLifecycle", LogRedactor.redact("$lifecycleId $callback"))`；删除过时的 shortcut 注释。
- `notes/android-diagnostics.md`：仅在文末追加本节，文件开头的派活 brief 原文保留。

未改动的范围为 `board.json`、`tests/`、`windows/`、`shared/`、`.github/workflows/`、`scripts/ci/`、`policy-proposals/`、`.codex/`、`.claude/`。

Codex CLI 使用 `gpt-6-astra`、reasoning `high`，按 ponytail full 执行。接线会话的证明文件为：

- 任务卡：`/home/box/osaka/logs/android-diagnostics-2/codex-taskcard-fix-20261107-001123.md`
- 会话日志：`/home/box/osaka/logs/android-diagnostics-2/codex-fix-20261107-001123.log`
- lastmsg：`/home/box/osaka/logs/android-diagnostics-2/codex-fix-20261107-001123.lastmsg`
- meta：`/home/box/osaka/logs/android-diagnostics-2/codex-fix-20261107-001123.meta`

接线会话开始于 2026-11-07 00:11:30 AEST，结束于 00:12:16 AEST，exit 0。时区为 Australia/Brisbane（UTC+10）。

随后执行的 `/no-comments` 会话证明文件为：

- 任务卡：`/home/box/osaka/logs/android-diagnostics-2/codex-taskcard-nocomments-20261107-001234.md`
- 会话日志：`/home/box/osaka/logs/android-diagnostics-2/codex-nocomments-20261107-001234.log`
- lastmsg：`/home/box/osaka/logs/android-diagnostics-2/codex-nocomments-20261107-001234.lastmsg`
- meta：`/home/box/osaka/logs/android-diagnostics-2/codex-nocomments-20261107-001234.meta`

该会话开始于 2026-11-07 00:12:34 AEST，结束于 00:13:13 AEST，exit 0；结论为无需改动，现存注释均为约束或 why。本追加由同目录 Codex notes 会话完成，任务卡与日志分别按 `codex-taskcard-notes-*.md`、`codex-notes-*.log` 命名。CLI 的 exit 0 仅表示会话正常结束，不表示编译或测试通过。

本地尝试从 `android/` 运行构建、JVM 单测和 lint，计划命令为 `./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug`。工具链预检发现本机没有 `java`、`javac`、`gradle`、`gradlew`、`sdkmanager`、`adb`、`emulator`，`JAVA_HOME` 与 `ANDROID_HOME` 未设置；`android/` 没有 `gradlew` 包装脚本，wrapper 目录仅有 `gradle-wrapper.properties`。因此这三项任务均跳过，未实际执行，没有本次 `BUILD SUCCESSFUL` 结论。

- 工具链检查时间：2026-11-07 00:13:20 AEST；记录：`/home/box/osaka/logs/android-diagnostics-2/verify-toolchain.txt`。
- 编译、JVM 单测、lint 共用的跳过说明：`/home/box/osaka/logs/android-diagnostics-2/gradlew-assembleDebug-testDebugUnitTest-lintDebug.log`。
- 静态检查 `git diff --check` 干净，记录见上述 `verify-toolchain.txt`。
- 语言守卫命令 `bash tests/android/android-shell/lang-guard.sh` 输出「通过：A14/A17（android/）」；输出文件：`/home/box/osaka/logs/android-diagnostics-2/lang-guard-android-shell.txt`。

已知限制：编译、lint、JVM 单测仍需等待 CI（android-emulator / Gradle 8.9 + JDK 17）执行。diagnostics 产品目录存在后，android-emulator 按决策 009 应能真正运行 diagnostics 脚本；这是待 CI 验证的预期，不是本次已通过的结果。本 notes 会话仅追加本文件，不修改 board.json，不联网，不提交 git。

## 补充说明（2026-11-07，组长）

上面列的 13 个文件是中间版本的清单，最后一项 notes 后来按要求移出了功能分支。最终交付的 [#104](https://github.com/Yarrow-he/OsakaConnect/pull/104)（合并提交 `44f3f70`）对第一父提交的净改动是 12 个文件，不含 notes：

1. `android/app/src/main/java/com/osakaconnect/diagnostics/AdminContract.kt`
2. `android/app/src/main/java/com/osakaconnect/diagnostics/ErrorMessages.kt`
3. `android/app/src/main/java/com/osakaconnect/diagnostics/InviteCode.kt`
4. `android/app/src/main/java/com/osakaconnect/diagnostics/LogRedactor.kt`
5. `android/app/src/main/java/com/osakaconnect/shell/MainActivity.kt`
6. `android/app/src/test/java/com/osakaconnect/diagnostics/AdminContractTest.kt`
7. `android/app/src/test/java/com/osakaconnect/diagnostics/ErrorMessagesTest.kt`
8. `android/app/src/test/java/com/osakaconnect/diagnostics/InviteCodeTest.kt`
9. `android/app/src/test/java/com/osakaconnect/diagnostics/JvmJson.kt`
10. `android/app/src/test/java/com/osakaconnect/diagnostics/JvmJsonTest.kt`
11. `android/app/src/test/java/com/osakaconnect/diagnostics/LogRedactorTest.kt`
12. `android/app/src/test/java/com/osakaconnect/diagnostics/SharedVectors.kt`

核对命令：`git diff --name-only 44f3f70^1 44f3f70`。
