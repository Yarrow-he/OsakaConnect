# 派活 brief：admin-connect（管理端）

- owner：写手 · Windows　分支：`admin/admin-connect`（从 main 拉，审阅/测试只针对该分支）
- 执行方式：用指定 CLI，**按 ponytail full 执行**；一个模块一个新会话。

## 目标
.NET 8 WPF 管理连接页：输入后台地址，令牌只取环境变量 OSAKA_ADMIN_TOKEN；HttpClient 默认证书校验（禁止自定义放行回调）；System.Text.Json record+required+Disallow 未知字段做边界校验；被拒/证书失败/响应不合 schema 时显示错误且不显示已连接。

## 只准改的文件
- `admin/`
- `notes/admin-connect.md`（追加交付证明）、`board.json`（本模块条目）、`reports/model-issues.md`（如有）

## 接口引用
openapi/admin.v1.yaml；shared/errors.json；shared/test-vectors/admin-contract.json。架构见 `docs/architecture.md`（含边界 schema 校验方式）。

## 验收条目
A10, A14（见 `acceptance.md`）。完成标准：单测：admin-contract 正反例；A10 四项必测（可信+正确凭证、错误凭证、证书不可信、主机名不匹配）由 ci-guards 的 docker 端到端驱动，本模块提供可无界面运行的查询入口（如 --probe <url>，退出码区分结果）；源码 grep 无 ServerCertificateCustomValidationCallback。

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
1. CI：https://github.com/Yarrow-he/OsakaConnect/actions/runs/41403156768，全部 job 成功，其中 windows-dotnet 跑在 windows-latest。
2. 提交：798112b（origin 分支头，含组长合入 main 的 merge）。
3. 构建：CI 里 dotnet build -c Release 输出 Build succeeded。
4. 测试：dotnet test -c Release --no-build，OsakaAdmin.Tests.dll 结果是 Passed 63、Failed 0、Skipped 0、Total 63。
5. lint：本仓库没有单独的 .NET lint job，guards job 已通过（范围、依赖、硬编码线路检查）。
6. CI 日志：/home/box/osaka/logs/admin-connect-ci-41403156768.log
7. 任务卡：/home/box/osaka/logs/admin-connect-card.md
8. CLI 会话日志：/home/box/osaka/logs/admin-connect-codex-1.log
9. 改动文件（git diff --name-only origin/main...HEAD，不含 notes 和 board.json）：
   1. admin/OsakaAdmin.Tests/AdminContractTests.cs
   2. admin/OsakaAdmin.Tests/OsakaAdmin.Tests.csproj
   3. admin/OsakaAdmin/AdminContract.cs
   4. admin/OsakaAdmin/AdminProbe.cs
   5. admin/OsakaAdmin/App.xaml
   6. admin/OsakaAdmin/App.xaml.cs
   7. admin/OsakaAdmin/MainWindow.xaml
   8. admin/OsakaAdmin/MainWindow.xaml.cs
   9. admin/OsakaAdmin/OsakaAdmin.csproj
10. 运行方式：本机没有 .NET SDK，编译和测试只在 CI（windows-latest）上跑。
11. 已知限制（rework1 更正）：A10 目前未验证。CI 41403156768 的 a10-docker 实际没有跑 A10：ci.yml:102-105 找不到 tests/admin/admin-connect/run-a10.sh 而跳过，全程无 --probe 调用。A10 端到端脚本归测试补；模块自身单测不覆盖真实 TLS；测试项目用了 3 个测试包，需组长确认；WPF 界面没在真机启动过。

## 审阅（审阅 · Osaka，2026-11-02）
结论：必须改。
必须改：
1. A10 未执行：CI 41403156768 的 a10-docker 只跑了 echo "无 A10 脚本，跳过"（ci.yml:102-105，tests/admin/admin-connect/run-a10.sh 不存在），没有任何 --probe 调用；notes 第 11 条“由 a10-docker 端到端驱动”与事实不符，需更正，A10 不能算有证据（可机器化：a10-docker 缺脚本时改为失败而非跳过，需组长定）。
2. App.xaml.cs:18-20 --probe 成功只返回退出码，丢弃版本/健康，A10 ① “显示版本/健康”无法断言；最小改法：成功时向 stdout 输出一行版本/健康摘要（可机器化：e2e grep 版本串）。
建议改：
1. AdminContract.cs:57 正则 $ 允许尾随换行，改 \z 或补向量反例（可机器化：向量/单测）。
2. AdminProbe.cs:77-79 FormatException 分支无用例，补 InlineData("abc def")（可机器化：单测）。
3. AdminContract.cs:17、:56 冗余 JsonPropertyName 与 null 判断可删。
4. OsakaAdmin.Tests.csproj:11-13 测试包待组长补批。
证据：日志 /home/box/osaka/logs/admin-connect-ci-41403156768.log（a10-docker 段）；审阅日志 /home/box/osaka/logs/review-admin-connect-20261102.log；diff /home/box/osaka/logs/review-admin-connect-diff.txt。

## rework1 交付（2026-11-02）
1. 改动：--probe 成功时 stdout 输出一行 `version=<x.y.z> health=<status> routes=<n>`（ProbeResult.FormatSummary），失败只返回退出码不变；版本正则改 `^[0-9]+\.[0-9]+\.[0-9]+\z`（去掉 ECMAScript 选项）；删 Route.DisplayName 冗余 JsonPropertyName 与 `version is not null`。
2. 新增测试：SummaryIncludesVersionAndHealth；RejectsVersionWithTrailingNewlineOrNonAsciiDigits（2 例）；FormatExceptionDuringRequestReturnsUsageError（桩注入 FormatException）。
3. 任务卡 /home/box/osaka/logs/admin-connect-rework1-card.md；Codex 日志 /home/box/osaka/logs/admin-connect-codex-rework1.log。
4. 本机无 .NET SDK，未编译未跑测试：本次待 CI 验证（组长 push 后）。
