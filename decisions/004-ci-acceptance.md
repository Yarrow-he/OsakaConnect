# 004 CI 接线：各端验收脚本接入 GitHub Actions（分支 lead/ci-acceptance）

## 问题
1. 6 个模块（server-core、win-shell、win-diagnostics、admin-connect、android-shell、android-diagnostics）已过审阅，处于待测试；测试要靠 CI 跑各端验收脚本才能出结论。
2. 主分支 ci.yml 没有调用 tests/windows/win-shell/smoke.ps1、tests/windows/win-diagnostics/WinDiagnostics.Tests.csproj、tests/admin/admin-connect/grep-guard.sh、tests/android/android-shell/lang-guard.sh、tests/android/android-diagnostics/run-instrumented.sh。
3. 审阅在 PR #7 提出两处“看起来是绿但其实没测”：
   1. tests/android/android-shell/ci-run.sh 第 26-27 行，产品未交付时 exit 0。
   2. ci.yml 第 127-128 行（android-emulator 任务），无 Android 代码时打印“跳过”，任务仍为绿。

## 方案
1. 新增任务 windows-acceptance（windows-latest）：setup-dotnet 8，构建 windows/ 下已跟踪的 csproj，再跑 `pwsh -NoProfile -File tests/windows/win-shell/smoke.ps1` 和 `dotnet test tests/windows/win-diagnostics/WinDiagnostics.Tests.csproj -c Release`。
2. guards 任务追加两步：`bash tests/admin/admin-connect/grep-guard.sh`、`bash tests/android/android-shell/lang-guard.sh`。
3. android-emulator 任务：模拟器脚本里追加 `bash tests/android/android-diagnostics/run-instrumented.sh`；无 Android 代码时改为 `::error::` 并 exit 3（不再绿）。
4. ci-run.sh 第 26-27 行 exit 0 改为 exit 3。
5. tests/server/*（python 任务内 pytest tests/server/server-core）和 a10-docker（run-a10.sh）在主分支已接入，本 PR 不动。
6. 不加 continue-on-error，不跳过测试，不新增第三方 action（只用 ci.yml 已有的 actions/checkout@v4、actions/setup-dotnet@v4），不改 permissions 和 secrets。

## 理由
1. 一个 PR 把各端验收脚本全部接上，测试拿 CI 运行记录做证据，解除 6 个模块的阻塞。
2. “待测”路径非 0 退出，杜绝假绿，与 PR #7 的修正方向一致。
3. 改动只增加检查步骤，加上把两处假绿改成失败，没有放宽任何已有检查。

## 风险
1. 主分支现在没有 android/，android-emulator 任务会变红，本 PR 自身 CI 也会红，直到 android-shell 合入；这是预期的“未交付即不绿”。
2. smoke.ps1、grep-guard.sh、lang-guard.sh、run.sh、run-instrumented.sh 在产品未交付时仍 exit 0（脚本自带行为），主分支上这几步会显示绿但实际只打印“待测”；本 PR 按最小改动只改审阅点名的两处，其余留给测试在 PR #7 跟进。
3. windows-acceptance 依赖 windows-latest 有桌面会话做 UI Automation，若托管 runner 不支持，smoke.ps1 的 A2/A16 会失败，需测试按日志调整。
4. 多一个 windows-latest 任务，CI 时长和分钟数增加。

## 评审记录（2026-11-02，两者新会话、互不可见，只喂本稿 + diff）

### 评审 A：codex exec，gpt-6-astra，reasoning high（日志 logs/ci-acceptance/review-A.log）
我会只读这两个文件，按六项给出评审结论。

exec
/bin/bash -lc 'cat diff.txt' in /tmp/rvA004
 succeeded in 0ms:
diff --git a/.github/workflows/ci.yml b/.github/workflows/ci.yml
index 93d610c..c845464 100644
--- a/.github/workflows/ci.yml
+++ b/.github/workflows/ci.yml
@@ -15,6 +15,10 @@ jobs:
       - name: scope
         if: github.event_name == 'pull_request'
         run: scripts/check-scope.sh origin/${{ github.base_ref }}
+      - name: grep-guard（A10、A14）
+        run: bash tests/admin/admin-connect/grep-guard.sh
+      - name: android lang-guard（A14、A17）
+        run: bash tests/android/android-shell/lang-guard.sh
   contract:
     runs-on: ubuntu-latest
     steps:
@@ -44,6 +48,23 @@ jobs:
           set -e
           for p in ${{ steps.has.outputs.code }}; do dotnet restore "$p"; dotnet build "$p" -c Release --no-restore; done
           for p in ${{ steps.has.outputs.code }}; do case "$p" in *Tests*.csproj) dotnet test "$p" -c Release --no-build;; esac; done
+  windows-acceptance:
+    runs-on: windows-latest
+    steps:
+      - uses: actions/checkout@v4
+      - uses: actions/setup-dotnet@v4
+        with: { dotnet-version: '8.0.x' }
+      - name: 构建 windows/ 产品（smoke.ps1 不自行构建）
+        shell: bash
+        run: |
+          set -e
+          for p in $(git ls-files 'windows/*.csproj'); do dotnet build "$p" -c Release; done
+      - name: win-shell smoke.ps1（A1、A2、A16、A14、A17）
+        shell: pwsh
+        run: pwsh -NoProfile -File tests/windows/win-shell/smoke.ps1
+      - name: WinDiagnostics.Tests（A3、A4、A15）
+        shell: bash
+        run: dotnet test tests/windows/win-diagnostics/WinDiagnostics.Tests.csproj -c Release
   android:
     runs-on: ubuntu-latest
     steps:
@@ -125,7 +146,7 @@ jobs:
       - id: has
         run: echo "code=$([ -f android/settings.gradle.kts ] && echo yes || true)" >> "$GITHUB_OUTPUT"
       - if: steps.has.outputs.code == ''
-        run: echo "无 Android 代码，跳过"
+        run: echo "::error::待测：模块未交付（无 android/settings.gradle.kts），模拟器验收未执行" && exit 3
       - if: steps.has.outputs.code != ''
         uses: actions/setup-java@v4
         with: { distribution: temurin, java-version: '17' }
@@ -156,6 +177,7 @@ jobs:
             (cd android && ./gradlew --no-daemon connectedDebugAndroidTest)
             bash tests/android/android-shell/ci-run.sh  # 缺脚本即失败
             bash tests/android/android-diagnostics/run.sh  # 缺脚本即失败
+            bash tests/android/android-diagnostics/run-instrumented.sh  # 缺脚本即失败
       - if: always() && steps.has.outputs.code != ''
         uses: actions/upload-artifact@v4
         with:
diff --git a/tests/android/android-shell/ci-run.sh b/tests/android/android-shell/ci-run.sh
index 694fc16..1483e93 100644
--- a/tests/android/android-shell/ci-run.sh
+++ b/tests/android/android-shell/ci-run.sh
@@ -22,9 +22,9 @@ MAIN_COMPONENT="$APP_ID/$MAIN_ACTIVITY"
 step() { echo "== $*"; }
 die() { echo "失败：$*"; exit 1; }
 
-# 产品代码未交付：明确提示后以退出码 0 结束，不伪装通过（此路径不创建产物）。
-[ -d "$ANDROID_DIR" ] || { echo "待测：模块未交付（android/ 目录不存在）"; exit 0; }
-[ -x "$ANDROID_DIR/gradlew" ] || { echo "待测：模块未交付（android/gradlew 不存在）"; exit 0; }
+# 产品代码未交付：明确提示后以退出码 3 结束（非 0，CI 不会显示为通过；此路径不创建产物）。
+[ -d "$ANDROID_DIR" ] || { echo "待测：模块未交付（android/ 目录不存在）"; exit 3; }
+[ -x "$ANDROID_DIR/gradlew" ] || { echo "待测：模块未交付（android/gradlew 不存在）"; exit 3; }
 
 mkdir -p "$RESULT_DIR"
 LOG="$RESULT_DIR/ci-run-$(date +%Y%m%d-%H%M%S).log"

exec
/bin/bash -lc 'cat 004-ci-acceptance.md' in /tmp/rvA004
 succeeded in 0ms:
# 004 CI 接线：各端验收脚本接入 GitHub Actions（分支 lead/ci-acceptance）

## 问题
1. 6 个模块（server-core、win-shell、win-diagnostics、admin-connect、android-shell、android-diagnostics）已过审阅，处于待测试；测试要靠 CI 跑各端验收脚本才能出结论。
2. 主分支 ci.yml 没有调用 tests/windows/win-shell/smoke.ps1、tests/windows/win-diagnostics/WinDiagnostics.Tests.csproj、tests/admin/admin-connect/grep-guard.sh、tests/android/android-shell/lang-guard.sh、tests/android/android-diagnostics/run-instrumented.sh。
3. 审阅在 PR #7 提出两处“看起来是绿但其实没测”：
   1. tests/android/android-shell/ci-run.sh 第 26-27 行，产品未交付时 exit 0。
   2. ci.yml 第 127-128 行（android-emulator 任务），无 Android 代码时打印“跳过”，任务仍为绿。

## 方案
1. 新增任务 windows-acceptance（windows-latest）：setup-dotnet 8，构建 windows/ 下已跟踪的 csproj，再跑 `pwsh -NoProfile -File tests/windows/win-shell/smoke.ps1` 和 `dotnet test tests/windows/win-diagnostics/WinDiagnostics.Tests.csproj -c Release`。
2. guards 任务追加两步：`bash tests/admin/admin-connect/grep-guard.sh`、`bash tests/android/android-shell/lang-guard.sh`。
3. android-emulator 任务：模拟器脚本里追加 `bash tests/android/android-diagnostics/run-instrumented.sh`；无 Android 代码时改为 `::error::` 并 exit 3（不再绿）。
4. ci-run.sh 第 26-27 行 exit 0 改为 exit 3。
5. tests/server/*（python 任务内 pytest tests/server/server-core）和 a10-docker（run-a10.sh）在主分支已接入，本 PR 不动。
6. 不加 continue-on-error，不跳过测试，不新增第三方 action（只用 ci.yml 已有的 actions/checkout@v4、actions/setup-dotnet@v4），不改 permissions 和 secrets。

## 理由
1. 一个 PR 把各端验收脚本全部接上，测试拿 CI 运行记录做证据，解除 6 个模块的阻塞。
2. “待测”路径非 0 退出，杜绝假绿，与 PR #7 的修正方向一致。
3. 改动只增加检查步骤，加上把两处假绿改成失败，没有放宽任何已有检查。

## 风险
1. 主分支现在没有 android/，android-emulator 任务会变红，本 PR 自身 CI 也会红，直到 android-shell 合入；这是预期的“未交付即不绿”。
2. smoke.ps1、grep-guard.sh、lang-guard.sh、run.sh、run-instrumented.sh 在产品未交付时仍 exit 0（脚本自带行为），主分支上这几步会显示绿但实际只打印“待测”；本 PR 按最小改动只改审阅点名的两处，其余留给测试在 PR #7 跟进。
3. windows-acceptance 依赖 windows-latest 有桌面会话做 UI Automation，若托管 runner 不支持，smoke.ps1 的 A2/A16 会失败，需测试按日志调整。
4. 多一个 windows-latest 任务，CI 时长和分钟数增加。

1. 反对当前稿：接线方向正确，但尚不能作为六模块验收完成的依据。
2. 最大风险：多份脚本在未交付时仍返回成功，造成假绿；Android 缺失必红，若为必需检查将阻塞合并。
3. 更好替代方案：统一将未交付、未执行测试设为失败；构建列表为空即失败，并验证 Windows runner 支持 UI 自动化。
4. 未见新增绕过或放宽；修复了两处假绿，但保留其他假绿路径。
5. diff 未新增密钥引用或输出；被调用脚本内容未提供，无法排除其泄密风险。
6. 未见修改本机网络或权限的操作；构建可能在 CI runner 联网下载依赖，脚本内部行为无法确认。

### 评审 B：ds-claude -p --model deepseek-v4-flash，effort high（日志 logs/ci-acceptance/review-B.log）
1. 方向同意，反对按现状合并：只修两处假绿，其余验收脚本仍以 0 退出。
2. 最大风险：android-emulator 在本 PR 即变红，主分支长期红灯可能阻塞所有 PR；同时 grep-guard、lang-guard、smoke 等“待测”仍显示绿，验收证据不完整。
3. 更好替代：所有验收脚本统一 fail-closed（未交付 exit 3）；各端先探测代码路径，无代码明确失败而非静默通过；或拆为“测试先行合入、产品随后合入”两步。
4. 未见削弱或绕过现有检查；但新增步骤在缺产品时仍可返回 0，假绿面未完全消除。
5. 无密钥泄露风险：未改 permissions/secrets，未引入第三方 action。
6. 无本机网络或权限影响；仅 CI runner 会联网下载 .NET SDK、NuGet 等依赖。

## 档位
1. A、B 均反对按现状合并，不满足一类（两人同意且只加检查）条件，按二类处理，等 Hao Yin 拍板。

## Hao Yin 决定
1. Hao Yin 2026-11-02 21:31 决定采纳 A/B 建议，6 脚本无产品时 exit 3。

## 3b9eac4 评审（smoke.ps1 步骤 pwsh→bash，2026-11-02，新会话、互不可见，只喂该提交 diff + 上下各 2 行）

### 评审 A：codex exec，gpt-6-astra，reasoning high（日志 logs/ci-acceptance/review-A-3b9eac4.log）
1. 同意：bash 可保留子进程退出码 3，仍判失败。
2. 最大风险：新增对运行器安装并可用 bash 的依赖。
3. 更好替代方案：保留 pwsh，调用脚本后显式 `exit $LASTEXITCODE`。
4. 未见削弱或绕过检查，非零退出仍失败。
5. 此 diff 未见密钥读取、输出或外传。
6. 此 diff 未新增修改本机网络或权限的操作；脚本内部行为无法据此判断。

### 评审 B：ds-claude -p --model deepseek-v4-flash，effort high（日志 logs/ci-acceptance/review-B-3b9eac4.log）
1. 同意方向，但换外壳只能解决表面问题，建议先复现，确认 `pwsh -File` 真的会把退出码折叠。
2. 最大风险：丢失 Actions pwsh 外壳注入的 `ErrorActionPreference=stop` 与 `LASTEXITCODE` 收尾，导致非终止错误不再必然失败；`exit 3` 现在会真的判定失败。
3. 更优方案：保留 `shell: pwsh`，命令写 `& script; exit $LASTEXITCODE`；或在 bash 中显式映射 rc，并注明 3 的语义。
4. 有轻微削弱：对错误的严格性下降，可能漏检非终止错误。
5. 无密钥泄露，diff 中没有 secret 或回显。
6. 无网络、提权或本机权限操作。

### 组长备注
1. B 说的 ErrorActionPreference 只作用于 Actions 外层壳；smoke.ps1 第 12 行自带 $ErrorActionPreference = 'Stop'，子进程内的严格性不变。
2. A、B 都提出可改为保留 pwsh 并显式 exit $LASTEXITCODE，是否改由 Hao Yin 决定。
