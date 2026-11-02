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
