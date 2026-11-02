# 派活 brief：win-diagnostics（Windows）

- owner：写手 · Windows　分支：`windows/win-diagnostics`（从 main 拉，审阅/测试只针对该分支）
- 执行方式：用指定 CLI，**按 ponytail full 执行**；一个模块一个新会话。

## 目标
ErrorPresenter（errors.json 码→中文文案，不含堆栈）、LogRedactor、InviteCode.IsValid；均读 shared 测试向量做单测。

## 只准改的文件
- `windows/`
- `notes/win-diagnostics.md`（追加交付证明）、`board.json`（本模块条目）、`reports/model-issues.md`（如有）

## 接口引用
shared/errors.json；shared/test-vectors/{redaction,invite-code}.json；shared/invite-code.md。架构见 `docs/architecture.md`（含边界 schema 校验方式）。

## 验收条目
A3, A4, A7, A15（见 `acceptance.md`）。完成标准：dotnet test：redaction 全部 case、invite-code 全部正反例、注入异常后显示文案不含堆栈。

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

## 交付证明（写手 · Windows，2026-11-02T17:22:07+10:00）
1. CI：https://github.com/Yarrow-he/OsakaConnect/actions/runs/41403148691，全部 job 成功，其中 windows-dotnet 跑在 windows-latest。
2. 提交：1f6adf6（origin 分支头，含组长合入 main 的 merge）。
3. 构建：CI 里 dotnet build -c Release 输出 Build succeeded。
4. 测试：dotnet test -c Release --no-build，OsakaConnect.Diagnostics.Tests.dll 结果是 Passed 61、Failed 0、Skipped 0、Total 61。
5. lint：本仓库没有单独的 .NET lint job，guards job 已通过（范围、依赖、硬编码线路检查）。
6. CI 日志：/home/box/osaka/logs/win-diagnostics-ci-41403148691.log
7. 任务卡：/home/box/osaka/logs/win-diagnostics-card.md
8. CLI 会话日志：/home/box/osaka/logs/win-diagnostics-codex-1.log
9. 改动文件（git diff --name-only origin/main...HEAD，不含 notes 和 board.json）：
   1. windows/OsakaConnect.Diagnostics.Tests/DiagnosticsTests.cs
   2. windows/OsakaConnect.Diagnostics.Tests/OsakaConnect.Diagnostics.Tests.csproj
   3. windows/OsakaConnect.Diagnostics/ErrorPresenter.cs
   4. windows/OsakaConnect.Diagnostics/InviteCode.cs
   5. windows/OsakaConnect.Diagnostics/LogRedactor.cs
   6. windows/OsakaConnect.Diagnostics/OsakaConnect.Diagnostics.csproj
10. 运行方式：本机没有 .NET SDK，编译和测试只在 CI（windows-latest）上跑。
11. 已知限制：Diagnostics 是独立类库，还没接进 OsakaConnect 主程序，接入方式待组长定；LogRedactor 只覆盖 token、Bearer、invite、api_key、password 这几个键。
## 审阅 PR #11 @c56c80a（2026-11-02 22:16 AEST）
结论：通过
1. 证据：PR 附 CLI 日志 artifacts/cli-logs/fix-windiag-falsegreen-2026-11-02.log；CI 已跑，windows-acceptance 红，符合"缺产品必红"预期（main 尚无 windows/ 产品）。
2. 断言未削弱：ErrorPresenterTests.cs:22-30、LogRedactorTests.cs:20-23、InviteCodeTests.cs:26-29 仅反射改直调，断言保留。
3. 无删测试：3 个测试方法全在；ProductSurface.cs 只删反射与 Skip 逻辑。
4. 路径：WinDiagnostics.Tests.csproj:20 指向 windows/OsakaConnect.Diagnostics/OsakaConnect.Diagnostics.csproj，符合 architecture.md:14；签名已对照分支 windows/win-diagnostics 源码一致。
5. 缺产品必失败：csproj:24-27 EnsureProductProject 报 Error；:30 无条件 ProjectReference；无 Skip。
6. 范围仅 tests/windows/win-diagnostics/ 7 文件；无权限配置改动；无密钥。
建议改：
1. 合并顺序：须先合 win-shell、win-diagnostics 产品分支再合 PR #11，否则 windows-acceptance 按设计红。
2. board.json 中"已合并 main(PR #10)@1211740"与 git 不符（1211740 是 main 并入模块分支），请组长核对。
3. 缺产品退出码为 1 非 3（PR 正文已说明，属 workflow，归 ci-guards）。
日志：logs/win-diagnostics/review-pr11-20261102-221042.log
