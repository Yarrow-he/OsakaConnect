# 006 windows-acceptance：WinDiagnostics.Tests 在 smoke.ps1 失败时也执行（分支 lead/ci-win-diag-always）

## 问题
1. windows-acceptance 任务里 smoke.ps1 在前，失败（如 win-diagnostics 分支没有 win-shell 产品，exit 3）后，后面的 WinDiagnostics.Tests 不执行，win-diagnostics 在自己分支上得不到测试结果。

## 方案
1. WinDiagnostics.Tests 步骤加 `if: ${{ !cancelled() }}`：前一步失败也执行，任务被取消时不执行。
2. 任务结论不变：smoke.ps1 或 WinDiagnostics.Tests 任一失败，任务仍为失败。

## 理由
1. 只增加覆盖，不放宽任何判定；与 always() 相比，取消运行时不再白跑。

## 风险
1. 若前面的构建步骤失败，WinDiagnostics.Tests 仍会执行并多半报错，日志里会多一段失败输出，但不会把红变绿。
2. 多花几十秒 windows-latest 时长。

## 评审记录（2026-11-02，新会话、互不可见，只喂本稿 + diff；日志 logs/ci-win-diag-always/）

### 评审 A：codex exec，gpt-6-astra，high
1. 同意：失败后仍运行测试，保留失败结论。
2. 最大风险：构建失败后仍测试，增加噪声和运行耗时。
3. 更好替代方案：显式要求构建成功且未取消，仅允许跳过 smoke 成功条件。
4. 未削弱或绕过检查；smoke 失败仍使任务失败。
5. 未新增密钥读取或输出；仅凭差异无法判断测试内部行为。
6. 未新增影响本机网络或权限的操作。

### 评审 B：ds-claude -p，deepseek-v4-flash，high
1. 同意：用 `!cancelled()` 而不是 `always()`，既扩大覆盖率，又不改变任务的结论。
2. 最大风险：diff 里只有 4 行上下文，无法确认构建步骤是否排在测试之前；如果构建失败，测试仍会执行并连带报错，掩盖真实根因，也会拉长日志。
3. 更好替代：把 win-diagnostics 拆成独立 job，或者在测试步骤上加前置条件 `steps.<build>.outcome == 'success'`。
4. 未削弱检查：没有 `continue-on-error`，`smoke.ps1` 或测试任一失败，任务仍为失败。
5. 无密钥泄露：没有新增 secret、变量或输出。
6. 无影响本机网络或权限的操作，仅是在 CI runner 上跑 `dotnet test`。

## 档位
1. A、B 均同意，改动只增加执行覆盖，不放宽判定：一类，组长合并。
2. 构建失败时仍跑测试的噪声（A、B）记入风险 1，暂不加 outcome 条件，保持最小改动。
