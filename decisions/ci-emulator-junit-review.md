# CI 评审：android-emulator 上传 testDebugUnitTest XML（2026-11-03）

1. 评审 A：codex exec -m gpt-6-astra -c model_reasoning_effort=high，新会话，工作目录 /tmp。
2. 评审 B：ds-claude -p --model deepseek-v4-flash（--effort high），新会话，与 A 互不可见。
3. 两者输入完全相同（下附原文），输出未经编辑；日志 /home/box/osaka/logs/ci-emulator-junit/。

## 输入（原文）

````
下面是一份 CI 改动的 diff。请分别回答：1. 有没有削弱或绕过检查；2. 有没有泄露密钥的风险；3. 有没有影响本机网络或权限的操作。每项给出结论和依据。

diff --git a/.github/workflows/ci.yml b/.github/workflows/ci.yml
index bba5d16..dafe59c 100644
--- a/.github/workflows/ci.yml
+++ b/.github/workflows/ci.yml
@@ -202,3 +202,9 @@ jobs:
             tests/results/
             android/app/build/reports/androidTests/
           if-no-files-found: ignore
+      - if: always() && steps.has.outputs.code != ''
+        uses: actions/upload-artifact@v4
+        with:
+          name: android-emulator-junit
+          path: android/app/build/test-results/testDebugUnitTest/*.xml
+          if-no-files-found: ignore
````

## 评审 A 输出（原文）

````
1. **没有看到削弱或绕过测试检查的改动。**  
   新增步骤只上传测试结果 XML，没有修改测试命令、失败条件或添加 `continue-on-error`。`always()` 允许在此前步骤失败后仍尝试上传，不会把测试失败变成成功；`if-no-files-found: ignore` 仅表示找不到上传文件时不报错，不代表忽略测试失败。是否上传还取决于 `steps.has.outputs.code != ''`。

2. **没有看到直接泄露密钥的行为，但存在报告内容被间接上传的风险。**  
   diff 没有引用 `secrets`、打印环境变量或上传密钥文件。上传范围限于指定目录下的 `*.xml`。不过，若测试报告包含密钥、令牌或带凭据的日志，这些内容也会进入 artifact，被有权下载该产物的人获取。仅凭 diff 无法确认 XML 是否含敏感信息。

3. **有 CI runner 的网络上传行为，没有看到修改本机网络配置或权限的操作。**  
   `actions/upload-artifact@v4` 会读取匹配文件并通过网络上传到 GitHub 的 artifact 服务。这里没有修改代理、DNS、防火墙、路由或文件权限，也没有提权命令。操作发生在 CI runner 上；若使用自托管 runner，则涉及该机器的文件读取和出站网络访问，不会因这段配置自动修改开发者本机。
````

## 评审 B 输出（原文）

````
我先说明一下上下文：工作目录 `/home/box/osaka/logs/ci-emulator-junit` 里只有 `input.txt` 和运行日志，没有可读的 `.github/workflows/ci.yml` 原文。因此下面的判断只依据你给出的 diff 片段，涉及 `steps.has` 的定义等需要到原文件里核对。

## 1. 有没有削弱或绕过检查

**结论：没有直接削弱或绕过任何检查；但存在两处“静默跳过”和一处“名实不符”，会削弱结果的可信度，建议核实。**

依据：

- 新增的只是一个 `actions/upload-artifact` 步骤，没有改动任何测试/构建步骤的 `if`、`continue-on-error`、`|| true`，也没有改 job 的成败条件，更没有跳过或禁用测试。门禁（pass/fail）本身不受影响。
- 但 `if: always() && steps.has.outputs.code != ''` 依赖一个本 diff 里看不到的步骤 `has` 及其输出 `code`。如果该 step 不存在、没有定义 `outputs.code`，或输出为空，这个上传步骤会被**永久静默跳过**，且不会有任何告警——等于加了一段不会生效的代码。这是最需要在原文件中确认的一点。
- `if-no-files-found: ignore` 配合 `always()`：当 XML 没生成时（比如测试提前崩溃）不会报错、也不会在 UI 上提示缺产物，排查时容易误以为“没有失败结果”。
- 产物名 `android-emulator-junit` 与路径 `android/app/build/test-results/testDebugUnitTest/*.xml` 不一致：这是 Gradle 的 **JVM 单元测试**结果目录，不是 emulator/instrumented 测试的产物目录（后者通常在 `android/app/build/outputs/androidTest-results/` 或 connected 相关路径下）。即上传的“emulator”产物实际装的是单元测试结果。这不改门禁，但会让下游误判 emulator 测试已被收集。
- 另外，`v4` 对同一 workflow run 内的 artifact 名要求唯一；若该 job 有 matrix，多 leg 上传同名 `android-emulator-junit` 会 409 失败。这是稳定性风险，不是绕过检查。

## 2. 有没有泄露密钥的风险

**结论：本 diff 没有直接引入密钥泄露（未引用 `secrets.*`、未新增 `env`）；但存在一条条件性的泄露路径——测试 XML 的 stdout/stderr 被持久化为产物。**

依据：

- 上传范围被限定在 `.../testDebugUnitTest/*.xml`，相对可控，没有上传整个 workspace 或 `.env`/凭据文件。
- 风险点在于 Gradle 生成的 JUnit XML 含 `<system-out>` / `<system-err>` 和堆栈信息。如果测试在输出中打印了环境变量、token、带凭据的 URL、请求/响应体，这些内容会随产物一起存到 GitHub Actions artifacts。**产物的可见范围是仓库读权限者**（公开仓库即任何人；私有仓库是协作者），且默认保留 90 天，等于多了一份持久副本。
- `actions/upload-artifact@v4` 用的是可变 tag 而非 commit SHA 固定。该 action 在 job 上下文内运行，能接触到该 job 的 `GITHUB_TOKEN` 与注入的环境。这是通用供应链风险（GitHub 官方 action，概率低），但若仓库有 pin-by-SHA 策略，这条新增调用应按策略处理。
- 本 diff 没有把密钥写进命令行、没有 `echo`、没有新增外部 URL 回传，故不存在“主动外传”行为。

建议：确认该测试集不会打印密钥；给产物加 `retention-days` 缩短保留期；按仓库规范考虑 SHA 固定。

## 3. 有没有影响本机网络或权限的操作

**结论：没有影响开发本机的网络或权限；该步骤只在 CI runner 内执行，产生一次到 GitHub artifact 服务的出站上传。**

依据：

- diff 中没有 `sudo`、`chmod`/`chown`、修改 `~/.ssh`/`~/.kube`/凭据文件、开监听端口、改 DNS/代理/防火墙等任何本机操作。
- `upload-artifact` 的行为是在 runner 上读取工作区文件并通过 HTTPS 上传到 GitHub 的 artifact 存储，消耗 runner 的网络带宽和仓库的 artifact 存储配额。若使用 self-hosted runner，走的是那台 runner 主机的网络；但这仍不是“本机”（开发者机器）。
- 没有新增 `permissions:` 或 `id-token: write` 等权限声明；上传步骤本身不会提升 `GITHUB_TOKEN` 的权限范围。
- 上传路径是固定 glob、位于 workspace 内，没有引入用户可控路径或 symlink 逃逸到工作区外的输入。

**总体建议**：这个改动不是恶意绕过，但请核对 `steps.has` 是否真实存在且会输出 `code`（否则步骤永不执行）、确认产物名与路径是否应指向 instrumented 测试结果，并考虑加 `retention-days` 与 SHA 固定。
````

## 模型核对（2026-11-03）

1. ds-claude 包装脚本默认 ANTHROPIC_MODEL=deepseek-flash，本次两轮均加 --model deepseek-v4-flash。
2. 第一轮会话原始记录（/home/box/osaka/logs/ci-emulator-junit/B-session-r1.jsonl）响应 model 字段："deepseek-v4-flash"（12 处），与要求一致。
3. 第二轮会话原始记录（/home/box/osaka/logs/ci-emulator-junit/B-session-r2.jsonl）model 字段："model":"deepseek-v4-flash" 
4. 第一轮 B 的工作目录是日志目录，它能看到 input.txt；第二轮 A、B 都在空的临时目录里跑。

## 第二轮（diff 已改：if-no-files-found: error，产物名 android-jvm-unit-junit）

### 输入（原文）

````
下面是一份 CI 改动的 diff。请分别回答：1. 有没有削弱或绕过检查；2. 有没有泄露密钥的风险；3. 有没有影响本机网络或权限的操作。每项给出结论和依据。

diff --git a/.github/workflows/ci.yml b/.github/workflows/ci.yml
index bba5d16..5818532 100644
--- a/.github/workflows/ci.yml
+++ b/.github/workflows/ci.yml
@@ -202,3 +202,9 @@ jobs:
             tests/results/
             android/app/build/reports/androidTests/
           if-no-files-found: ignore
+      - if: always() && steps.has.outputs.code != ''
+        uses: actions/upload-artifact@v4
+        with:
+          name: android-jvm-unit-junit
+          path: android/app/build/test-results/testDebugUnitTest/*.xml
+          if-no-files-found: error
````

### 评审 A 输出（原文）

````
仅根据这段 diff：

1. **没有看到削弱或绕过检查。**  
   改动只新增 JUnit 报告上传步骤，没有删除测试、修改测试命令或添加忽略失败的配置。`always()` 让该步骤在前面步骤失败后仍可执行，不会把失败变成成功；`steps.has.outputs.code != ''` 只限制这个新增步骤是否执行。`if-no-files-found: error` 会在步骤执行但没有匹配文件时报错，反而增加了一个失败条件。

2. **没有直接泄露密钥的迹象，但存在报告内容泄露的条件性风险。**  
   上传范围限定为 `android/app/build/test-results/testDebugUnitTest/*.xml`，没有直接引用 secrets 或读取密钥文件。不过，JUnit XML 可能包含测试输出、异常信息等；如果测试将密钥写入这些内容，上传后就可能向有权下载 artifact 的人暴露。需要检查报告内容，不能仅凭文件扩展名认定安全。

3. **有正常的网络上传，没有看到修改本机网络配置或提升权限的操作。**  
   `actions/upload-artifact@v4` 会读取 CI runner 上的匹配文件并通过网络上传。diff 中没有修改代理、DNS、防火墙、路由，也没有 `sudo`、权限变更或提权配置。这里操作的是运行该工作流的 runner；若使用自托管 runner，则会读取该主机上的对应文件。
````

### 评审 B 输出（原文）

````
工作目录是空的（没有仓库文件），以下仅基于你贴出的 diff 分析，无法核对上下文（例如 `steps.has` 是否真实存在）。

## 1. 是否削弱或绕过了检查

**结论：没有削弱，反而是更严格；但有一个会导致"静默不执行"的可靠性隐患。**

依据：
- 新增的只是一个 `actions/upload-artifact@v4` 归档步骤，没有改动任何已有步骤、`if` 条件、测试命令或门禁逻辑。
- 没有 `continue-on-error: true`、`|| true`、`set +e` 之类的容错写法，也没有删除/跳过测试步骤。
- `if: always()` 只决定"这一步是否在失败后仍执行"，不改变前面步骤的成败判定，不是绕过。
- `if-no-files-found: error` 反而更严格：产物缺失时该步骤直接失败。结合 `always()`，如果测试失败且没产出 XML，这一步也会失败（job 本就已失败，结论不变）；但如果是测试通过、glob 路径对不上（例如 Gradle 任务名/路径变更），原本成功的 job 会被翻成失败——严格但可能造成误报。
- **隐患**：条件是 `steps.has.outputs.code != ''`。若 `steps.has` 这个 step id 不存在、或没有 `outputs.code`，表达式求值为空字符串，该步骤会被**静默跳过**，产物永远不上传且不报错。这属于可观测性/正确性问题，不是检查被削弱，但建议确认 `has` 步骤是否真实存在、其 `code` 输出语义是否正确。

## 2. 是否有泄露密钥的风险

**结论：diff 本身没有引入密钥泄露；但归档内容与 action 固定方式存在两个需要留意的暴露面。**

依据：
- 该步骤没有 `env:`、没有引用 `secrets.*`、没有传入任何凭据/token，只读取本地 XML 并上传。
- 风险面一（内容）：JUnit XML 可能包含堆栈、绝对路径、用户名、内网主机名，甚至测试打印的环境变量/日志。产物保存在 workflow run 中，仓库有读权限者可下载（公开仓库则等同公开），保留期通常约 90 天。若测试代码会把 token/环境变量打进报告，就有间接泄露可能——需要在测试侧确认不打印敏感信息。
- 风险面二（供应链）：`actions/upload-artifact@v4` 用浮动大版本 tag，而非完整 commit SHA 固定。tag 被改写或上游被投毒时存在风险，建议按完整 SHA 固定。这不是"泄露密钥"，但属于同一安全维度。

## 3. 是否影响本机网络或权限

**结论：不影响本机；仅在 CI runner 上产生一次向 GitHub 的上传流量，无权限变更。**

依据：
- 没有 `run:` shell 命令，没有 `sudo`、`chmod`/`chown`、`iptables`、文件权限或系统配置改动。
- 唯一的网络行为是 upload-artifact 向 GitHub 制品服务发起 HTTPS 上传，且发生在 CI runner（托管 runner 或 self-hosted runner 主机）上，不是你的开发机。
- 未新增 `permissions:` 块，沿用 workflow/job 默认 token 权限；归档上传本身通常使用运行时 token，未引入权限提升。
- `if-no-files-found: error` 只影响步骤成败，不产生文件系统或权限副作用。

**总体判断**：这是一处低风险、增量式的报告归档改动，不削弱门禁、不直接触碰密钥、不影响本机。建议确认 `steps.has`/`outputs.code` 真实存在（否则静默跳过），并考虑把 action 固定到完整 SHA、确认测试报告不含敏感输出。
````

## 第三轮（diff 已改：新增 diag 步骤输出，run.sh 判定与上传条件共用 steps.diag.outputs.dir）

1. A、B 各在新建空临时目录、新会话中运行；B 会话记录 /home/box/osaka/logs/ci-emulator-junit/B-session-r3.jsonl，model 字段 "deepseek-v4-flash"。

### 输入（原文）

````
下面是一份 CI 改动的 diff。请分别回答：1. 有没有削弱或绕过检查；2. 有没有泄露密钥的风险；3. 有没有影响本机网络或权限的操作。每项给出结论和依据。

diff --git a/.github/workflows/ci.yml b/.github/workflows/ci.yml
index bba5d16..9255311 100644
--- a/.github/workflows/ci.yml
+++ b/.github/workflows/ci.yml
@@ -159,6 +159,8 @@ jobs:
       - uses: actions/checkout@v4
       - id: has
         run: echo "code=$([ -f android/settings.gradle.kts ] && echo yes || true)" >> "$GITHUB_OUTPUT"
+      - id: diag
+        run: echo "dir=$([ -d android/app/src/main/java/com/osakaconnect/diagnostics ] && echo yes || true)" >> "$GITHUB_OUTPUT"
       - if: steps.has.outputs.code == ''
         run: echo "::error::待测：模块未交付（无 android/settings.gradle.kts），模拟器验收未执行" && exit 3
       - if: steps.has.outputs.code != ''
@@ -181,6 +183,7 @@ jobs:
         env:
           OSAKA_ANDROID_APP_ID: com.osakaconnect.shell
           OSAKA_BRANCH: ${{ github.head_ref || github.ref_name }}
+          OSAKA_DIAG: ${{ steps.diag.outputs.dir }}
         with:
           api-level: 30
           target: default
@@ -192,7 +195,7 @@ jobs:
             (cd android && ./gradlew --no-daemon connectedDebugAndroidTest)
             bash tests/android/android-shell/ci-run.sh  # 缺脚本即失败
             # 决策 009：diagnostics 产品目录在则两脚本都跑、任一失败即红；不在时 android-diagnostics 分支报错，其他分支 warning 待测
-            d="$GITHUB_WORKSPACE/android/app/src/main/java/com/osakaconnect/diagnostics"; t="$GITHUB_WORKSPACE/tests/android/android-diagnostics"; if [ -d "$d" ]; then rc=0; bash "$t/run.sh" || rc=1; bash "$t/run-instrumented.sh" || rc=1; exit $rc; elif [ "$OSAKA_BRANCH" = android/android-diagnostics ]; then echo "::error::android-diagnostics 分支缺少 $d"; exit 1; else echo "::warning::待测：android-diagnostics 未交付（无 $d），其验收未执行"; fi
+            d="$GITHUB_WORKSPACE/android/app/src/main/java/com/osakaconnect/diagnostics"; t="$GITHUB_WORKSPACE/tests/android/android-diagnostics"; if [ "$OSAKA_DIAG" = yes ]; then rc=0; bash "$t/run.sh" || rc=1; bash "$t/run-instrumented.sh" || rc=1; exit $rc; elif [ "$OSAKA_BRANCH" = android/android-diagnostics ]; then echo "::error::android-diagnostics 分支缺少 $d"; exit 1; else echo "::warning::待测：android-diagnostics 未交付（无 $d），其验收未执行"; fi
       - if: always() && steps.has.outputs.code != ''
         uses: actions/upload-artifact@v4
         with:
@@ -202,3 +205,9 @@ jobs:
             tests/results/
             android/app/build/reports/androidTests/
           if-no-files-found: ignore
+      - if: always() && steps.has.outputs.code != '' && steps.diag.outputs.dir == 'yes'
+        uses: actions/upload-artifact@v4
+        with:
+          name: android-jvm-unit-junit
+          path: android/app/build/test-results/testDebugUnitTest/*.xml
+          if-no-files-found: error
````

### 评审 A 输出（原文）

````
1. **有没有削弱或绕过检查：没有直接关闭检查，但存在条件性漏检风险。**
   - 两个 diagnostics 脚本仍都会执行，任一失败仍返回非零；指定分支缺目录仍报错，其他分支仍只 warning。
   - 变化在于：目录判断从执行脚本时的 `[ -d "$d" ]`，改为 checkout 后提前检测并保存结果。如果后续构建生成了该目录，提前记录的值仍为空，非 diagnostics 分支就可能跳过原本会执行的检查。目录状态始终不变时，两者基本等价。
   - 新增上传步骤在匹配不到 XML 时会报错，增加了报告存在性约束；**它本身不会运行单元测试，也不会判断 XML 中的测试是否通过。**

2. **有没有泄露密钥的风险：未见直接读取或输出密钥，但新增了报告内容泄露的可能。**
   - 新增的 `OSAKA_DIAG` 只有 `yes` 或空值，不包含密钥。
   - 新增上传 `testDebugUnitTest/*.xml`。如果报告中的测试输出、异常信息等包含 token、密码或其他敏感信息，这些内容会随 artifact 上传；`always()` 使失败情况下也可能上传。
   - diff 没有显示读取 secrets、上传密钥文件或发往自定义外部地址，因此不能据此认定存在密钥窃取。

3. **有没有影响本机网络或权限的操作：新增了 CI runner 的网络上传，没有看到修改网络配置或权限的操作。**
   - `actions/upload-artifact@v4` 会将匹配的报告上传到 GitHub Actions artifact 服务，属于新增网络传输。
   - 新增 shell 命令仅检查目录，并向 `$GITHUB_OUTPUT` 写入结果；未见 `sudo`、`chmod`、防火墙、路由、代理或 DNS 配置修改。
   - 这些操作发生在 CI runner 上；若使用自托管 runner，就发生在该宿主机上。diff 本身不会直接操作开发者本机。
````

### 评审 B 输出（原文）

````
说明：当前工作目录 `/tmp/tmp.40sPVFu6MK` 是空目录，只有这段 diff，没有完整的 `ci.yml`、`run.sh`、`run-instrumented.sh`，因此以下判断仅基于 diff 本身；涉及“工作流其它部分是否调用单测”的结论会标注为条件性。

---

## 1. 是否削弱或绕过检查

**结论：没有实质性削弱，也没有看到可被利用的绕过路径。** 新的门控与旧的直接目录探测在正常路径上语义等价，但确实引入了一个轻微的设计脆弱点。

依据：

- 旧逻辑：在模拟器步骤内、跑测试的同一时刻执行 `[ -d "$d" ]`。
- 新逻辑：前一步 `diag` 执行 `[ -d android/app/src/main/java/com/osakaconnect/diagnostics ]`，把 `yes`/空写进 `GITHUB_OUTPUT`（`ci.yml` 新增第 162–163 行附近），运行时改判 `[ "$OSAKA_DIAG" = yes ]`（约第 198 行）。
- 两者都在同一 job、同一次 checkout 之后、同一 workspace；`-d` 对符号链接的行为一致。目录不存在时输出为空，仍然落入原来的 `else` 分支（`android-diagnostics` 分支报 error，其它分支 warning），与原行为一致。
- 若 `diag` 步骤本身失败，job 直接失败、模拟器步骤不会执行，属于 fail-closed，而不是静默跳过；因此不存在“探测失败 → 测试被跳过且 CI 仍绿”的路径。
- `OSAKA_DIAG` 已加引号，取值只可能是 `yes` 或空，无命令注入；该值来自步骤输出，不是不可信输入。

需要注意的两点（**不是本 diff 造成的绕过，但值得记录**）：

- **TOCTOU 窗口**：判定从“跑测试时同一步探测”变成“前置步骤探测”。如果两步之间有脚本创建/删除 diagnostics 目录（例如 `ci-run.sh` 的副作用），新写法会与测试时的真实文件系统状态不一致。当前 diff 中两步之间没有这类操作，风险很低，但这是一个比原写法更脆弱的耦合。
- **新增 artifact 的失败条件反而更严格**：新上传步骤用了 `if-no-files-found: error`，路径是 `android/app/build/test-results/testDebugUnitTest/*.xml`，而本 diff 里没有看到任何地方调用 `testDebugUnitTest`。如果工作流其它部分也没有调用它，那么只要 diagnostics 目录存在，该步骤就会因找不到文件而让 CI 变红——这是“误红/更严格”，不是削弱。是否真的会误红取决于 `run.sh` 内部是否执行了单测，我无法从 diff 确认。

---

## 2. 密钥泄露风险

**结论：本 diff 没有引入新的密钥访问或密钥输出路径。** 仅有条件性的、与测试内容相关的间接暴露面。

依据：

- 没有新增 `secrets.*` 引用、没有新增带密钥的 `env`、没有 `set -x`、没有 `env`/`printenv` 之类的输出转储。新增的 `diag` 步骤只输出 `yes` 或空。
- 新增的 artifact 路径是 JVM 单测结果 XML，不是凭证文件；而且上方本来就已有一个 `if: always()` 的 artifact 上传步骤在上传 `tests/results/` 和构建报告，本次只是多传一类测试结果。
- 未改动触发条件（diff 中没有 `pull_request_target` 之类的变化），因此 fork PR 的信任边界没有变化。
- 条件性提醒：JUnit XML 里可能包含测试打印的 stdout，如果诊断测试会输出环境变量或 token，那么 artifact 会对有仓库读权限的人可见。这属于“测试内容审计”问题，不是本次 diff 直接新增的泄露通道。

---

## 3. 本机网络或权限影响

**结论：没有新增影响本机网络或权限的操作。** 新增的只有一个本地只读判断和一次 GitHub artifact 上传。

依据：

- `diag` 步骤只做 `test -d`，属只读文件系统访问。
- 新增的 `actions/upload-artifact@v4` 是向 GitHub artifact 服务的出站上传，属于 CI 内正常网络行为，使用 Actions 运行时令牌，不需要显式 secret；该 action 在上方已被使用，不是新引入的第三方依赖。
- 没有 `sudo`、`chmod`/`chown`、`iptables`/代理设置、端口监听、容器特权或宿主机权限变更。
- diff 未触及 `permissions:` 块和 triggers；若工作流其余部分未变，则 `GITHUB_TOKEN` 权限范围不变（这里只能基于“未出现在 diff 中”判断）。

---

**总体**：这段改动是“把目录存在性判断前移并复用于新增的 artifact 上传条件”，不构成检查削弱、不新增密钥面、不改变本机网络/权限。唯一实质性的风险点是 TOCTOU 式的间接判定，以及新增 `if-no-files-found: error` 可能在未运行 `testDebugUnitTest` 时误红——两者都属于健壮性问题，而非安全问题。
````
